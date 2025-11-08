package app.mitra.matel.network

import android.content.Context
import android.util.Log
import grpc.Health
import grpc.HealthServiceGrpcKt
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
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
import io.grpc.StatusException

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
    private val healthCheckMutex = Mutex()
    private var lastHealthCheckTime = 0L
    
    private val _connectionStatus = MutableStateFlow(GrpcConnectionStatus())
    val connectionStatus: StateFlow<GrpcConnectionStatus> = _connectionStatus.asStateFlow()

    // Base health service stub without authentication
    private val healthService by lazy {
        HealthServiceGrpcKt.HealthServiceCoroutineStub(channel)
    }
    
    private var isMonitoring = false
    
    suspend fun checkHealth() {
        // Prevent concurrent health checks to avoid multiple token refresh attempts
        if (!healthCheckMutex.tryLock()) {
            Log.d("gRPC Health", "Health check already in progress - skipping concurrent check")
            return
        }
        
        try {
            // Try different service names that might be registered
            val serviceNames = listOf(
                "VehicleService",
                "",
                "grpc.VehicleService",
                "HealthService"
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
                    
                    // Log health check response
                    Log.d("gRPC Health", "Health check response: ${response.status} - Service: $serviceName")
                    
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
                    // Log gRPC error but don't handle authentication errors for health checks
                    Log.e("gRPC Health", "StatusException: ${e.status.code} - ${e.status.description}")
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
        } finally {
            healthCheckMutex.unlock()
            lastHealthCheckTime = System.currentTimeMillis()
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
    

    
    fun stopMonitoring() {
        isMonitoring = false
    }
}