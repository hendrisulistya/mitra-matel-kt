package app.mitra.matel.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.mitra.matel.network.models.ProfileResponse
import app.mitra.matel.network.models.ProfileDevice
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * SessionManager - Handles secure storage of authentication data
 * Uses EncryptedSharedPreferences for secure storage with caching for performance
 * Includes fallback to regular SharedPreferences if encryption fails
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_NAME = "mitra_matel_session"
        private const val FALLBACK_PREFS_NAME = "mitra_matel_session_fallback"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        
        // Profile data keys
        private const val KEY_PROFILE_ID = "profile_id"
        private const val KEY_PROFILE_FULL_NAME = "profile_full_name"
        private const val KEY_PROFILE_EMAIL = "profile_email"
        private const val KEY_PROFILE_TELEPHONE = "profile_telephone"
        private const val KEY_PROFILE_TIER = "profile_tier"
        private const val KEY_PROFILE_SUBSCRIPTION_STATUS = "profile_subscription_status"
        private const val KEY_PROFILE_CREATED_AT = "profile_created_at"
        private const val KEY_PROFILE_UPDATED_AT = "profile_updated_at"
        private const val KEY_PROFILE_ASSETS = "profile_assets"
        
        // Device keys
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_UUID = "device_uuid"
        private const val KEY_DEVICE_MODEL = "device_model"
        private const val KEY_DEVICE_LAST_LOGIN = "device_last_login"
        
        // Announcement dismissal key
        private const val KEY_ANNOUNCEMENT_DISMISSED = "announcement_dismissed"
        
        // Keyboard layout preference key
        private const val KEY_KEYBOARD_LAYOUT = "keyboard_layout"
        
        // Vehicle history key
        private const val KEY_VEHICLE_HISTORY = "vehicle_history"
    }
    
    /**
     * Lazy initialization of SharedPreferences with error handling and fallback
     */
    private val sharedPreferences: SharedPreferences by lazy {
        createSecurePreferences() ?: createFallbackPreferences()
    }
    
    /**
     * Attempts to create EncryptedSharedPreferences with error handling
     */
    private fun createSecurePreferences(): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also {
                Log.d(TAG, "EncryptedSharedPreferences created successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences: ${e.message}", e)
            
            // Try to clear corrupted preferences and retry once
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().clear().apply()
                Log.d(TAG, "Cleared corrupted preferences, retrying...")
                
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).also {
                    Log.d(TAG, "EncryptedSharedPreferences created successfully after retry")
                }
            } catch (retryException: Exception) {
                Log.e(TAG, "Retry failed, falling back to regular SharedPreferences: ${retryException.message}", retryException)
                null
            }
        }
    }
    
    /**
     * Creates fallback regular SharedPreferences when encryption fails
     */
    private fun createFallbackPreferences(): SharedPreferences {
        Log.w(TAG, "Using fallback regular SharedPreferences (data will not be encrypted)")
        return context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Performance optimization: Cache frequently accessed values
    private var cachedToken: String? = null
    private var cachedLoginState: Boolean? = null
    private var cachedEmail: String? = null
    private var tokenCacheValid = false
    private var loginStateCacheValid = false
    private var emailCacheValid = false
    
    // Cache for profile data
    private var cachedProfile: ProfileResponse? = null
    private var profileCacheValid = false
    
    /**
     * Save authentication token
     */
    fun saveToken(token: String) {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_TOKEN, token)
                putBoolean(KEY_IS_LOGGED_IN, true)
                apply()
            }
            // Update cache
            cachedToken = token
            cachedLoginState = true
            tokenCacheValid = true
            loginStateCacheValid = true
            Log.d(TAG, "Token saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save token: ${e.message}", e)
        }
    }
    
    /**
     * Get authentication token (cached for performance)
     */
    fun getToken(): String? {
        return try {
            if (!tokenCacheValid) {
                cachedToken = sharedPreferences.getString(KEY_TOKEN, null)
                tokenCacheValid = true
            }
            cachedToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get token: ${e.message}", e)
            null
        }
    }
    
    /**
     * Save user credentials
     */
    fun saveCredentials(email: String, password: String) {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_EMAIL, email)
                putString(KEY_PASSWORD, password)
                apply()
            }
            // Update cache
            cachedEmail = email
            emailCacheValid = true
            Log.d(TAG, "Credentials saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save credentials: ${e.message}", e)
        }
    }
    
    /**
     * Get user email (cached for performance)
     */
    fun getEmail(): String? {
        return try {
            if (!emailCacheValid) {
                cachedEmail = sharedPreferences.getString(KEY_EMAIL, null)
                emailCacheValid = true
            }
            cachedEmail
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get email: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get user password
     */
    fun getPassword(): String? {
        return try {
            sharedPreferences.getString(KEY_PASSWORD, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get password: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check if user is logged in (cached for performance)
     */
    fun isLoggedIn(): Boolean {
        return try {
            if (!loginStateCacheValid) {
                cachedLoginState = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && 
                                  getToken() != null
                loginStateCacheValid = true
            }
            cachedLoginState ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check login status: ${e.message}", e)
            false
        }
    }
    
    /**
     * Add vehicle to access history
     */
    fun addVehicleToHistory(vehicleId: String, nomorPolisi: String) {
        try {
            val currentHistory = getVehicleHistory().toMutableList()
            val newItem = VehicleHistoryItem(
                vehicleId = vehicleId,
                nomorPolisi = nomorPolisi,
                accessTime = System.currentTimeMillis()
            )
            
            // Remove existing entry if it exists (to update timestamp)
            currentHistory.removeAll { it.vehicleId == vehicleId }
            
            // Add new item at the beginning
            currentHistory.add(0, newItem)
            
            // Keep only last 50 items
            if (currentHistory.size > 50) {
                currentHistory.subList(50, currentHistory.size).clear()
            }
            
            // Save updated history
            val json = Json.encodeToString(currentHistory)
            sharedPreferences.edit()
                .putString(KEY_VEHICLE_HISTORY, json)
                .apply()
                
            Log.d(TAG, "Vehicle added to history: $nomorPolisi")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add vehicle to history: ${e.message}", e)
        }
    }
    
    /**
     * Get vehicle access history
     */
    fun getVehicleHistory(): List<VehicleHistoryItem> {
        return try {
            val json = sharedPreferences.getString(KEY_VEHICLE_HISTORY, null)
            if (json != null) {
                Json.decodeFromString<List<VehicleHistoryItem>>(json)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get vehicle history: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Clear vehicle history
     */
    fun clearVehicleHistory() {
        try {
            sharedPreferences.edit()
                .remove(KEY_VEHICLE_HISTORY)
                .apply()
            Log.d(TAG, "Vehicle history cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear vehicle history: ${e.message}", e)
        }
    }
    
    /**
     * Clear user session
     */
    fun clearSession() {
        try {
            sharedPreferences.edit().apply {
                remove(KEY_TOKEN)
                remove(KEY_IS_LOGGED_IN)
                apply()
            }
            // Clear cache
            cachedToken = null
            cachedLoginState = false
            tokenCacheValid = false
            loginStateCacheValid = false
            Log.d(TAG, "Session cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear session: ${e.message}", e)
        }
    }
    
    /**
     * Save profile data
     */
    fun saveProfile(profile: ProfileResponse) {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_PROFILE_ID, profile.id)
                putString(KEY_PROFILE_FULL_NAME, profile.fullName)
                putString(KEY_PROFILE_EMAIL, profile.email)
                putString(KEY_PROFILE_TELEPHONE, profile.telephone)
                putString(KEY_PROFILE_TIER, profile.tier)
                putString(KEY_PROFILE_SUBSCRIPTION_STATUS, profile.subscriptionStatus)
                putString(KEY_PROFILE_CREATED_AT, profile.createdAt)
                putString(KEY_PROFILE_UPDATED_AT, profile.updatedAt)
                
                // Save assets data (including avatar)
                try {
                    val assetsJson = Json.encodeToString(profile.assets)
                    putString(KEY_PROFILE_ASSETS, assetsJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to serialize assets: ${e.message}")
                }
                
                // Save device data if available
                profile.device?.let { device ->
                    putString(KEY_DEVICE_ID, device.id)
                    putString(KEY_DEVICE_UUID, device.uuid)
                    putString(KEY_DEVICE_MODEL, device.model)
                    putString(KEY_DEVICE_LAST_LOGIN, device.lastLogin)
                }
                
                apply()
            }
            
            // Update cache
            cachedProfile = profile
            profileCacheValid = true
            Log.d(TAG, "Profile saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save profile: ${e.message}", e)
        }
    }
    
    /**
     * Get profile data (cached for performance)
     */
    fun getProfile(): ProfileResponse? {
        return try {
            if (!profileCacheValid) {
                val id = sharedPreferences.getString(KEY_PROFILE_ID, null)
                val fullName = sharedPreferences.getString(KEY_PROFILE_FULL_NAME, null)
                val email = sharedPreferences.getString(KEY_PROFILE_EMAIL, null)
                val telephone = sharedPreferences.getString(KEY_PROFILE_TELEPHONE, null)
                val tier = sharedPreferences.getString(KEY_PROFILE_TIER, null)
                val subscriptionStatus = sharedPreferences.getString(KEY_PROFILE_SUBSCRIPTION_STATUS, null)
                val createdAt = sharedPreferences.getString(KEY_PROFILE_CREATED_AT, null)
                val updatedAt = sharedPreferences.getString(KEY_PROFILE_UPDATED_AT, null)
                
                // Get assets data
                val assetsJson = sharedPreferences.getString(KEY_PROFILE_ASSETS, null)
                val assets = try {
                    if (assetsJson != null) {
                        Json.decodeFromString<JsonObject>(assetsJson)
                    } else {
                        JsonObject(emptyMap())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to deserialize assets: ${e.message}")
                    JsonObject(emptyMap())
                }
                
                // Get device data
                val deviceId = sharedPreferences.getString(KEY_DEVICE_ID, null)
                val deviceUuid = sharedPreferences.getString(KEY_DEVICE_UUID, null)
                val deviceModel = sharedPreferences.getString(KEY_DEVICE_MODEL, null)
                val deviceLastLogin = sharedPreferences.getString(KEY_DEVICE_LAST_LOGIN, null)
                
                cachedProfile = if (id != null && fullName != null && email != null) {
                    val device = if (deviceId != null && deviceUuid != null) {
                        ProfileDevice(
                            id = deviceId,
                            uuid = deviceUuid,
                            model = deviceModel,
                            lastLogin = deviceLastLogin
                        )
                    } else null
                    
                    ProfileResponse(
                        id = id,
                        email = email,
                        fullName = fullName,
                        telephone = telephone,
                        tier = tier ?: "free",
                        assets = assets, // Use the retrieved assets instead of empty map
                        device = device,
                        subscriptionStatus = subscriptionStatus ?: "inactive",
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                } else null
                
                profileCacheValid = true
            }
            cachedProfile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get user's full name from profile
     */
    fun getFullName(): String? {
        return getProfile()?.fullName
    }
    
    /**
     * Get user's tier from profile
     */
    fun getTier(): String? {
        return getProfile()?.tier
    }
    
    /**
     * Clear profile data
     */
    fun clearProfile() {
        try {
            sharedPreferences.edit().apply {
                remove(KEY_PROFILE_ID)
                remove(KEY_PROFILE_FULL_NAME)
                remove(KEY_PROFILE_EMAIL)
                remove(KEY_PROFILE_TELEPHONE)
                remove(KEY_PROFILE_TIER)
                remove(KEY_PROFILE_SUBSCRIPTION_STATUS)
                remove(KEY_PROFILE_CREATED_AT)
                remove(KEY_PROFILE_UPDATED_AT)
                remove(KEY_DEVICE_ID)
                remove(KEY_DEVICE_UUID)
                remove(KEY_DEVICE_MODEL)
                remove(KEY_DEVICE_LAST_LOGIN)
                apply()
            }
            
            // Clear cache
            cachedProfile = null
            profileCacheValid = false
            Log.d(TAG, "Profile cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear profile: ${e.message}", e)
        }
    }

    /**
     * Clear all stored data
     */
    fun clearAll() {
        try {
            sharedPreferences.edit().clear().apply()
            // Clear all cache
            cachedToken = null
            cachedLoginState = null
            cachedEmail = null
            cachedProfile = null
            tokenCacheValid = false
            loginStateCacheValid = false
            emailCacheValid = false
            profileCacheValid = false
            Log.d(TAG, "All data cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all data: ${e.message}", e)
        }
    }
    
    /**
     * Set announcement dismissal state
     */
    fun setAnnouncementDismissed(dismissed: Boolean) {
        try {
            sharedPreferences.edit()
                .putBoolean(KEY_ANNOUNCEMENT_DISMISSED, dismissed)
                .apply()
            Log.d(TAG, "Announcement dismissal state saved: $dismissed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save announcement dismissal state: ${e.message}", e)
        }
    }
    
    /**
     * Check if announcement has been dismissed
     */
    fun isAnnouncementDismissed(): Boolean {
        return try {
            sharedPreferences.getBoolean(KEY_ANNOUNCEMENT_DISMISSED, false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get announcement dismissal state: ${e.message}", e)
            false
        }
    }
    
    /**
     * Save keyboard layout preference
     */
    fun saveKeyboardLayout(layoutName: String) {
        try {
            sharedPreferences.edit()
                .putString(KEY_KEYBOARD_LAYOUT, layoutName)
                .apply()
            Log.d(TAG, "Keyboard layout saved: $layoutName")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving keyboard layout", e)
        }
    }
    
    /**
     * Get saved keyboard layout preference
     * Returns "QWERTY1" as default if no preference is saved
     */
    fun getKeyboardLayout(): String {
        return try {
            sharedPreferences.getString(KEY_KEYBOARD_LAYOUT, "QWERTY1") ?: "QWERTY1"
        } catch (e: Exception) {
            Log.e(TAG, "Error reading keyboard layout", e)
            "QWERTY1"
        }
    }
    
    /**
     * Remove a specific vehicle from history
     */
    fun removeVehicleFromHistory(vehicleId: String) {
        try {
            val currentHistory = getVehicleHistory().toMutableList()
            val updatedHistory = currentHistory.filter { it.vehicleId != vehicleId }
            
            val historyJson = Json.encodeToString(updatedHistory)
            sharedPreferences.edit().putString(KEY_VEHICLE_HISTORY, historyJson).apply()
            
            Log.d(TAG, "Vehicle removed from history: $vehicleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing vehicle from history", e)
        }
    }
}

/**
 * Data class for vehicle history items
 */
@Serializable
data class VehicleHistoryItem(
    val vehicleId: String,
    val nomorPolisi: String,
    val accessTime: Long // Unix timestamp
)
