package app.mitra.matel.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import app.mitra.matel.network.models.DeviceInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*
import java.security.MessageDigest
import kotlinx.coroutines.*
import app.mitra.matel.utils.SessionManager

object DeviceUtils {
    
    
    /**
     * Get device model name
     * Returns manufacturer + model (e.g., "Samsung SM-G991B")
     */
    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        val model = Build.MODEL
        
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }
    
    /**
     * Get device location if available
     */
    suspend fun getDeviceLocation(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Check location permissions
                val hasFineLocation = ContextCompat.checkSelfPermission(
                    context, 
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                val hasCoarseLocation = ContextCompat.checkSelfPermission(
                    context, 
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                if (!hasFineLocation && !hasCoarseLocation) {
                    return@withContext null
                }
                
                // Get location using callback pattern (simpler approach)
                val fusedLocationClient: FusedLocationProviderClient = 
                    LocationServices.getFusedLocationProviderClient(context)
                
                var locationResult: Location? = null
                var locationError: Exception? = null
                
                val latch = CompletableDeferred<Unit>()
                
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        locationResult = location
                        latch.complete(Unit)
                    }
                    .addOnFailureListener { exception ->
                        locationError = exception
                        latch.complete(Unit)
                    }
                
                // Wait for the callback to complete
                latch.await()
                
                locationResult?.let {
                    "${it.latitude},${it.longitude}"
                } ?: run {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get deterministic app-specific device info (cached)
     */
    fun detDeviceInfo(context: Context): DeviceInfo {
        val manager = SessionManager.getInstance(context)
        val cached = manager.getStoredLocalDeviceInfo()
        if (cached != null) return cached
        val androidId = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (e: Exception) { null }
        val deviceId = androidId ?: generateAppSpecificHexId16()
        val info = DeviceInfo(deviceId = deviceId, model = getDeviceModel())
        manager.saveLocalDeviceInfo(info)
        return info
    }
    
    private fun generateAppSpecificHexId16(): String {
        val rnd = UUID.randomUUID().toString()
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(rnd.toByteArray())
        val hex = bytes.joinToString("") { b -> ((b.toInt() and 0xFF).toString(16)).padStart(2, '0') }
        return hex.take(16)
    }
    
    /**
     * Get device details for debugging
     */
    fun getDeviceDetails(): String {
        return """
            Manufacturer: ${Build.MANUFACTURER}
            Model: ${Build.MODEL}
            Brand: ${Build.BRAND}
            Device: ${Build.DEVICE}
            Product: ${Build.PRODUCT}
            Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        """.trimIndent()
    }
}
