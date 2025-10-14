package app.mitra.matel.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import app.mitra.matel.network.models.DeviceInfo
import java.util.*

object DeviceUtils {
    
    /**
     * Get unique device ID
     * Uses Android ID which is unique per app installation
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: generateFallbackDeviceId()
        } catch (e: Exception) {
            generateFallbackDeviceId()
        }
    }
    
    /**
     * Generate fallback device ID if Android ID is not available
     */
    private fun generateFallbackDeviceId(): String {
        return UUID.randomUUID().toString()
    }
    
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
     * Get complete device information
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            deviceId = getDeviceId(context),
            model = getDeviceModel()
        )
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
