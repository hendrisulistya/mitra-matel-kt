package app.mitra.matel

import app.mitra.matel.BuildConfig

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
     * 
     * This value is automatically set by the build system based on build type
     */
    val IS_PRODUCTION: Boolean
        get() = BuildConfig.IS_PRODUCTION
    
    /**
     * App Version Information
     */
    const val APP_NAME = "Mitra Matel"
    val VERSION_NAME: String
        get() = BuildConfig.VERSION_NAME
    val VERSION_BUILD: Int
        get() = BuildConfig.BUILD_NUMBER
    
    /**
     * Debug Features
     */
    val ENABLE_DEBUG_FEATURES: Boolean
        get() = !BuildConfig.IS_PRODUCTION
    val ENABLE_NETWORK_LOGGING: Boolean
        get() = !BuildConfig.IS_PRODUCTION
    
    /**
     * Get current environment name for display
     */
    fun getEnvironmentName(): String = if (IS_PRODUCTION) "Production" else "Development"

}