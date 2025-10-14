package app.mitra.matel.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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
    
    companion object {
        private const val PREFS_NAME = "mitra_matel_session"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
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
     * Clear all stored data
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        // Clear all cache
        cachedToken = null
        cachedLoginState = null
        cachedEmail = null
        tokenCacheValid = false
        loginStateCacheValid = false
        emailCacheValid = false
    }
}
