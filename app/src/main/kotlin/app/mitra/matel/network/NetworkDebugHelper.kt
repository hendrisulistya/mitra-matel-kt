package app.mitra.matel.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

import io.ktor.client.request.*

/**
 * Network Debug Helper
 * Use this to verify network connectivity and API configuration
 */
object NetworkDebugHelper {
    
    private const val TAG = "NetworkDebug"
    
    /**
     * Log all network and API configuration info
     * Call this in your screen or ViewModel to debug
     */
    fun logNetworkInfo(context: Context) {
        Log.d(TAG, "=".repeat(50))
        Log.d(TAG, "NETWORK DEBUG INFO")
        Log.d(TAG, "=".repeat(50))
        
        // Current environment
        Log.d(TAG, "Environment: ${ApiConfig.getCurrentEnvironment()}")
        Log.d(TAG, "Base URL: ${ApiConfig.BASE_URL}")
        Log.d(TAG, "Login Endpoint: ${ApiConfig.BASE_URL}${ApiConfig.Endpoints.LOGIN}")
        
        // Network connectivity
        val isConnected = isNetworkAvailable(context)
        Log.d(TAG, "Network Connected: $isConnected")
        
        if (isConnected) {
            val networkType = getNetworkType(context)
            Log.d(TAG, "Network Type: $networkType")
        }
        
        Log.d(TAG, "=".repeat(50))
    }
    
    /**
     * Check if network is available
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Get network type (WiFi, Cellular, etc.)
     */
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "None"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }
    
    /**
     * Test connectivity to base URL
     * Returns result message
     */
    suspend fun testConnection(): String {
        return try {
            val response = httpClient.get(ApiConfig.BASE_URL)
            "Success: Connected to ${ApiConfig.BASE_URL} - Status: ${response.status}"
        } catch (e: Exception) {
            "Failed: ${e.message}"
        }
    }
}
