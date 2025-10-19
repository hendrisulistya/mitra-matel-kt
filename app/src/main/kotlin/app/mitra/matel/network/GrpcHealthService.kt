package app.mitra.matel.network

import android.content.Context
import grpc.Health
import grpc.HealthServiceGrpcKt
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log
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

data class GrpcConnectionStatus(
    val state: ConnectivityState = ConnectivityState.IDLE,
    val isHealthy: Boolean = false,
    val latencyMs: Long = 0L,
    val lastHeartbeat: Long = 0L,
    val serverVersion: String = "",
    val activeConnections: Int = 0,
    val errorMessage: String? = null
)

class GrpcHealthService(
    private val context: Context,
    private val channel: ManagedChannel
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessionManager = SessionManager(context)
    private val refreshMutex = Mutex()
    private var isRefreshing = false
    
    private val _connectionStatus = MutableStateFlow(GrpcConnectionStatus())
    val connectionStatus: StateFlow<GrpcConnectionStatus> = _connectionStatus.asStateFlow()
    
    // Dynamic auth metadata - fetches fresh token on each call
    private fun getAuthMetadata(): Metadata {
        val metadata = Metadata()
        sessionManager.getToken()?.let { token ->
            val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
            metadata.put(authKey, "Bearer $token")
        }
        return metadata
    }

    // Base health service stub without metadata
    private val baseHealthService by lazy {
        HealthServiceGrpcKt.HealthServiceCoroutineStub(channel)
    }

    // Get authenticated stub with current token
    @Suppress("DEPRECATION")
    private fun getAuthenticatedStub(): HealthServiceGrpcKt.HealthServiceCoroutineStub {
        return MetadataUtils.attachHeaders(baseHealthService, getAuthMetadata())
    }
    
    private var isMonitoring = false
    
    private suspend fun checkHealth() {
        try {
            // Try different service names that might be registered
            val serviceNames = listOf(
                "VehicleSearchService",  // Current
                "",                      // Overall server health
                "grpc.VehicleSearchService", // With package name
                "HealthService"          // Health service itself
            )
            
            var healthCheckSucceeded = false
            var latency = 0L
            
            for (serviceName in serviceNames) {
                try {
                    val request = Health.HealthCheckRequest.newBuilder()
                        .setService(serviceName)
                        .build()
                        
                    val startTime = System.currentTimeMillis()
                    val response = getAuthenticatedStub().check(request)
                    latency = System.currentTimeMillis() - startTime
                    
                    val isHealthy = response.status == Health.HealthCheckResponse.ServingStatus.SERVING
                    
                    if (isHealthy) {
                        _connectionStatus.value = _connectionStatus.value.copy(
                            isHealthy = true,
                            latencyMs = latency,
                            lastHeartbeat = System.currentTimeMillis(),
                            errorMessage = null
                        )
                        
                        Log.d("GrpcHealthService", "Health check SUCCESS for '$serviceName': ${latency}ms")
                        healthCheckSucceeded = true
                        break
                    } else {
                        Log.w("GrpcHealthService", "Service '$serviceName' not serving: ${response.status}")
                    }
                } catch (e: StatusException) {
                    // Handle token expiration specifically
                    if (e.status.code == Status.Code.UNAUTHENTICATED) {
                        Log.d("GrpcHealthService", "Token expired during health check, attempting refresh")
                        
                        val refreshed = refreshTokenIfPossible()
                        if (refreshed) {
                            Log.d("GrpcHealthService", "Token refreshed, retrying health check")
                            // Retry with new token
                            try {
                                val retryRequest = Health.HealthCheckRequest.newBuilder()
                                    .setService(serviceName)
                                    .build()
                                val startTime = System.currentTimeMillis()
                                val retryResponse = getAuthenticatedStub().check(retryRequest)
                                latency = System.currentTimeMillis() - startTime
                                
                                val isHealthy = retryResponse.status == Health.HealthCheckResponse.ServingStatus.SERVING
                                
                                if (isHealthy) {
                                    _connectionStatus.value = _connectionStatus.value.copy(
                                        isHealthy = true,
                                        latencyMs = latency,
                                        lastHeartbeat = System.currentTimeMillis(),
                                        errorMessage = null
                                    )
                                    Log.d("GrpcHealthService", "Health check SUCCESS for '$serviceName' after token refresh: ${latency}ms")
                                    healthCheckSucceeded = true
                                    break
                                }
                            } catch (retryException: Exception) {
                                Log.e("GrpcHealthService", "Health check retry failed: ${retryException.message}")
                            }
                        } else {
                            Log.w("GrpcHealthService", "Token refresh failed during health check")
                            sessionManager.clearSession()
                        }
                    } else {
                        Log.w("GrpcHealthService", "Health check failed for '$serviceName': ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.w("GrpcHealthService", "Health check failed for '$serviceName': ${e.message}")
                    // Continue to next service name
                }
            }
            
            if (!healthCheckSucceeded) {
                _connectionStatus.value = _connectionStatus.value.copy(
                    isHealthy = false,
                    latencyMs = latency,
                    errorMessage = "All health checks failed"
                )
                Log.e("GrpcHealthService", "All health check attempts failed")
            }
            
        } catch (e: Exception) {
            Log.e("GrpcHealthService", "Health check error: ${e.message}")
            _connectionStatus.value = _connectionStatus.value.copy(
                isHealthy = false,
                latencyMs = 0L,
                errorMessage = "Health check failed: ${e.message}"
            )
        }
    }
    
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        Log.d("GrpcHealthService", "Starting gRPC health monitoring")
        
        scope.launch {
            while (isActive) {
                try {
                    val currentState = channel.getState(false)
                    val previousState = _connectionStatus.value.state
                    
                    // Only update if state changed
                    if (currentState != previousState) {
                        Log.d("GrpcHealthService", "Connection state changed: $previousState -> $currentState")
                        _connectionStatus.value = _connectionStatus.value.copy(state = currentState)
                    }
                    
                    // Adaptive health checking based on state
                    when (currentState) {
                        ConnectivityState.READY -> {
                            // Only check health occasionally when connected
                            val timeSinceLastCheck = System.currentTimeMillis() - _connectionStatus.value.lastHeartbeat
                            if (timeSinceLastCheck > 10000) { // Check every 10 seconds when READY
                                checkHealth()
                            }
                            delay(2000) // Check state every 2 seconds when ready
                        }
                        ConnectivityState.CONNECTING -> {
                            // More frequent checks during connection
                            _connectionStatus.value = _connectionStatus.value.copy(
                                isHealthy = false,
                                errorMessage = "Connecting..."
                            )
                            delay(1000) // Check every 1 second when connecting
                        }
                        else -> {
                            // Less frequent checks when idle/failed
                            _connectionStatus.value = _connectionStatus.value.copy(
                                isHealthy = false,
                                latencyMs = 0L,
                                errorMessage = "Connection not ready: $currentState"
                            )
                            delay(5000) // Check every 5 seconds when not ready
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e("GrpcHealthService", "Monitoring error: ${e.message}")
                    _connectionStatus.value = _connectionStatus.value.copy(
                        isHealthy = false,
                        errorMessage = "Monitoring error: ${e.message}"
                    )
                    delay(10000) // Wait 10 seconds on error
                }
            }
        }
    }
    
    private suspend fun performHeartbeat() {
        try {
            val request = Health.HeartbeatRequest.newBuilder()
                .setClientId("android-client")
                .setClientTimestamp(System.currentTimeMillis())
                .build()
                
            val startTime = System.currentTimeMillis()

            // Use streaming heartbeat for real-time monitoring with fresh token
            getAuthenticatedStub().heartbeat(kotlinx.coroutines.flow.flowOf(request))
                .collect { response ->
                    val latency = System.currentTimeMillis() - startTime
                    
                    _connectionStatus.value = _connectionStatus.value.copy(
                        isHealthy = response.connectionHealthy,
                        latencyMs = latency,
                        lastHeartbeat = System.currentTimeMillis(),
                        serverVersion = response.serverId,
                        errorMessage = null
                    )
                }
        } catch (e: Exception) {
            Log.e("GrpcHealthService", "Heartbeat failed: ${e.message}")
        }
    }

    /**
     * Attempt to refresh token using saved credentials
     * Returns true if successful, false otherwise
     */
    private suspend fun refreshTokenIfPossible(): Boolean {
        return refreshMutex.withLock {
            if (isRefreshing) {
                Log.d("GrpcHealthService", "Token refresh already in progress")
                return@withLock false
            }
            
            isRefreshing = true
            
            try {
                val email = sessionManager.getEmail()
                val password = sessionManager.getPassword()
                
                if (email.isNullOrBlank() || password.isNullOrBlank()) {
                    Log.w("GrpcHealthService", "No saved credentials available for token refresh")
                    return@withLock false
                }
                
                Log.d("GrpcHealthService", "Attempting token refresh with saved credentials")
                
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
                    val httpResponse = refreshClient.post("${ApiConfig.BASE_URL}${ApiConfig.Endpoints.LOGIN}") {
                        contentType(ContentType.Application.Json)
                        setBody(loginRequest)
                    }
                    
                    if (httpResponse.status.isSuccess()) {
                        val loginResponse: LoginResponse = httpResponse.body()
                        val newToken = loginResponse.token
                        
                        // Update token in session manager
                        sessionManager.saveToken(newToken)
                        
                        Log.d("GrpcHealthService", "Token refresh successful")
                        return@withLock true
                    } else {
                        Log.w("GrpcHealthService", "Token refresh failed: ${httpResponse.status}")
                        return@withLock false
                    }
                } finally {
                    refreshClient.close()
                }
                
            } catch (e: Exception) {
                Log.e("GrpcHealthService", "Token refresh error: ${e.message}")
                return@withLock false
            } finally {
                isRefreshing = false
            }
        }
    }

    suspend fun getConnectionStatus(): GrpcConnectionStatus {
        return try {
            val request = Health.ConnectionStatusRequest.newBuilder()
                .setClientId("android-client")
                .setLastPing(System.currentTimeMillis())
                .build()

            val response = getAuthenticatedStub().getConnectionStatus(request)
            
            _connectionStatus.value.copy(
                activeConnections = response.activeConnections,
                serverVersion = response.serverVersion,
                lastHeartbeat = response.serverTime
            )
        } catch (e: Exception) {
            _connectionStatus.value.copy(errorMessage = e.message)
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
    }
}