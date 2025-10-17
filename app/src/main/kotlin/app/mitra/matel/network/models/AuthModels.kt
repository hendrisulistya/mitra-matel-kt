package app.mitra.matel.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    @SerialName("device_id")
    val deviceId: String,
    val model: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val device: DeviceInfo?
)

@Serializable
data class LoginResponse(
    val token: String
)

@Serializable
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val confirmPassword: String
)

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val data: User? = null
)

@Serializable
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val tier: String = "Basic"
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@Serializable
data class DeviceRecord(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    val model: String,
    @SerialName("last_login") val lastLogin: String,
    @SerialName("last_ip") val lastIp: String,
    @SerialName("last_location") val lastLocation: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("deleted_at") val deletedAt: String
)

@Serializable
data class DeviceConflictResponse(
    @SerialName("current_device") val currentDevice: DeviceRecord,
    @SerialName("requested_device") val requestedDevice: DeviceRecord,
    @SerialName("use_force_login") val useForceLogin: Boolean,
    val error: String? = null,
    val message: String? = null
)

class DeviceConflictException(
    val data: DeviceConflictResponse
) : Exception(data.message ?: "Device conflict detected")
