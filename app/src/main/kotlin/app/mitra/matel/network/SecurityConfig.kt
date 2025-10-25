package app.mitra.matel.network

import android.util.Log
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Security configuration for HTTP/gRPC connections
 */
object SecurityConfig {
    
    // Certificate pins for production domains (SHA-256 hashes)
    // Current certificate pin (Let's Encrypt issued)
    private val CURRENT_CERTIFICATE_PIN = "sha256/2uqZNWk18aYlBbtYaC2bCFUl439TPzyudzVy9nrY1+E="
    
    // Let's Encrypt root certificate pins (backup for automatic renewals)
    // These are the root CA pins that Let's Encrypt uses
    private val LETS_ENCRYPT_ROOT_PINS = listOf(
        // Let's Encrypt Authority X3 (Cross-signed by IdenTrust)
        "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=",
        // Let's Encrypt Authority X4 (Cross-signed by IdenTrust) 
        "sha256/sRHdihwgkaib1P1gxX8HFszlD+7/gTfNvuAybgLPNis=",
        // ISRG Root X1 (Let's Encrypt's own root)
        "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=",
        // ISRG Root X2 (Let's Encrypt's ECDSA root)
        "sha256/diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI="
    )
    
    private val CERTIFICATE_PINS = mapOf(
        "api.mitra-matel.com" to listOf(
            CURRENT_CERTIFICATE_PIN,
            *LETS_ENCRYPT_ROOT_PINS.toTypedArray()
        ),
        "grpc.mitra-matel.com" to listOf(
            CURRENT_CERTIFICATE_PIN,
            *LETS_ENCRYPT_ROOT_PINS.toTypedArray()
        ),
        "mitra-matel.com" to listOf(
            CURRENT_CERTIFICATE_PIN,
            *LETS_ENCRYPT_ROOT_PINS.toTypedArray()
        )
    )
    
    /**
     * Validates certificate pins for the given hostname
     */
    fun validateCertificatePin(hostname: String, certificates: Array<X509Certificate>): Boolean {
        val pins = CERTIFICATE_PINS[hostname] ?: return true // No pinning configured
        
        if (pins.isEmpty()) return true // No pins to validate
        
        for (cert in certificates) {
            val pin = "sha256/" + android.util.Base64.encodeToString(
                MessageDigest.getInstance("SHA-256").digest(cert.publicKey.encoded),
                android.util.Base64.NO_WRAP
            )
            
            if (pins.contains(pin)) {
                Log.d("SecurityConfig", "Certificate pin validated for $hostname")
                return true
            }
        }
        
        Log.w("SecurityConfig", "Certificate pin validation failed for $hostname")
        return false
    }
    
    /**
     * Check if domain requires secure connection
     */
    fun requiresSecureConnection(hostname: String): Boolean {
        return when {
            hostname.endsWith("mitra-matel.com") -> true
            hostname == "localhost" || hostname == "127.0.0.1" || hostname == "10.0.2.2" -> false
            else -> true // Default to secure for unknown domains
        }
    }
    
    /**
     * Debug utility: Extract certificate pin from X509Certificate
     * Use this in debug builds to get certificate pins
     */
    fun extractPinFromCertificate(certificate: X509Certificate): String {
        val publicKey = certificate.publicKey.encoded
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKey)
        return "sha256/" + android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }
    
    /**
     * Debug utility: Log all configured pins
     */
    fun logConfiguredPins() {
        Log.d("SecurityConfig", "=== Configured Certificate Pins ===")
        Log.d("SecurityConfig", "Current Certificate Pin: $CURRENT_CERTIFICATE_PIN")
        Log.d("SecurityConfig", "Let's Encrypt Backup Pins: ${LETS_ENCRYPT_ROOT_PINS.size} pins")
        LETS_ENCRYPT_ROOT_PINS.forEachIndexed { index, pin ->
            Log.d("SecurityConfig", "  Backup Pin ${index + 1}: $pin")
        }
        Log.d("SecurityConfig", "--- Domain Configuration ---")
        CERTIFICATE_PINS.forEach { (domain, pins) ->
            Log.d("SecurityConfig", "Domain: $domain (${pins.size} pins total)")
            pins.forEachIndexed { index, pin ->
                val type = when {
                    pin == CURRENT_CERTIFICATE_PIN -> "CURRENT"
                    pin in LETS_ENCRYPT_ROOT_PINS -> "BACKUP"
                    else -> "UNKNOWN"
                }
                Log.d("SecurityConfig", "  [$type] Pin ${index + 1}: $pin")
            }
        }
    }
    
    /**
     * Get pins for a specific domain
     */
    fun getPinsForDomain(hostname: String): List<String> {
        return CERTIFICATE_PINS[hostname] ?: emptyList()
    }
    
    /**
     * Validate if a pin matches the current certificate
     */
    fun isValidPin(pin: String): Boolean {
        return pin == CURRENT_CERTIFICATE_PIN
    }
}