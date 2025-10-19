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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
    
    // EAGER initialization - create channel immediately
    private val channel: ManagedChannel = run {
        val builder = ManagedChannelBuilder.forAddress(ApiConfig.GRPC_HOST, ApiConfig.GRPC_PORT)
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS) // Increased for Cloudflare
            .keepAliveWithoutCalls(true)
            .maxInboundMessageSize(4 * 1024 * 1024) // Reduced for Cloudflare compatibility
            .idleTimeout(120, TimeUnit.SECONDS) // Increased for proxy stability
            // Cloudflare-friendly metadata size (reduced from 8192)
            .maxInboundMetadataSize(4096)
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
                android.util.Log.d("GrpcService", "Connection warmed up successfully")
                
                // Start health monitoring
                healthService.startMonitoring()
            } catch (e: Exception) {
                android.util.Log.w("GrpcService", "Connection warmup failed: ${e.message}")
            }
        }
    }

    // OPTIMIZED: Direct method without Result wrapper and auth check
    suspend fun searchVehicle(
        searchType: String,
        searchValue: String
    ): List<VehicleResult> {
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
        
        return try {
            // ✅ CORRECT STREAMING METHOD: SearchVehicleStream with fresh token
            getAuthenticatedStub().searchVehicleStream(request)
                .map { response ->
                    VehicleResult(
                        id = response.id,
                        nomorPolisi = response.nomorPolisi,
                        tipeKendaraan = response.tipeKendaraan,
                        dataVersion = response.dataVersion,
                        financeName = response.financeName
                    )
                }
                .toList()
        } catch (e: StatusException) {
            // Handle token expiration specifically
            if (e.status.code == Status.Code.UNAUTHENTICATED) {
                android.util.Log.d("GrpcService", "Token expired, attempting refresh")
                
                val refreshed = refreshTokenIfPossible()
                if (refreshed) {
                    android.util.Log.d("GrpcService", "Token refreshed, retrying request")
                    // Retry with new token
                    try {
                        getAuthenticatedStub().searchVehicleStream(request)
                            .map { response ->
                                VehicleResult(
                                    id = response.id,
                                    nomorPolisi = response.nomorPolisi,
                                    tipeKendaraan = response.tipeKendaraan,
                                    dataVersion = response.dataVersion,
                                    financeName = response.financeName
                                )
                            }
                            .toList()
                    } catch (retryException: Exception) {
                        android.util.Log.e("GrpcService", "Retry failed: ${retryException.message}")
                        emptyList()
                    }
                } else {
                    android.util.Log.w("GrpcService", "Token refresh failed")
                    sessionManager.clearSession()
                    emptyList()
                }
            } else {
                android.util.Log.e("GrpcService", "Search failed: ${e.message}")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("GrpcService", "Search failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Attempt to refresh token using saved credentials
     * Returns true if successful, false otherwise
     */
    private suspend fun refreshTokenIfPossible(): Boolean {
        return refreshMutex.withLock {
            if (isRefreshing) {
                android.util.Log.d("GrpcService", "Token refresh already in progress")
                return@withLock false
            }
            
            isRefreshing = true
            
            try {
                val email = sessionManager.getEmail()
                val password = sessionManager.getPassword()
                
                if (email.isNullOrBlank() || password.isNullOrBlank()) {
                    android.util.Log.w("GrpcService", "No saved credentials available for token refresh")
                    return@withLock false
                }
                
                android.util.Log.d("GrpcService", "Attempting token refresh with saved credentials")
                
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
                        
                        android.util.Log.d("GrpcService", "Token refresh successful")
                        return@withLock true
                    } else {
                        android.util.Log.w("GrpcService", "Token refresh failed: ${response.status}")
                        return@withLock false
                    }
                } finally {
                    refreshClient.close()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("GrpcService", "Token refresh error: ${e.message}")
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