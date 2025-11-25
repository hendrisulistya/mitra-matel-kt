package app.mitra.matel

import android.Manifest
import android.content.pm.PackageManager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Color
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import app.mitra.matel.navigation.app

class MainActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }
    
    // Static reference to current gRPC service for lifecycle management
    companion object {
        var currentGrpcService: app.mitra.matel.network.GrpcService? = null
    }

    private fun clearSharedImagesCache() {
        cacheDir.resolve("images").takeIf { it.exists() }?.listFiles()?.forEach { runCatching { it.delete() } }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        
        // Request permissions on startup
        requestPermissions()
        clearSharedImagesCache()

        setContent {
            // Main App Content
            app()
        }
    }
    
    override fun onResume() {
        super.onResume()
    }
    
    override fun onPause() {
        super.onPause()
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        val toRequest = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }
}

@Preview
@Composable
fun appAndroidPreview() {
    app()
}