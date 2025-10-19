package app.mitra.matel.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
data class ProfileDevice(
    val id: String,
    @SerialName("device_uuid") val uuid: String,
    val model: String? = null,
    @SerialName("last_login") val lastLogin: String? = null
)

@Serializable
data class ProfileResponse(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val telephone: String? = null,
    val tier: String,
    val assets: JsonObject = JsonObject(emptyMap()),
    val device: ProfileDevice? = null,
    @SerialName("subscription_status") val subscriptionStatus: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
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

@Serializable
data class AddVehicleRequest(
    @SerialName("is_shared") val isShared: Boolean,
    @SerialName("nama_konsumen") val namaKonsumen: String,
    @SerialName("finance_name") val financeName: String,
    @SerialName("nomor_polisi") val nomorPolisi: String? = null,
    @SerialName("nomor_rangka") val nomorRangka: String? = null,
    @SerialName("nomor_mesin") val nomorMesin: String? = null,
    @SerialName("tipe_kendaraan") val tipeKendaraan: String,
    @SerialName("warna_kendaraan") val warnaKendaraan: String,
    @SerialName("tahun_kendaraan") val tahunKendaraan: String
)

@Serializable
data class ErrorResponse(
    val error: String,
    val details: String
)

class DeviceConflictException(
    val data: DeviceConflictResponse
) : Exception(data.message ?: "Device conflict detected")
