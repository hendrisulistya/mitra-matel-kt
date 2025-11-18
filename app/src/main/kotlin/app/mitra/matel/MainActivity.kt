package app.mitra.matel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
    ) { permissions ->
        // Handle permission results if needed
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val isGranted = entry.value
            // You can add specific handling for each permission here
        }
    }
    
    // Static reference to current gRPC service for lifecycle management
    companion object {
        var currentGrpcService: app.mitra.matel.network.GrpcService? = null
    }

    private fun clearSharedImagesCache() {
        val dir = File(cacheDir, "images")
        if (dir.exists()) {
            dir.listFiles()?.forEach { runCatching { it.delete() } }
        }
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
        // Proactively warm up gRPC connection when app resumes
        currentGrpcService?.let { grpcService ->
            android.util.Log.d("MainActivity", "App resumed - warming up gRPC connection")
            grpcService.warmUpConnection()
        }
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("MainActivity", "App paused")
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>().apply {
            // Location permissions
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            
            // Microphone permission for voice search
            add(Manifest.permission.RECORD_AUDIO)
        }
        
        // Filter out already granted permissions
        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

@Preview
@Composable
fun appAndroidPreview() {
    app()
}