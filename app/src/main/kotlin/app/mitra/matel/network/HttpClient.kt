package app.mitra.matel.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Ktor HTTP Client Configuration
 */
object HttpClientFactory {
    
    private var authToken: String? = null
    
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    fun getAuthToken(): String? = authToken
    
    fun create(): HttpClient {
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
}

/**
 * Global HTTP Client Instance
 */
val httpClient: HttpClient by lazy {
    HttpClientFactory.create()
}
