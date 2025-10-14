package app.mitra.matel.network

import android.content.Context
import grpc.Vehicle
import grpc.VehicleSearchServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList
import app.mitra.matel.utils.SessionManager

data class VehicleResult(
    val id: String,
    val nomorPolisi: String,
    val tipeKendaraan: String,
    val dataVersion: String,
    val financeName: String
)

class GrpcService(private val context: Context) {
    private val sessionManager = SessionManager(context)
    
    private val channel: ManagedChannel by lazy {
        ManagedChannelBuilder.forAddress("localhost", 50051)
            .usePlaintext()
            .build()
    }
    
    private val vehicleService by lazy {
        val stub = VehicleSearchServiceGrpcKt.VehicleSearchServiceCoroutineStub(channel)
        
        // Add authentication headers
        val token = sessionManager.getToken()
        if (token != null) {
            val metadata = Metadata()
            val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
            metadata.put(authKey, "Bearer $token")
            
            // Attach metadata to all calls
            MetadataUtils.attachHeaders(stub, metadata)
        } else {
            stub
        }
    }

    suspend fun searchVehicle(
        searchType: String,
        searchValue: String
    ): Result<List<VehicleResult>> {
        return try {
            // Check if user is authenticated
            if (!sessionManager.isLoggedIn()) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val request = Vehicle.VehicleSearchRequest.newBuilder().apply {
                when (searchType) {
                    "nopol" -> nomorPolisi = searchValue
                    "noka" -> nomorRangka = searchValue
                    "nosin" -> nomorMesin = searchValue
                }
            }.build()

            val results = mutableListOf<VehicleResult>()
            vehicleService.searchVehicle(request)
                .catch { e -> 
                    throw e
                }
                .toList()
                .forEach { response ->
                    results.add(
                        VehicleResult(
                            id = response.id,
                            nomorPolisi = response.nomorPolisi,
                            tipeKendaraan = response.tipeKendaraan,
                            dataVersion = response.dataVersion,
                            financeName = response.financeName
                        )
                    )
                }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        if (!channel.isShutdown) {
            channel.shutdown()
        }
    }
}