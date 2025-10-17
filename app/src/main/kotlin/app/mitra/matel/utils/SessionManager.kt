package app.mitra.matel.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.mitra.matel.network.models.ProfileResponse
import app.mitra.matel.network.models.ProfileDevice
import kotlinx.serialization.json.JsonObject

/**
 * SessionManager - Handles secure storage of authentication data
 * Uses EncryptedSharedPreferences for secure storage with caching for performance
 */
class SessionManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
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
    
    companion object {
        private const val PREFS_NAME = "mitra_matel_session"
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
        
        // Device data keys
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_UUID = "device_uuid"
        private const val KEY_DEVICE_MODEL = "device_model"
        private const val KEY_DEVICE_LAST_LOGIN = "device_last_login"
    }
    
    /**
     * Save authentication token
     */
    fun saveToken(token: String) {
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
    }
    
    /**
     * Get authentication token (cached for performance)
     */
    fun getToken(): String? {
        if (!tokenCacheValid) {
            cachedToken = sharedPreferences.getString(KEY_TOKEN, null)
            tokenCacheValid = true
        }
        return cachedToken
    }
    
    /**
     * Save user credentials
     */
    fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit().apply {
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
            apply()
        }
        // Update cache
        cachedEmail = email
        emailCacheValid = true
    }
    
    /**
     * Get user email (cached for performance)
     */
    fun getEmail(): String? {
        if (!emailCacheValid) {
            cachedEmail = sharedPreferences.getString(KEY_EMAIL, null)
            emailCacheValid = true
        }
        return cachedEmail
    }
    
    /**
     * Get user password
     */
    fun getPassword(): String? {
        return sharedPreferences.getString(KEY_PASSWORD, null)
    }
    
    /**
     * Check if user is logged in (cached for performance)
     */
    fun isLoggedIn(): Boolean {
        if (!loginStateCacheValid) {
            cachedLoginState = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && 
                              getToken() != null
            loginStateCacheValid = true
        }
        return cachedLoginState ?: false
    }
    
    /**
     * Clear user session
     */
    fun clearSession() {
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
    }
    
    /**
     * Save profile data
     */
    fun saveProfile(profile: ProfileResponse) {
        sharedPreferences.edit().apply {
            putString(KEY_PROFILE_ID, profile.id)
            putString(KEY_PROFILE_FULL_NAME, profile.fullName)
            putString(KEY_PROFILE_EMAIL, profile.email)
            putString(KEY_PROFILE_TELEPHONE, profile.telephone)
            putString(KEY_PROFILE_TIER, profile.tier)
            putString(KEY_PROFILE_SUBSCRIPTION_STATUS, profile.subscriptionStatus)
            putString(KEY_PROFILE_CREATED_AT, profile.createdAt)
            putString(KEY_PROFILE_UPDATED_AT, profile.updatedAt)
            
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
    }
    
    /**
     * Get profile data (cached for performance)
     */
    fun getProfile(): ProfileResponse? {
        if (!profileCacheValid) {
            val id = sharedPreferences.getString(KEY_PROFILE_ID, null)
            val fullName = sharedPreferences.getString(KEY_PROFILE_FULL_NAME, null)
            val email = sharedPreferences.getString(KEY_PROFILE_EMAIL, null)
            val telephone = sharedPreferences.getString(KEY_PROFILE_TELEPHONE, null)
            val tier = sharedPreferences.getString(KEY_PROFILE_TIER, null)
            val subscriptionStatus = sharedPreferences.getString(KEY_PROFILE_SUBSCRIPTION_STATUS, null)
            val createdAt = sharedPreferences.getString(KEY_PROFILE_CREATED_AT, null)
            val updatedAt = sharedPreferences.getString(KEY_PROFILE_UPDATED_AT, null)
            
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
                    assets = JsonObject(emptyMap()), // Default value, not stored in preferences
                    device = device,
                    subscriptionStatus = subscriptionStatus ?: "inactive",
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            } else null
            
            profileCacheValid = true
        }
        return cachedProfile
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
    }

    /**
     * Clear all stored data
     */
    fun clearAll() {
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
    }
}
