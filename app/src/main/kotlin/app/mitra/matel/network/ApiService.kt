package app.mitra.matel.network

import android.content.Context
import app.mitra.matel.network.models.*
import app.mitra.matel.utils.DeviceUtils
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * API Service for making HTTP requests
 */
class ApiService(
    private val client: io.ktor.client.HttpClient = httpClient,
    private val context: Context
) {

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
                Result.failure(Exception("Login failed: ${response.status.description}"))
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
                Result.failure(Exception("Force login failed: ${response.status.description}"))
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
    suspend fun getProfile(): Result<ApiResponse<User>> {
        return try {
            val response = client.get(ApiConfig.Endpoints.PROFILE)
            
            if (response.status.isSuccess()) {
                val apiResponse: ApiResponse<User> = response.body()
                Result.success(apiResponse)
            } else {
                Result.failure(Exception("Failed to get profile: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Vehicle Search
     */
    suspend fun searchVehicle(plateNumber: String): Result<ApiResponse<Any>> {
        return try {
            val response = client.get(ApiConfig.Endpoints.SEARCH_VEHICLE) {
                parameter("plateNumber", plateNumber)
            }
            
            if (response.status.isSuccess()) {
                val apiResponse: ApiResponse<Any> = response.body()
                Result.success(apiResponse)
            } else {
                Result.failure(Exception("Search failed: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Note: ApiService requires Context, so create instance where Context is available
// Example: val apiService = ApiService(context = applicationContext)
