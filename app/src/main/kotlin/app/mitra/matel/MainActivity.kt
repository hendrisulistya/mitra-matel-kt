package app.mitra.matel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import app.mitra.matel.navigation.App

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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Request permissions on startup
        requestPermissions()

        setContent {
            // Main App Content
            App()
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
            // Storage permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            
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
fun AppAndroidPreview() {
    App()
}