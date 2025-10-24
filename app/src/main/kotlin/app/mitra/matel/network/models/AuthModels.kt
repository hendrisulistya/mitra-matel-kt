package app.mitra.matel.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class VehicleCountResponse(
    val total: Int
)

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
    @SerialName("full_name") val fullName: String,
    val email: String,
    @SerialName("telephone") val phoneNumber: String,
    val password: String
)

@Serializable
data class RegisterResponse(
    val message: String,
    val user: RegisteredUser? = null
)

@Serializable
data class RegisteredUser(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val telephone: String,
    val tier: String = "regular"
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
    @SerialName("sucess") val success: Boolean,
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
    val details: String? = null
)

@Serializable
data class AddVehicleResponse(
    val message: String,
    @SerialName("vehicle_id") val vehicleId: String,
    @SerialName("is_shared") val isShared: Boolean
)

@Serializable
data class SimpleErrorResponse(
    val error: String
)

@Serializable
data class PaymentData(
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("verified_at") val verifiedAt: String? = null,
    @SerialName("verified_by") val verifiedBy: String? = null,
    @SerialName("sender_phone") val senderPhone: String? = null,
    @SerialName("transfer_date") val transferDate: String? = null,
    @SerialName("transfer_note") val transferNote: String? = null,
    @SerialName("sender_address") val senderAddress: String? = null,
    @SerialName("transfer_amount") val transferAmount: Int? = null,
    @SerialName("receiver_address") val receiverAddress: String? = null,
    @SerialName("reference_number") val referenceNumber: String? = null,
    @SerialName("sender_bank_code") val senderBankCode: String? = null,
    @SerialName("sender_bank_name") val senderBankName: String? = null,
    @SerialName("proof_of_transfer") val proofOfTransfer: String? = null,
    @SerialName("proof_uploaded_at") val proofUploadedAt: String? = null,
    @SerialName("receiver_bank_code") val receiverBankCode: String? = null,
    @SerialName("receiver_bank_name") val receiverBankName: String? = null,
    @SerialName("verification_notes") val verificationNotes: String? = null,
    @SerialName("verification_status") val verificationStatus: String? = null,
    @SerialName("receiver_account_name") val receiverAccountName: String? = null,
    @SerialName("sender_account_number") val senderAccountNumber: String? = null,
    @SerialName("receiver_account_number") val receiverAccountNumber: String? = null,
    @SerialName("trial_days") val trialDays: Int? = null,
    @SerialName("activated_at") val activatedAt: String? = null,
    @SerialName("activated_by") val activatedBy: String? = null
)

@Serializable
data class PaymentHistoryItem(
    val id: String,
    @SerialName("subscription_id") val subscriptionId: String,
    @SerialName("order_id") val orderId: String,
    val amount: Int,
    val currency: String,
    val status: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("payment_data") val paymentData: PaymentData,
    @SerialName("paid_at") val paidAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("subscription_plan") val subscriptionPlan: String,
    @SerialName("subscription_days") val subscriptionDays: Int,
    @SerialName("subscription_status") val subscriptionStatus: String,
    @SerialName("subscription_start") val subscriptionStart: String,
    @SerialName("subscription_end") val subscriptionEnd: String
)

typealias PaymentHistoryResponse = List<PaymentHistoryItem>

class DeviceConflictException(
    val data: DeviceConflictResponse
) : Exception(data.message ?: "Device conflict detected")
