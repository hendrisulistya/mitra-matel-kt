package app.mitra.matel

/**
 * Centralized Application Configuration
 * 
 * This file contains all global app settings that can be easily modified
 * for different environments (development, staging, production).
 */
object AppConfig {
    
    /**
     * Production Environment Flag
     * 
     * Set to:
     * - false: Development mode (localhost APIs, debug features enabled)
     * - true:  Production mode (production APIs, optimized for release)
     */
    const val IS_PRODUCTION = true
    
    /**
     * App Version Information
     */
    const val APP_NAME = "Mitra Matel"
    
    /**
     * Debug Features
     */
    const val ENABLE_DEBUG_FEATURES = !IS_PRODUCTION
    const val ENABLE_NETWORK_LOGGING = !IS_PRODUCTION
    
    /**
     * Get current environment name for display
     */
    fun getEnvironmentName(): String {
        return if (IS_PRODUCTION) "Production" else "Development"
    }
    fun getAppVersion(): String {
        return "0.9.114 (Build 1212)"+if (IS_PRODUCTION) "-prod" else "-debug"
    }
}