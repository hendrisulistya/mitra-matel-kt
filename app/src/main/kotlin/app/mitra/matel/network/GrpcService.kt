package app.mitra.matel.network

import android.content.Context
import grpc.Vehicle
import grpc.VehicleSearchServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import app.mitra.matel.utils.SessionManager
import app.mitra.matel.network.models.LoginRequest
import app.mitra.matel.network.models.LoginResponse
import app.mitra.matel.utils.DeviceUtils
import io.grpc.StatusException
import io.grpc.Status
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

data class VehicleResult(
    val id: String,
    val nomorPolisi: String,
    val tipeKendaraan: String,
    val dataVersion: String,
    val financeName: String
)

class GrpcService(private val context: Context) {
    private val sessionManager = SessionManager(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val refreshMutex = Mutex()
    private var isRefreshing = false
    
    // ✅ CONCURRENT SEARCH: Prevent duplicate requests for same search
    private val activeSearches = mutableMapOf<String, Deferred<List<VehicleResult>>>()
    
    // ✅ SEARCH CACHE: LRU cache for search results (max 50 entries, 5 minute TTL)
    private data class CacheEntry(
        val results: List<VehicleResult>,
        val timestamp: Long
    )
    private val searchCache = mutableMapOf<String, CacheEntry>()
    private val cacheMaxSize = 50
    private val cacheTtlMs = 5 * 60 * 1000L // 5 minutes
    
    // EAGER initialization - create channel immediately with optimized connection pooling
    private val channel: ManagedChannel = run {
        val builder = ManagedChannelBuilder.forAddress(ApiConfig.GRPC_HOST, ApiConfig.GRPC_PORT)
            // ✅ OPTIMIZED: Faster connection establishment
            .keepAliveTime(15, TimeUnit.SECONDS) // Reduced for faster detection
            .keepAliveTimeout(5, TimeUnit.SECONDS) // Faster timeout for quicker recovery
            .keepAliveWithoutCalls(true)
            // ✅ PERFORMANCE: Larger message size for batch operations
            .maxInboundMessageSize(8 * 1024 * 1024) // Increased for better throughput
            // ✅ CONNECTION REUSE: Shorter idle timeout for more aggressive connection reuse
            .idleTimeout(60, TimeUnit.SECONDS) // Reduced for faster reconnection
            // ✅ METADATA OPTIMIZATION: Larger metadata for complex auth headers
            .maxInboundMetadataSize(8192) // Increased for auth token flexibility
            // ✅ CONNECTION POOLING: Enable HTTP/2 connection reuse
            .maxInboundMessageSize(8 * 1024 * 1024)
            // Add user agent for better Cloudflare compatibility
            .userAgent("MitraMatel-Android/1.0")
            
        if (!ApiConfig.IS_PRODUCTION) {
            builder.usePlaintext()
        }
        
        builder.build()
    }
    
    // Dynamic auth metadata - fetches fresh token on each call
    private fun getAuthMetadata(): Metadata {
        val metadata = Metadata()
        sessionManager.getToken()?.let { token ->
            val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
            metadata.put(authKey, "Bearer $token")
        }
        return metadata
    }

    // Base service stub without metadata
    private val baseVehicleService: VehicleSearchServiceGrpcKt.VehicleSearchServiceCoroutineStub =
        VehicleSearchServiceGrpcKt.VehicleSearchServiceCoroutineStub(channel)

    // Get authenticated stub with current token
    @Suppress("DEPRECATION")
    private fun getAuthenticatedStub(): VehicleSearchServiceGrpcKt.VehicleSearchServiceCoroutineStub {
        return MetadataUtils.attachHeaders(baseVehicleService, getAuthMetadata())
    }
    
    // Add health monitoring
    val healthService = GrpcHealthService(context, channel)
    
    init {
        // Use proper coroutine scope instead of GlobalScope
        scope.launch {
            try {
                // Force connection attempt to establish channel early
                channel.getState(true)
                
                // Start health monitoring
                healthService.startMonitoring()
            } catch (e: Exception) {
                // Connection warmup failed, will retry on first request
            }
        }
    }
    
    /**
     * Proactively warm up the connection and validate token
     * Call this when app resumes to reduce first search latency
     */
    fun warmUpConnection() {
        scope.launch {
            try {
                // Force channel to connect if idle
                val currentState = channel.getState(true)
                
                // More aggressive connection establishment
                try {
                    waitForConnectionReady(timeoutMs = 3000) // 3 second timeout for warmup
                    
                    // Proactively check token validity with a lightweight health check
                    try {
                        healthService.checkHealth()
                    } catch (e: Exception) {
                        // Try to refresh token proactively in a coroutine
                        scope.launch {
                            try {
                                refreshTokenIfPossible()
                            } catch (refreshError: Exception) {
                                // Token refresh failed during warmup
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    // Connection warmup timeout, will retry on first request
                    // Don't throw - let individual requests handle connection
                }
                
            } catch (e: Exception) {
                // Connection warmup failed
            }
        }
    }

    /**
     * Check if connection is ready without waiting
     * Useful for UI status indicators
     */
    fun isConnectionReady(): Boolean {
        return channel.getState(false) == io.grpc.ConnectivityState.READY
    }

    /**
     * Wait for connection to be ready before making requests
     * Prevents hanging when app resumes and connection is not ready
     */
    private suspend fun waitForConnectionReady(timeoutMs: Long = 5000) {
        val startTime = System.currentTimeMillis()
        var attempts = 0
        val maxAttempts = 50 // 5 seconds with 100ms intervals
        
        while (attempts < maxAttempts) {
            val currentState = channel.getState(false)
            
            when (currentState) {
                io.grpc.ConnectivityState.READY -> {
                    return
                }
                io.grpc.ConnectivityState.IDLE -> {
                    channel.getState(true) // Request connection
                }
                io.grpc.ConnectivityState.CONNECTING -> {
                    // Connection in progress, waiting...
                }
                io.grpc.ConnectivityState.TRANSIENT_FAILURE -> {
                    channel.getState(true) // Request reconnection
                }
                io.grpc.ConnectivityState.SHUTDOWN -> {
                    throw IllegalStateException("Connection channel is shutdown")
                }
            }
            
            kotlinx.coroutines.delay(100) // Wait 100ms between checks
            attempts++
            
            // Check timeout
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw Exception("Connection timeout: Service not ready after ${timeoutMs}ms")
            }
        }
        
        throw Exception("Connection failed: Maximum attempts reached")
    }

    // ✅ UNARY METHOD: Fast search with caching, deduplication, and optimized connection
    suspend fun searchVehicle(
        searchType: String,
        searchValue: String
    ): List<VehicleResult> {
        // ✅ CACHE CHECK: Return cached results if available and fresh
        val cacheKey = "${searchType}:${searchValue.uppercase()}"
        val cachedEntry = searchCache[cacheKey]
        val currentTime = System.currentTimeMillis()
        
        if (cachedEntry != null && (currentTime - cachedEntry.timestamp) < cacheTtlMs) {
            return cachedEntry.results
        }
        
        // ✅ DEDUPLICATION: Check if same search is already in progress
        activeSearches[cacheKey]?.let { activeSearch ->
            return activeSearch.await()
        }
        
        // ✅ ASYNC SEARCH: Create deferred for this search request
        val searchDeferred = scope.async {
            try {
                // ✅ OPTIMIZED CONNECTION: Only wait if not already ready
                if (!isConnectionReady()) {
                    waitForConnectionReady(timeoutMs = 3000) // Reduced timeout for faster UX
                }
                
                val request = when (searchType) {
                    "nopol" -> Vehicle.VehicleSearchRequest.newBuilder()
                        .setNomorPolisi(searchValue)
                        .build()
                    "noka" -> Vehicle.VehicleSearchRequest.newBuilder()
                        .setNomorRangka(searchValue)
                        .build()
                    "nosin" -> Vehicle.VehicleSearchRequest.newBuilder()
                        .setNomorMesin(searchValue)
                        .build()
                    else -> throw IllegalArgumentException("Invalid search type: $searchType")
                }
                
                performActualSearch(request, cacheKey)
            } finally {
                // ✅ CLEANUP: Remove from active searches when done
                activeSearches.remove(cacheKey)
            }
        }
        
        // Store the deferred for potential deduplication
        activeSearches[cacheKey] = searchDeferred
        
        return searchDeferred.await()
    }
    
    // ✅ ACTUAL SEARCH: Separated for better error handling and caching
    private suspend fun performActualSearch(
        request: Vehicle.VehicleSearchRequest,
        cacheKey: String
    ): List<VehicleResult> {
        
        return try {
            // ✅ OPTIMIZED UNARY METHOD: SearchVehicleUnary with faster connection
            val response = getAuthenticatedStub().searchVehicleUnary(request)
            val results = response.vehiclesList.map { vehicle ->
                VehicleResult(
                    id = vehicle.id,
                    nomorPolisi = vehicle.nomorPolisi,
                    tipeKendaraan = vehicle.tipeKendaraan,
                    dataVersion = vehicle.dataVersion,
                    financeName = vehicle.financeName
                )
            }
            
            // ✅ CACHE STORAGE: Store results in cache with LRU eviction
            storeInCache(cacheKey, results)
            
            results
        } catch (e: StatusException) {
            // Handle token expiration specifically
            if (e.status.code == Status.Code.UNAUTHENTICATED) {
                val refreshed = refreshTokenIfPossible()
                if (refreshed) {
                    // Retry with new token
                    try {
                        val retryResponse = getAuthenticatedStub().searchVehicleUnary(request)
                        val retryResults = retryResponse.vehiclesList.map { vehicle ->
                            VehicleResult(
                                id = vehicle.id,
                                nomorPolisi = vehicle.nomorPolisi,
                                tipeKendaraan = vehicle.tipeKendaraan,
                                dataVersion = vehicle.dataVersion,
                                financeName = vehicle.financeName
                            )
                        }
                        
                        // Cache the retry results
                        storeInCache(cacheKey, retryResults)
                        
                        retryResults
                    } catch (retryException: Exception) {
                        emptyList()
                    }
                } else {
                    sessionManager.clearSession()
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Attempt to refresh token using saved credentials
     * Returns true if successful, false otherwise
     */

    // ✅ CACHE MANAGEMENT: Store results with LRU eviction
    private fun storeInCache(key: String, results: List<VehicleResult>) {
        // Remove expired entries first
        cleanExpiredCache()
        
        // LRU eviction: remove oldest entries if cache is full
        if (searchCache.size >= cacheMaxSize) {
            val oldestKey = searchCache.entries.minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { searchCache.remove(it) }
        }
        
        searchCache[key] = CacheEntry(results, System.currentTimeMillis())
    }
    
    // ✅ CACHE CLEANUP: Remove expired entries
    private fun cleanExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = searchCache.entries
            .filter { (currentTime - it.value.timestamp) >= cacheTtlMs }
            .map { it.key }
        
        expiredKeys.forEach { searchCache.remove(it) }
    }
    
    // ✅ CACHE UTILITY: Clear all cached search results
    fun clearSearchCache() {
        searchCache.clear()
    }

    private suspend fun refreshTokenIfPossible(): Boolean {
        return refreshMutex.withLock {
            if (isRefreshing) {
                return@withLock false
            }
            
            isRefreshing = true
            
            try {
                val email = sessionManager.getEmail()
                val password = sessionManager.getPassword()
                
                if (email.isNullOrBlank() || password.isNullOrBlank()) {
                    return@withLock false
                }
                
                // Create a simple HTTP client for token refresh
                val refreshClient = HttpClient(Android) {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        })
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = 10000
                        connectTimeoutMillis = 5000
                        socketTimeoutMillis = 10000
                    }
                }
                
                try {
                    val deviceInfo = DeviceUtils.getDeviceInfo(context)
                    val loginRequest = LoginRequest(email, password, deviceInfo)
                    val response = refreshClient.post("${ApiConfig.BASE_URL}${ApiConfig.Endpoints.LOGIN}") {
                        contentType(ContentType.Application.Json)
                        setBody(loginRequest)
                    }
                    
                    if (response.status.isSuccess()) {
                        val loginResponse: LoginResponse = response.body()
                        val newToken = loginResponse.token
                        
                        // Update token in session manager
                        sessionManager.saveToken(newToken)
                        
                        return@withLock true
                    } else {
                        return@withLock false
                    }
                } finally {
                    refreshClient.close()
                }
                
            } catch (e: Exception) {
                return@withLock false
            } finally {
                isRefreshing = false
            }
        }
    }

    fun close() {
        healthService.stopMonitoring()
        if (!channel.isShutdown) {
            channel.shutdown()
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow()
                }
            } catch (e: InterruptedException) {
                channel.shutdownNow()
            }
        }
    }
}