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
    
    suspend fun checkHealth() {
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
                        
                        healthCheckSucceeded = true
                        break
                    }
                } catch (e: StatusException) {
                    // Handle token expiration specifically
                    if (e.status.code == Status.Code.UNAUTHENTICATED) {
                        val refreshed = refreshTokenIfPossible()
                        if (refreshed) {
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
                                    healthCheckSucceeded = true
                                    break
                                }
                            } catch (retryException: Exception) {
                                // Health check retry failed
                            }
                        } else {
                            sessionManager.clearSession()
                        }
                    }
                } catch (e: Exception) {
                    // Continue to next service name
                }
            }
            
            if (!healthCheckSucceeded) {
                _connectionStatus.value = _connectionStatus.value.copy(
                    isHealthy = false,
                    latencyMs = latency,
                    errorMessage = "Service unavailable"
                )
            }
            
        } catch (e: Exception) {
            _connectionStatus.value = _connectionStatus.value.copy(
                isHealthy = false,
                latencyMs = 0L,
                errorMessage = "Connection failed"
            )
        }
    }
    
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        scope.launch {
            while (isActive) {
                try {
                    val currentState = channel.getState(false)
                    val previousState = _connectionStatus.value.state
                    
                    // Only update if state changed
                    if (currentState != previousState) {
                        _connectionStatus.value = _connectionStatus.value.copy(state = currentState)
                    }
                    
                    // Adaptive health checking based on state
                    when (currentState) {
                        ConnectivityState.READY -> {
                            // Only check health occasionally when connected
                            val timeSinceLastCheck = System.currentTimeMillis() - _connectionStatus.value.lastHeartbeat
                            if (timeSinceLastCheck > 15000) { // Check every 15 seconds when READY (reduced frequency)
                                checkHealth()
                            }
                            delay(3000) // Check state every 3 seconds when ready (reduced frequency)
                        }
                        ConnectivityState.CONNECTING -> {
                            // Less frequent checks during connection to reduce overhead
                            _connectionStatus.value = _connectionStatus.value.copy(
                                isHealthy = false,
                                errorMessage = "Connecting..."
                            )
                            delay(2000) // Check every 2 seconds when connecting (reduced overhead)
                        }
                        else -> {
                            // Less frequent checks when idle/failed
                            _connectionStatus.value = _connectionStatus.value.copy(
                                isHealthy = false,
                                latencyMs = 0L,
                                errorMessage = "Connection unavailable"
                            )
                            delay(5000) // Check every 5 seconds when not ready
                        }
                    }
                    
                } catch (e: Exception) {
                    _connectionStatus.value = _connectionStatus.value.copy(
                        isHealthy = false,
                        errorMessage = "Connection monitoring failed"
                    )
                    delay(10000) // Wait 10 seconds on error
                }
            }
        }
    }
    
    private suspend fun performHeartbeat() {
        try {
            // Use unary connection status check instead of streaming heartbeat
            val request = Health.ConnectionStatusRequest.newBuilder()
                .setClientId("android-client")
                .setLastPing(System.currentTimeMillis())
                .build()
                
            val startTime = System.currentTimeMillis()
            val response = getAuthenticatedStub().getConnectionStatus(request)
            val latency = System.currentTimeMillis() - startTime
            
            _connectionStatus.value = _connectionStatus.value.copy(
                isHealthy = response.state == Health.ConnectionStatusResponse.ConnectionState.CONNECTED,
                latencyMs = latency,
                lastHeartbeat = System.currentTimeMillis(),
                serverVersion = response.serverVersion,
                activeConnections = response.activeConnections,
                errorMessage = null
            )
            
        } catch (e: Exception) {
            _connectionStatus.value = _connectionStatus.value.copy(
                isHealthy = false,
                errorMessage = "Status check failed"
            )
        }
    }

    /**
     * Attempt to refresh token using saved credentials
     * Returns true if successful, false otherwise
     */
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
                    val httpResponse = refreshClient.post("${ApiConfig.BASE_URL}${ApiConfig.Endpoints.LOGIN}") {
                        contentType(ContentType.Application.Json)
                        setBody(loginRequest)
                    }
                    
                    if (httpResponse.status.isSuccess()) {
                        val loginResponse: LoginResponse = httpResponse.body()
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