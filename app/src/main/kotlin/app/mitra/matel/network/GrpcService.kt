package app.mitra.matel.network

// Top-level imports in GrpcService.kt
import android.content.Context
import grpc.Vehicle
import grpc.VehicleServiceGrpcKt
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import app.mitra.matel.utils.SessionManager
import app.mitra.matel.network.models.LoginRequest
import app.mitra.matel.network.models.LoginResponse
import app.mitra.matel.utils.DeviceUtils
import io.grpc.StatusException
import app.mitra.matel.network.HttpClientFactory
import app.mitra.matel.network.NetworkDebugHelper
import io.grpc.Status
import android.util.Log
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
    private val sessionManager = SessionManager.getInstance(context)
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
    private val baseVehicleService: VehicleServiceGrpcKt.VehicleServiceCoroutineStub =
        VehicleServiceGrpcKt.VehicleServiceCoroutineStub(channel)

    // Get authenticated stub with current token
    @Suppress("DEPRECATION")
    private fun getAuthenticatedStub(): VehicleServiceGrpcKt.VehicleServiceCoroutineStub {
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
                        healthService.quickCheck()
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
            // ✅ OPTIMIZED UNARY METHOD: SearchVehicle (updated to new proto)
            val response = getAuthenticatedStub().searchVehicle(request)
            
            // Log gRPC response
            Log.d("gRPC Service", "Search response: ${response.vehiclesList.size} vehicles found")
            if (response.vehiclesList.isNotEmpty()) {
                Log.d("gRPC Service", "First vehicle: ${response.vehiclesList.first().nomorPolisi}")
            }
            
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
            // Log gRPC error
            Log.e("gRPC Service", "StatusException: ${e.status.code} - ${e.status.description}")
            
            // Handle authentication errors based on error codes
            if (e.status.code == io.grpc.Status.Code.UNAUTHENTICATED) {
                // Extract error message and code for categorization
                val errorMessage = e.status.description ?: ""
                val errorCode = extractErrorCode(errorMessage)
                
                // Log authentication error details
                Log.w("gRPC Service", "UNAUTHENTICATED error: $errorCode - $errorMessage")
                
                // Categorize error based on error codes only (no string matching)
                when (errorCode) {
                    // Immediate logout required (Critical Security)
                    "AUTH_005", "AUTH_006", "AUTH_007", "AUTH_008" -> {
                        // User not found, device ownership changed, invalid device claim, or device claim required
                        Log.w("gRPC Service", "Critical auth error - clearing session: $errorCode")
                        sessionManager.clearSession()
                        emptyList()
                    }
                    
                    // Reauthentication required (Regular token issues) - retry refresh 3 times
                    "AUTH_001", "AUTH_002", "AUTH_003", "AUTH_004" -> {
                        // Missing metadata or token issues - attempt refresh with retry
                        Log.d("gRPC Service", "Token refresh required: $errorCode")
                        handleRegularTokenRefresh(cacheKey, request)
                    }
                    
                    // Default case for other Unauthenticated errors - attempt refresh with retry
                    else -> {
                        // Regular token expiration or unknown error - attempt refresh with retry
                        Log.d("gRPC Service", "Generic auth error - attempting refresh: $errorCode")
                        handleRegularTokenRefresh(cacheKey, request)
                    }
                }
            } else if (e.status.code == io.grpc.Status.Code.PERMISSION_DENIED) {
                // Handle authorization failures
                val errorMessage = e.status.description ?: ""
                val errorCode = extractErrorCode(errorMessage)
                
                Log.w("gRPC Service", "PERMISSION_DENIED error: $errorCode - $errorMessage")
                
                when (errorCode) {
                    "SUB_001", "SUB_002" -> {
                        // Subscription or tier issues - return empty but don't clear session
                        Log.w("gRPC Service", "Subscription issue: $errorCode")
                        emptyList()
                    }
                    else -> {
                        Log.w("gRPC Service", "Unknown permission error: $errorCode")
                        emptyList()
                    }
                }
            } else {
                Log.w("gRPC Service", "Other gRPC error: ${e.status.code}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("gRPC Service", "General exception: ${e.message}", e)
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

    // Helper method for handling regular token refresh scenarios with retry tracking
    private suspend fun handleRegularTokenRefresh(cacheKey: String, request: Vehicle.VehicleSearchRequest): List<VehicleResult> {
        var retryCount = 0
        val maxRetryAttempts = 3
        
        while (retryCount < maxRetryAttempts) {
            val refreshed = refreshTokenIfPossible()
            if (refreshed) {
                // Retry with new token
                try {
                    val retryResponse = getAuthenticatedStub().searchVehicle(request)
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
                    
                    return retryResults
                } catch (retryException: Exception) {
                    // If retry fails after token refresh, increment count and try again
                    retryCount++
                    Log.w("gRPC Service", "Token refresh retry failed (attempt $retryCount/$maxRetryAttempts): ${retryException.message}")
                    
                    // Wait a bit before next retry
                    delay(1000)
                }
            } else {
                retryCount++
                Log.w("gRPC Service", "Token refresh failed (attempt $retryCount/$maxRetryAttempts)")
                
                // Wait a bit before next retry
                delay(1000)
            }
        }
        
        // All retry attempts failed
        Log.w("gRPC Service", "All $maxRetryAttempts token refresh attempts failed")
        val hasCreds = !sessionManager.getEmail().isNullOrBlank() && !sessionManager.getPassword().isNullOrBlank()
        if (!hasCreds) {
            sessionManager.clearSession()
        }
        return emptyList()
    }

    // Helper method to extract error code from error message
    private fun extractErrorCode(message: String): String {
        // Error codes are in format "AUTH_001", "SUB_002", etc.
        val pattern = Regex("""(AUTH|SUB)_\d{3}""")
        return pattern.find(message)?.value ?: ""
    }

    // Helper method for handling vehicle detail token refresh scenarios
    private suspend fun handleVehicleDetailTokenRefresh(vehicleId: String): Result<VehicleDetail> {
        val refreshed = refreshTokenIfPossible()
        if (refreshed) {
            try {
                val retryResponse = getAuthenticatedStub().getVehicleDetail(
                    Vehicle.VehicleDetailRequest.newBuilder().setId(vehicleId).build()
                )
                val retryDetail = VehicleDetail(
                    id = vehicleId,
                    nomor_kontrak = retryResponse.nomorKontrak,
                    nama_konsumen = "",
                    past_due = retryResponse.pastDue,
                    nomor_polisi = retryResponse.nomorPolisi,
                    nomor_rangka = retryResponse.nomorRangka,
                    nomor_mesin = retryResponse.nomorMesin,
                    tipe_kendaraan = retryResponse.tipeKendaraan,
                    finance_name = retryResponse.financeName,
                    cabang = retryResponse.cabang,
                    tahun_kendaraan = retryResponse.tahunKendaraan,
                    warna_kendaraan = retryResponse.warnaKendaraan
                )
                return Result.success(retryDetail)
            } catch (retryException: Exception) {
                return Result.failure(retryException)
            }
        } else {
            val hasCreds = !sessionManager.getEmail().isNullOrBlank() && !sessionManager.getPassword().isNullOrBlank()
            if (!hasCreds) {
                sessionManager.clearSession()
            }
            return Result.failure(Exception("Authentication required"))
        }
    }

    private suspend fun refreshTokenIfPossible(): Boolean {
        return if (NetworkDebugHelper.isNetworkAvailable(context)) HttpClientFactory.refreshTokenWithSavedCredentials(context) else false
    }

    // ✅ UNARY METHOD: Get vehicle detail via gRPC VehicleService
    suspend fun getVehicleDetail(vehicleId: String): Result<VehicleDetail> {
        return try {
            val request = Vehicle.VehicleDetailRequest.newBuilder()
                .setId(vehicleId)
                .build()

            val response = getAuthenticatedStub().getVehicleDetail(request)

            val detail = VehicleDetail(
                id = vehicleId, // proto detail doesn’t include id; use input
                nomor_kontrak = response.nomorKontrak,
                nama_konsumen = "", // not in proto; keep empty
                past_due = response.pastDue,
                nomor_polisi = response.nomorPolisi,
                nomor_rangka = response.nomorRangka,
                nomor_mesin = response.nomorMesin,
                tipe_kendaraan = response.tipeKendaraan,
                finance_name = response.financeName,
                cabang = response.cabang,
                tahun_kendaraan = response.tahunKendaraan,
                warna_kendaraan = response.warnaKendaraan
            )

            Result.success(detail)
        } catch (e: io.grpc.StatusException) {
            // Handle authentication errors based on error codes
            if (e.status.code == io.grpc.Status.Code.UNAUTHENTICATED) {
                // Extract error message and code for categorization
                val errorMessage = e.status.description ?: ""
                val errorCode = extractErrorCode(errorMessage)
                
                // Categorize error based on error codes only (no string matching)
                when (errorCode) {
                    // Immediate logout required (Critical Security)
                    "AUTH_005", "AUTH_006", "AUTH_007", "AUTH_008" -> {
                        // User not found, device ownership changed, invalid device claim, or device claim required
                        sessionManager.clearSession()
                        Result.failure(Exception("Authentication required - please reauthenticate"))
                    }
                    
                    // Reauthentication required (Regular token issues)
                    "AUTH_001", "AUTH_002", "AUTH_003", "AUTH_004" -> {
                        // Missing metadata or token issues - attempt refresh
                        handleVehicleDetailTokenRefresh(vehicleId)
                    }
                    
                    // Default case for other Unauthenticated errors - attempt refresh
                    else -> {
                        // Regular token expiration or unknown error - attempt refresh
                        handleVehicleDetailTokenRefresh(vehicleId)
                    }
                }
            } else if (e.status.code == io.grpc.Status.Code.PERMISSION_DENIED) {
                // Handle authorization failures
                val errorMessage = e.status.description ?: ""
                val errorCode = extractErrorCode(errorMessage)
                
                when (errorCode) {
                    "SUB_001" -> {
                        // Subscription issue
                        Result.failure(Exception("No active subscription - please renew your subscription"))
                    }
                    "SUB_002" -> {
                        // Tier issue
                        Result.failure(Exception("System configuration error - please contact support"))
                    }
                    else -> Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}