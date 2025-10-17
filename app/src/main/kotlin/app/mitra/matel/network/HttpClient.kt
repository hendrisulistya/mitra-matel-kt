package app.mitra.matel.network

import android.content.Context
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import app.mitra.matel.utils.SessionManager
import app.mitra.matel.network.models.LoginRequest
import app.mitra.matel.network.models.LoginResponse
import app.mitra.matel.utils.DeviceUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ktor HTTP Client Configuration with Automatic Token Refresh
 */
object HttpClientFactory {
    
    private var authToken: String? = null
    private var sessionManager: SessionManager? = null
    private val refreshMutex = Mutex()
    private var isRefreshing = false
    
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    fun getAuthToken(): String? = authToken
    
    fun setSessionManager(manager: SessionManager) {
        sessionManager = manager
    }
    
    fun create(context: Context? = null): HttpClient {
        return HttpClient(Android) {
            // JSON Configuration
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            
            // Logging
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("HTTP Client", message)
                    }
                }
                level = LogLevel.ALL
            }
            
            // Timeout Configuration - Optimized for faster response
            install(HttpTimeout) {
                requestTimeoutMillis = 15000  // Reduced from 30s to 15s
                connectTimeoutMillis = 10000  // Reduced from 30s to 10s
                socketTimeoutMillis = 15000   // Reduced from 30s to 15s
            }
            
            // Automatic Token Refresh Interceptor
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    when {
                        exception is ClientRequestException && exception.response.status == HttpStatusCode.Unauthorized -> {
                            Log.d("HTTP Client", "401 Unauthorized - attempting token refresh")
                            
                            // Try to refresh token using saved credentials
                            val refreshed = refreshTokenIfPossible(context)
                            
                            if (refreshed) {
                                Log.d("HTTP Client", "Token refreshed successfully")
                                // The client will automatically retry with the new token
                                // due to the updated authToken in defaultRequest
                            } else {
                                Log.w("HTTP Client", "Token refresh failed")
                                // Clear session
                                sessionManager?.clearSession()
                                throw exception
                            }
                        }
                        exception is ClientRequestException && exception.response.status == HttpStatusCode.Forbidden -> {
                            Log.w("HTTP Client", "403 Forbidden - Device conflict detected")
                            
                            // Clear session immediately for device conflicts
                            sessionManager?.clearSession()
                            
                            // Don't attempt token refresh for device conflicts
                            // Let the exception propagate to trigger navigation to login
                            throw exception
                        }
                        else -> {
                            throw exception
                        }
                    }
                }
            }
            
            // Default Request Configuration
            defaultRequest {
                url(ApiConfig.BASE_URL)
                contentType(ContentType.Application.Json)
                
                // Add auth token if available
                authToken?.let {
                    header("Authorization", "Bearer $it")
                }
            }
        }
    }
    
    /**
     * Attempt to refresh token using saved credentials
     * Returns true if successful, false otherwise
     */
    private suspend fun refreshTokenIfPossible(context: Context?): Boolean {
        return refreshMutex.withLock {
            if (isRefreshing) {
                Log.d("HTTP Client", "Token refresh already in progress")
                return@withLock false
            }
            
            isRefreshing = true
            
            try {
                val manager = sessionManager ?: context?.let { SessionManager(it) }
                if (manager == null) {
                    Log.w("HTTP Client", "No SessionManager available for token refresh")
                    return@withLock false
                }
                
                val email = manager.getEmail()
                val password = manager.getPassword()
                
                if (email.isNullOrBlank() || password.isNullOrBlank()) {
                    Log.w("HTTP Client", "No saved credentials available for token refresh")
                    return@withLock false
                }
                
                Log.d("HTTP Client", "Attempting token refresh with saved credentials")
                
                // Create a simple HTTP client for token refresh (to avoid recursion)
                val refreshClient = HttpClient(Android) {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        })
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = 10000
                        connectTimeoutMillis = 5000
                        socketTimeoutMillis = 10000
                    }
                }
                
                try {
                    val deviceInfo = context?.let { DeviceUtils.getDeviceInfo(it) }
                    val loginRequest = LoginRequest(email, password, deviceInfo)
                    val response = refreshClient.post("${ApiConfig.BASE_URL}${ApiConfig.Endpoints.LOGIN}") {
                        contentType(ContentType.Application.Json)
                        setBody(loginRequest)
                    }
                    
                    if (response.status.isSuccess()) {
                        val loginResponse: LoginResponse = response.body()
                        val newToken = loginResponse.token
                        
                        // Update tokens
                        setAuthToken(newToken)
                        manager.saveToken(newToken)
                        
                        Log.d("HTTP Client", "Token refresh successful")
                        return@withLock true
                    } else {
                        Log.w("HTTP Client", "Token refresh failed: ${response.status}")
                        return@withLock false
                    }
                } finally {
                    refreshClient.close()
                }
                
            } catch (e: Exception) {
                Log.e("HTTP Client", "Token refresh error: ${e.message}")
                return@withLock false
            } finally {
                isRefreshing = false
            }
        }
    }
}

/**
 * Global HTTP Client Instance
 */
fun createHttpClient(context: Context): HttpClient {
    HttpClientFactory.setSessionManager(SessionManager(context))
    return HttpClientFactory.create(context)
}

val httpClient: HttpClient by lazy {
    HttpClientFactory.create()
}
