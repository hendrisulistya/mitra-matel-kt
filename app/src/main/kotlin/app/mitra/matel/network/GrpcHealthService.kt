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



class GrpcHealthService(
    private val context: Context,
    private val channel: ManagedChannel
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val healthCheckMutex = Mutex()

    


    // Base health service stub without authentication
    private val healthService by lazy {
        HealthServiceGrpcKt.HealthServiceCoroutineStub(channel)
    }
    

    
    suspend fun quickCheck(): Pair<String, Long?>? {
        if (!healthCheckMutex.tryLock()) return null
        return try {
            val request = Health.HealthCheckRequest.newBuilder()
                .setService("")
                .build()
            val start = System.currentTimeMillis()
            val response = healthService.check(request)
            val latency = System.currentTimeMillis() - start
            val status = when (response.status) {
                Health.HealthCheckResponse.ServingStatus.SERVING -> "SERVING"
                Health.HealthCheckResponse.ServingStatus.NOT_SERVING -> "NOT_SERVING"
                Health.HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN -> "SERVICE_UNKNOWN"
                else -> "UNKNOWN"
            }
            Pair(status, latency)
        } catch (e: Exception) {
            null
        } finally {
            healthCheckMutex.unlock()
        }
    }
    

    

    

}