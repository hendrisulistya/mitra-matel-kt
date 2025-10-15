package app.mitra.matel.network

/**
 * API Configuration
 * Change isProduction to toggle between local and production environments
 */
object ApiConfig {
    // Toggle this to switch between environments
    private const val isProduction = false
    
    // API Base URLs
    // For Android Emulator - 10.0.2.2 maps to host machine's localhost
    private const val LOCAL_BASE_URL = "http://localhost:3000"

    // For Real Android Device - use your computer's IP address on local network
    // Find your IP: Mac/Linux: ifconfig | Windows: ipconfig
    // Example: private const val LOCAL_BASE_URL = "http://192.168.1.100:3000"

    private const val PRODUCTION_BASE_URL = "https://api.mitra-matel.com"

    // gRPC Configuration
    private const val LOCAL_GRPC_HOST = "localhost"
    private const val LOCAL_GRPC_PORT = 50051
    private const val PRODUCTION_GRPC_HOST = "grpc.mitra-matel.com"
    private const val PRODUCTION_GRPC_PORT = 443

    // Get current base URL based on environment
    val BASE_URL: String
        get() = if (isProduction) PRODUCTION_BASE_URL else LOCAL_BASE_URL

    // Get current gRPC configuration
    val GRPC_HOST: String
        get() = if (isProduction) PRODUCTION_GRPC_HOST else LOCAL_GRPC_HOST
    
    val GRPC_PORT: Int
        get() = if (isProduction) PRODUCTION_GRPC_PORT else LOCAL_GRPC_PORT
    
    val IS_PRODUCTION: Boolean
        get() = isProduction

    // API Endpoints
    object Endpoints {
        const val LOGIN = "/public/auth/user/login"
        const val FORCE_LOGIN = "/public/auth/user/force-login"
        const val REGISTER = "/public/auth/user/register"
        const val LOGOUT = "/public/auth/user/logout"
        const val PROFILE = "/user/profile"
        const val SEARCH_VEHICLE = "/vehicle/search"
        const val MY_VEHICLES = "/vehicle/my-vehicles"
        const val ADD_VEHICLE = "/vehicle/add"
        const val DETAIL_VEHICLE = "/private/vehicle/detail/:id"
        const val UPDATE_VEHICLE = "/private/vehicle/update"
        const val SEARCH_HISTORY = "/history/search"
        const val PAYMENT_PLANS = "/payment/plans"
        const val PAYMENT_HISTORY = "/payment/history"
    }
    
    // Debug info
    fun getCurrentEnvironment(): String {
        return if (isProduction) "Production" else "Local Development"
    }
}
