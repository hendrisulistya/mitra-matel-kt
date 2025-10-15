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
import android.util.Log
import app.mitra.matel.utils.SessionManager

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
    
    private val _connectionStatus = MutableStateFlow(GrpcConnectionStatus())
    val connectionStatus: StateFlow<GrpcConnectionStatus> = _connectionStatus.asStateFlow()
    
    // Add authentication metadata like in GrpcService
    private val authMetadata: Metadata = run {
        val metadata = Metadata()
        sessionManager.getToken()?.let { token ->
            val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
            metadata.put(authKey, "Bearer $token")
        }
        metadata
    }
    
    // Apply authentication to health service stub
    private val healthService by lazy {
        val stub = HealthServiceGrpcKt.HealthServiceCoroutineStub(channel)
        MetadataUtils.attachHeaders(stub, authMetadata)
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
                    val response = healthService.check(request)
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
            
            // Use streaming heartbeat for real-time monitoring
            healthService.heartbeat(kotlinx.coroutines.flow.flowOf(request))
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
    
    suspend fun getConnectionStatus(): GrpcConnectionStatus {
        return try {
            val request = Health.ConnectionStatusRequest.newBuilder()
                .setClientId("android-client")
                .setLastPing(System.currentTimeMillis())
                .build()
                
            val response = healthService.getConnectionStatus(request)
            
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