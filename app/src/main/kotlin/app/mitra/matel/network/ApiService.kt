package app.mitra.matel.network

import android.content.Context
import app.mitra.matel.network.models.*
import app.mitra.matel.utils.DeviceUtils
import app.mitra.matel.utils.SessionManager
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class VehicleDetail(
    val id: String,
    val nomor_kontrak: String,
    val nama_konsumen: String,
    val past_due: String,
    val nomor_polisi: String,
    val nomor_rangka: String,
    val nomor_mesin: String,
    val tipe_kendaraan: String,
    val finance_name: String,
    val cabang: String,
    val tahun_kendaraan: String,
    val warna_kendaraan: String
)

@Serializable
data class AvatarUploadRequest(
    val avatar: String
)

@Serializable
data class DeviceLocationRequest(
    val location: String
)

/**
 * API Service for making HTTP requests
 */
class ApiService(
    private val context: Context,
    private val client: io.ktor.client.HttpClient = createHttpClient(context)
) {
    
    private val sessionManager = SessionManager(context)
    
    init {
        // Set auth token from SessionManager if available
        val token = sessionManager.getToken()
        if (token != null) {
            HttpClientFactory.setAuthToken(token)
        }
        // Set SessionManager for token refresh
        HttpClientFactory.setSessionManager(sessionManager)
    }

    /**
     * User Authentication
     */
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val deviceInfo = DeviceUtils.getDeviceInfo(context)

            val response = client.post(ApiConfig.Endpoints.LOGIN) {
                setBody(LoginRequest(email, password, deviceInfo))
            }

            if (response.status.isSuccess()) {
                val loginResponse: LoginResponse = response.body()
                HttpClientFactory.setAuthToken(loginResponse.token)
                Result.success(loginResponse)
            } else if (response.status == HttpStatusCode.Forbidden) {
                val conflict: DeviceConflictResponse = response.body()
                Result.failure(DeviceConflictException(conflict))
            } else {
                val errorText = try {
                    val responseText = response.bodyAsText()
                    // Try to parse JSON and extract error message
                    try {
                        val jsonObject = Json.parseToJsonElement(responseText) as JsonObject
                        jsonObject["error"]?.jsonPrimitive?.content ?: responseText
                    } catch (jsonException: Exception) {
                        // If JSON parsing fails, return the raw response
                        responseText
                    }
                } catch (e: Exception) {
                    response.status.description
                }
                Result.failure(Exception(errorText))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun forceLogin(email: String, password: String): Result<LoginResponse> {
        return try {
            val deviceInfo = DeviceUtils.getDeviceInfo(context)

            val response = client.post(ApiConfig.Endpoints.FORCE_LOGIN) {
                setBody(LoginRequest(email, password, deviceInfo))
            }

            if (response.status.isSuccess()) {
                val loginResponse: LoginResponse = response.body()
                HttpClientFactory.setAuthToken(loginResponse.token)
                Result.success(loginResponse)
            } else {
                val errorText = try {
                    val responseText = response.bodyAsText()
                    // Try to parse JSON and extract error message
                    try {
                        val jsonObject = Json.parseToJsonElement(responseText) as JsonObject
                        jsonObject["error"]?.jsonPrimitive?.content ?: responseText
                    } catch (jsonException: Exception) {
                        // If JSON parsing fails, return the raw response
                        responseText
                    }
                } catch (e: Exception) {
                    response.status.description
                }
                Result.failure(Exception(errorText))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun register(
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ): Result<RegisterResponse> {
        return try {
            val response = client.post(ApiConfig.Endpoints.REGISTER) {
                setBody(RegisterRequest(fullName, email, phoneNumber, password, confirmPassword))
            }
            
            if (response.status.isSuccess()) {
                val registerResponse: RegisterResponse = response.body()
                Result.success(registerResponse)
            } else {
                Result.failure(Exception("Registration failed: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<ApiResponse<Unit>> {
        return try {
            val response = client.post(ApiConfig.Endpoints.LOGOUT)
            
            if (response.status.isSuccess()) {
                // Clear token on logout
                HttpClientFactory.setAuthToken(null)
                Result.success(ApiResponse(true, "Logged out successfully"))
            } else {
                Result.failure(Exception("Logout failed: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * User Profile
     */
    suspend fun getProfile(): Result<ProfileResponse> {
        return try {
            val token = sessionManager.getToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token available"))
            }
            
            // Ensure HttpClient has the latest token
            HttpClientFactory.setAuthToken(token)
            
            val response = client.get(ApiConfig.Endpoints.PROFILE)
            
            if (response.status.isSuccess()) {
                val profileResponse: ProfileResponse = response.body()
                Result.success(profileResponse)
            } else {
                Result.failure(Exception("Failed to get profile: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload Avatar
     */
    suspend fun uploadAvatar(avatarBase64: String): Result<ApiResponse<Unit>> {
        return try {
            val token = sessionManager.getToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token available"))
            }
            
            // Ensure HttpClient has the latest token
            HttpClientFactory.setAuthToken(token)
            
            val requestBody = AvatarUploadRequest(avatar = avatarBase64)
            val response = client.post(ApiConfig.Endpoints.PROFILE_AVATAR) {
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                try {
                    val apiResponse: ApiResponse<Unit> = response.body()
                    Result.success(apiResponse)
                } catch (serializationException: Exception) {
                    // Server might return a different format, try to handle it gracefully
                    val responseText = response.bodyAsText()
                    
                    // If the response is successful but doesn't match our expected format,
                    // create a success response manually
                    if (response.status.value in 200..299) {
                        val successResponse = ApiResponse<Unit>(
                            success = true,
                            message = "Avatar uploaded successfully",
                            data = Unit
                        )
                        Result.success(successResponse)
                    } else {
                        Result.failure(Exception("Unexpected response format: $responseText"))
                    }
                }
            } else {
                val errorText = try {
                    response.bodyAsText()
                } catch (e: Exception) {
                    response.status.description
                }
                Result.failure(Exception("Failed to upload avatar: $errorText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Vehicle Search
     */
    suspend fun searchVehicle(plateNumber: String): Result<ApiResponse<JsonObject>> {
        return try {
            val response = client.get(ApiConfig.Endpoints.SEARCH_VEHICLE) {
                parameter("plateNumber", plateNumber)
            }
            
            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponse<JsonObject>>()
                Result.success(apiResponse)
            } else {
                Result.failure(Exception("Search failed: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Vehicle Detail
     */
    suspend fun getVehicleDetail(vehicleId: String): Result<VehicleDetail> {
        return try {
            val response = client.get(ApiConfig.Endpoints.DETAIL_VEHICLE.replace(":id", vehicleId))
            
            if (response.status.isSuccess()) {
                val vehicleDetail: VehicleDetail = response.body()
                Result.success(vehicleDetail)
            } else {
                Result.failure(Exception("Failed to get vehicle detail: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add Vehicle
     */
    suspend fun addVehicle(request: AddVehicleRequest): Result<AddVehicleResponse> {
        return try {
            val response = client.post(ApiConfig.Endpoints.ADD_VEHICLE) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                val addVehicleResponse = response.body<AddVehicleResponse>()
                Result.success(addVehicleResponse)
            } else {
                // Handle different error response formats based on status code
                try {
                    when (response.status) {
                        HttpStatusCode.BadRequest -> {
                            // 400 - Validation errors with error/details structure
                            val errorResponse: ErrorResponse = response.body()
                            val serverMessage = if (!errorResponse.details.isNullOrBlank()) {
                                "${errorResponse.error}: ${errorResponse.details}"
                            } else {
                                errorResponse.error
                            }
                            Result.failure(Exception(serverMessage))
                        }
                        HttpStatusCode.Unauthorized -> {
                            // 401 - Authentication errors
                            val errorResponse: SimpleErrorResponse = response.body()
                            Result.failure(Exception(errorResponse.error))
                        }
                        HttpStatusCode.Conflict -> {
                            // 409 - Conflict errors
                            val errorResponse: SimpleErrorResponse = response.body()
                            Result.failure(Exception(errorResponse.error))
                        }
                        HttpStatusCode.InternalServerError -> {
                            // 500 - Internal server errors
                            val errorResponse: SimpleErrorResponse = response.body()
                            Result.failure(Exception(errorResponse.error))
                        }
                        else -> {
                            // Fallback for other status codes
                            Result.failure(Exception("Gagal menambahkan: ${response.status.description}"))
                        }
                    }
                } catch (parseException: Exception) {
                    // Fallback to status description if parsing fails
                    Result.failure(Exception("Gagal menambahkan: ${response.status.description}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVehicleCount(): Result<VehicleCountResponse> {
        return try {
            val response = client.get(ApiConfig.Endpoints.VEHICLES_COUNT)
            
            if (response.status.isSuccess()) {
                val vehicleCountResponse: VehicleCountResponse = response.body()
                Result.success(vehicleCountResponse)
            } else {
                Result.failure(Exception("Failed to get vehicle count: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update Device Location
     */
    suspend fun patchDeviceLocation(location: String): Result<ApiResponse<Unit>> {
        return try {
            // Ensure we have a valid token
            val token = sessionManager.getToken()
            if (token == null) {
                return Result.failure(Exception("No authentication token available"))
            }
            
            // Ensure HttpClient has the latest token
            HttpClientFactory.setAuthToken(token)
            
            val requestBody = DeviceLocationRequest(location = location)
            val response = client.patch(ApiConfig.Endpoints.DEVICE_LOCATION) {
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                try {
                    val apiResponse: ApiResponse<Unit> = response.body()
                    Result.success(apiResponse)
                } catch (serializationException: Exception) {
                    // Server might return a different format, try to handle it gracefully
                    val responseText = response.bodyAsText()
                    
                    // If the response is successful but doesn't match our expected format,
                    // create a success response manually
                    if (response.status.value in 200..299) {
                        val successResponse = ApiResponse<Unit>(
                            success = true,
                            message = "Device location updated successfully",
                            data = Unit
                        )
                        Result.success(successResponse)
                    } else {
                        Result.failure(Exception("Unexpected response format: $responseText"))
                    }
                }
            } else {
                val errorText = try {
                    // Try to parse JSON error response
                    val responseText = response.bodyAsText()
                    val jsonObject = Json.parseToJsonElement(responseText) as? JsonObject
                    jsonObject?.get("error")?.jsonPrimitive?.content ?: responseText
                } catch (e: Exception) {
                    response.status.description
                }
                Result.failure(Exception("Failed to update device location: $errorText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Note: ApiService requires Context, so create instance where Context is available
// Example: val apiService = ApiService(context = applicationContext)
