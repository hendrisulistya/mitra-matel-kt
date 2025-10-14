package app.mitra.matel.network

import android.content.Context
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
import android.util.Log

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
    
    private val _connectionStatus = MutableStateFlow(GrpcConnectionStatus())
    val connectionStatus: StateFlow<GrpcConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val healthService by lazy {
        HealthServiceGrpcKt.HealthServiceCoroutineStub(channel)
    }
    
    private var isMonitoring = false
    
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        // Monitor channel connectivity state
        scope.launch {
            while (isActive) {
                try {
                    val currentState = channel.getState(false)
                    _connectionStatus.value = _connectionStatus.value.copy(state = currentState)
                    
                    // Check health if connected
                    if (currentState == ConnectivityState.READY) {
                        checkHealth()
                    }
                    
                    delay(1000) // Check every second
                } catch (e: Exception) {
                    Log.e("GrpcHealthService", "Monitoring error: ${e.message}")
                    _connectionStatus.value = _connectionStatus.value.copy(
                        isHealthy = false,
                        errorMessage = e.message
                    )
                }
            }
        }
        
        // Periodic heartbeat
        scope.launch {
            while (isActive) {
                try {
                    if (channel.getState(false) == ConnectivityState.READY) {
                        performHeartbeat()
                    }
                    delay(5000) // Heartbeat every 5 seconds
                } catch (e: Exception) {
                    Log.e("GrpcHealthService", "Heartbeat error: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun checkHealth() {
        try {
            val request = Health.HealthCheckRequest.newBuilder()
                .setService("VehicleSearchService")
                .build()
                
            val startTime = System.currentTimeMillis()
            val response = healthService.check(request)
            val latency = System.currentTimeMillis() - startTime
            
            _connectionStatus.value = _connectionStatus.value.copy(
                isHealthy = response.status == Health.HealthCheckResponse.ServingStatus.SERVING,
                latencyMs = latency,
                lastHeartbeat = System.currentTimeMillis(),
                errorMessage = if (response.status == Health.HealthCheckResponse.ServingStatus.SERVING) null else response.message
            )
        } catch (e: Exception) {
            _connectionStatus.value = _connectionStatus.value.copy(
                isHealthy = false,
                errorMessage = e.message
            )
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