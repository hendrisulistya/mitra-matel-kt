package app.mitra.matel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import app.mitra.matel.network.ApiService
import app.mitra.matel.network.ApiConfig
import app.mitra.matel.network.VehicleDetail
import app.mitra.matel.network.models.ApiResponse
import app.mitra.matel.network.HttpClientFactory
import app.mitra.matel.utils.SessionManager
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.util.Log
import app.mitra.matel.network.GrpcService
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@Composable
fun VehicleDetailContent(
    vehicleId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val apiService = remember { ApiService(context = context) }
    val grpcService: GrpcService = remember { GrpcService(context) }
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var lastKnownLocation by remember { mutableStateOf<Location?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var requireLocationGate by remember { mutableStateOf(false) }
    
    // GPS permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                               permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    var vehicleDetail by remember { mutableStateOf<VehicleDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Cache token once and reuse - performance optimization
    val authToken = remember { sessionManager.getToken() }
    
    // Single LaunchedEffect for both auth setup and data fetching - eliminates redundant calls
    LaunchedEffect(vehicleId, authToken, hasLocationPermission) {
        // Only proceed if GPS permission is granted
        if (!hasLocationPermission) {
            isLoading = false
            return@LaunchedEffect
        }
        
        try {
            isLoading = true
            error = null
            
            // Quick auth check and setup
            if (authToken == null) {
                error = "Authentication required. Please login again."
                return@LaunchedEffect
            }
            
            // Fetch vehicle detail via gRPC (REST removed)
            val result = grpcService.getVehicleDetail(vehicleId)
            result.fold(
                onSuccess = { vehicleDetail = it },
                onFailure = { error = it.message ?: "Failed to load vehicle details" }
            )
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }
    
    // Try to retrieve location whenever permission state changes
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            lastKnownLocation = null
            locationError = null
            return@LaunchedEffect
        }
        fusedLocationClient
            .lastLocation
            .addOnSuccessListener { location: Location? ->
                lastKnownLocation = location
                locationError = null
            }
            .addOnFailureListener { ex ->
                lastKnownLocation = null
                locationError = ex.message
            }
    }
    
    // Only fetch details if we have auth, permission, and a location
    LaunchedEffect(vehicleId, authToken, hasLocationPermission, lastKnownLocation) {
        if (!hasLocationPermission || lastKnownLocation == null) {
            isLoading = false
            return@LaunchedEffect
        }
        // Fetch details: do NOT gate on location/permission
        try {
            isLoading = true
            error = null
            if (authToken == null) {
                error = "Authentication required. Please login again."
                return@LaunchedEffect
            }
            val result = grpcService.getVehicleDetail(vehicleId)
            result.fold(
                onSuccess = { vehicleDetail = it },
                onFailure = { error = it.message ?: "Failed to load vehicle details" }
            )
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Content
        when {
            // Gate to GPS request only when triggered after display
            requireLocationGate && !hasLocationPermission -> {
                LocationPermissionContent(
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onBack = onBack
                )
            }
            requireLocationGate && hasLocationPermission && lastKnownLocation == null -> {
                LocationAccessRequiredContent(
                    onOpenSettings = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    },
                    onRetry = {
                        fusedLocationClient
                            .lastLocation
                            .addOnSuccessListener { location: Location? ->
                                lastKnownLocation = location
                                locationError = null
                                if (location != null) {
                                    // Location available, dismiss gate
                                    requireLocationGate = false
                                }
                            }
                            .addOnFailureListener { ex ->
                                lastKnownLocation = null
                                locationError = ex.message
                            }
                    },
                    onBack = onBack
                )
            }
            isLoading -> {
                LoadingContent()
            }
            error != null -> {
                ErrorContent(
                    error = error!!,
                    onRetry = {
                        if (authToken == null) {
                            error = "Authentication required. Please login again."
                            return@ErrorContent
                        }
                        kotlinx.coroutines.MainScope().launch {
                            try {
                                isLoading = true
                                error = null
                                val result = grpcService.getVehicleDetail(vehicleId)
                                result.fold(
                                    onSuccess = { vehicleDetail = it },
                                    onFailure = { error = it.message ?: "Failed to load vehicle details" }
                                )
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )
            }
            vehicleDetail != null -> {
                VehicleDetailDisplay(
                    vehicleDetail = vehicleDetail!!,
                    context = context,
                    onBack = onBack,
                    onRequireLocationGate = { requireLocationGate = true }
                )
            }
        }
    }
}

/**
 * Sends user location to server as per policy requirement
 * This function is called when vehicle details are displayed
 */
private suspend fun sendUserLocation(
    fusedLocationClient: FusedLocationProviderClient,
    apiService: ApiService,
    context: android.content.Context,
    vehicleId: String
) {
    try {
        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return // Permission not granted, skip location sending
        }
        
        // Get last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val locationString = "${it.latitude},${it.longitude}"
                kotlinx.coroutines.MainScope().launch {
                    try {
                        apiService.sendVehicleAccess(vehicleId, locationString)
                    } catch (e: Exception) {
                        Log.w("VehicleDetail", "Failed to send vehicle access: ${e.message}")
                    }
                }
            }
        }.addOnFailureListener { exception: Exception ->
            Log.w("VehicleDetail", "Failed to get location: ${exception.message}")
        }
    } catch (e: Exception) {
        // Silent failure - background operation
        Log.w("VehicleDetail", "Location service error: ${e.message}")
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Optimized CircularProgressIndicator with reduced animation overhead
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp), // Smaller size = less rendering overhead
                strokeWidth = 3.dp // Thinner stroke = less drawing overhead
            )
            Text(
                text = "Loading vehicle details...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Failed to load vehicle details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun VehicleDetailDisplay(
    vehicleDetail: VehicleDetail,
    context: android.content.Context,
    onBack: () -> Unit,
    onRequireLocationGate: () -> Unit
) {
    val apiService = remember { ApiService(context = context) }
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()
    var showShareDialog by remember { mutableStateOf(false) }
    
    // After display, attempt to send access; if missing GPS or location, trigger gate
    LaunchedEffect(vehicleDetail) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted || !coarseGranted) {
            onRequireLocationGate()
            return@LaunchedEffect
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location == null) {
                    onRequireLocationGate()
                    return@addOnSuccessListener
                }
                val locationString = "${location.latitude},${location.longitude}"
                kotlinx.coroutines.MainScope().launch {
                    try {
                        apiService.sendVehicleAccess(vehicleDetail.id, locationString)
                    } catch (e: Exception) {
                        Log.w("VehicleDetail", "Failed to send vehicle access: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { ex ->
                onRequireLocationGate()
                Log.w("VehicleDetail", "Failed to get location: ${ex.message}")
            }
        
        val sessionManager = SessionManager(context)
        sessionManager.addVehicleToHistory(vehicleDetail.id, vehicleDetail.nomor_polisi)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 16.dp), // Extra bottom padding for safe area
    ) {
        // Content that can expand
        Column(
            modifier = Modifier.weight(1f), // Take available space but allow buttons to show
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Single list - one data per line, no cards - ordered as requested
            MinimalistDetailRow(
                label = "Nomor Polisi",
                value = vehicleDetail.nomor_polisi.ifEmpty { "-" }
            )
            
            MinimalistDetailRow(
                label = "Nomor Rangka",
                value = vehicleDetail.nomor_rangka.ifEmpty { "-" }
            )
            
            MinimalistDetailRow(
                label = "Nomor Mesin",
                value = vehicleDetail.nomor_mesin.ifEmpty { "-" }
            )
            
            MinimalistDetailRow(
                label = "Tipe Kendaraan",
                value = vehicleDetail.tipe_kendaraan.ifEmpty { "-" }
            )
            
            MinimalistDetailRow(
                label = "Finance",
                value = vehicleDetail.finance_name.ifEmpty { "-" }
            )
            
            MinimalistDetailRow(
                label = "Cabang",
                value = vehicleDetail.cabang.ifEmpty { "-" }
            )
            
            MinimalistDetailRow(
                label = "Nomor Kontrak",
                value = vehicleDetail.nomor_kontrak.ifEmpty { "-" }
            )
            
            MinimalistDetailRow(
                label = "Warna Kendaraan",
                value = vehicleDetail.warna_kendaraan.ifEmpty { "-" }
            )
            
            // Combined row for Tahun Kendaraan and Past Due to save space
            DualColumnDetailRow(
                leftLabel = "Tahun Kendaraan",
                leftValue = vehicleDetail.tahun_kendaraan.ifEmpty { "-" },
                rightLabel = "Past Due",
                rightValue = vehicleDetail.past_due.ifEmpty { "-" }
            )
        }
        
        // Action buttons at the bottom - always visible
        Spacer(modifier = Modifier.height(16.dp)) // Reduced spacing
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { showShareDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Bagikan")
            }
            
            OutlinedButton(
                onClick = onBack, // Navigate back to dashboard
                modifier = Modifier.weight(1f)
            ) {
                Text("Tutup")
            }
        }
    }

    // Share dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Pilih Format Berbagi") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Bagaimana Anda ingin membagikan data kendaraan ini?")

                    Button(
                        onClick = {
                            showShareDialog = false
                            shareAsImage(context, vehicleDetail)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bagikan sebagai Gambar")
                    }

                    OutlinedButton(
                        onClick = {
                            showShareDialog = false
                            shareToWhatsApp(context, vehicleDetail)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bagikan sebagai Teks")
                    }

                    TextButton(
                        onClick = { showShareDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Batal")
                    }
                }
            },
            confirmButton = { },
            dismissButton = { }
        )
    }
}



@Composable
private fun MinimalistDetailRow(
    label: String,
    value: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = textColor.copy(alpha = 0.3f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun DualColumnDetailRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Labels row with dividers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left label with dash line
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = leftLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = textColor.copy(alpha = 0.3f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Right label with dash line
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rightLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = textColor.copy(alpha = 0.3f)
                )
            }
        }
        
        // Values row with bigger, bold text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = leftValue,
                style = MaterialTheme.typography.headlineMedium, // Bigger font
                fontWeight = FontWeight.Bold, // Bold as requested
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = rightValue,
                style = MaterialTheme.typography.headlineMedium, // Bigger font
                fontWeight = FontWeight.Bold, // Bold as requested
                color = textColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LocationPermissionContent(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Required",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Location permission required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Grant location access to proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationAccessRequiredContent(
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Required",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Location required to send access data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Enable device location services and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) { Text("Back") }
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) { Text("Try Again") }
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    ) { Text("Enable Location") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VehicleDetailContentPreview() {
    MaterialTheme {
        VehicleDetailDisplay(
            vehicleDetail = VehicleDetail(
                id = "870a3747-9223-46ab-bac0-81a6a4a496b8",
                nomor_kontrak = "CTR-2024-001",
                nama_konsumen = "John Doe",
                past_due = "30 days",
                nomor_polisi = "B4257FBF",
                nomor_rangka = "MH8BG41FAFJ122082",
                nomor_mesin = "G428ID121181",
                tipe_kendaraan = "Sedan",
                finance_name = "FIF",
                cabang = "BEKASI",
                tahun_kendaraan = "2022",
                warna_kendaraan = "Silver"
            ),
            context = LocalContext.current,
            onBack = { /* Preview - no action */ },
            onRequireLocationGate = { /* no-op in preview */ }
        )
    }
}

/**
 * Generate and share vehicle details as image
 */
private fun shareAsImage(context: android.content.Context, vehicleDetail: VehicleDetail) {
    try {
        // Create bitmap with vehicle details - compact layout
        val width = 1080
        val height = 1650  // Adjusted for disclaimer
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(android.graphics.Color.WHITE)

        // Draw gray header background
        val headerBackgroundPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 120f, headerBackgroundPaint)

        // Paint for text
        val headerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1976D2")
            textSize = 56f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 32f
            isAntiAlias = true
        }

        val valuePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 42f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }

        val disclaimerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#666666")
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        var y = 80f
        val leftMargin = 50f
        val rightMargin = width - 50f

        // Header - MITRA APP
        canvas.drawText("MITRA APP", width / 2f, y, headerPaint)
        y += 90f

        // Draw each field - very compact spacing
        fun drawField(label: String, value: String) {
            canvas.drawText(label, leftMargin, y, labelPaint)
            y += 42f

            // Handle long text by wrapping
            val maxWidth = rightMargin - leftMargin
            val displayValue = value.ifEmpty { "-" }

            if (valuePaint.measureText(displayValue) > maxWidth) {
                // Split long text into multiple lines
                val words = displayValue.split(" ")
                var line = ""
                words.forEach { word ->
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    if (valuePaint.measureText(testLine) > maxWidth) {
                        canvas.drawText(line, leftMargin, y, valuePaint)
                        y += 46f
                        line = word
                    } else {
                        line = testLine
                    }
                }
                if (line.isNotEmpty()) {
                    canvas.drawText(line, leftMargin, y, valuePaint)
                    y += 46f
                }
            } else {
                canvas.drawText(displayValue, leftMargin, y, valuePaint)
                y += 46f
            }

            y += 32f // Reduced space between fields
        }

        // Draw all fields in order
        drawField("Nomor Polisi", vehicleDetail.nomor_polisi)
        drawField("Nomor Rangka", vehicleDetail.nomor_rangka)
        drawField("Nomor Mesin", vehicleDetail.nomor_mesin)
        drawField("Nomor Kontrak", vehicleDetail.nomor_kontrak)
        drawField("Nama Konsumen", vehicleDetail.nama_konsumen)
        drawField("Tipe Kendaraan", vehicleDetail.tipe_kendaraan)
        drawField("Warna Kendaraan", vehicleDetail.warna_kendaraan)
        drawField("Tahun Kendaraan", vehicleDetail.tahun_kendaraan)
        drawField("Finance", vehicleDetail.finance_name)
        drawField("Cabang", vehicleDetail.cabang)
        drawField("Past Due", vehicleDetail.past_due)

        // Add disclaimer at bottom
        val disclaimerY = height - 100f
        val disclaimerText = "App ini bukan alat penarikan yang sah,"
        val disclaimerText2 = "konsultasikan ke finance terkait"
        canvas.drawText(disclaimerText, width / 2f, disclaimerY, disclaimerPaint)
        canvas.drawText(disclaimerText2, width / 2f, disclaimerY + 35f, disclaimerPaint)

        // Save bitmap to cache
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "vehicle_detail_${vehicleDetail.nomor_polisi}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        // Share via intent
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Filter WhatsApp apps
        val packageManager = context.packageManager
        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(shareIntent, android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_ALL.toLong()))
        } else {
            packageManager.queryIntentActivities(shareIntent, android.content.pm.PackageManager.MATCH_ALL)
        }

        val whatsappIntents = resolveInfos.filter { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            packageName.startsWith("com.whatsapp")
        }.map { resolveInfo ->
            Intent(shareIntent).apply {
                setPackage(resolveInfo.activityInfo.packageName)
            }
        }

        if (whatsappIntents.isEmpty()) {
            Toast.makeText(context, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
            return
        }

        if (whatsappIntents.size == 1) {
            context.startActivity(whatsappIntents[0])
        } else {
            val chooser = Intent.createChooser(whatsappIntents[0], "Bagikan via WhatsApp")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, whatsappIntents.drop(1).toTypedArray())
            context.startActivity(chooser)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membuat gambar: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Share vehicle details via WhatsApp
 * Shows only WhatsApp and WhatsApp Business in the chooser
 */
private fun shareToWhatsApp(context: android.content.Context, vehicleDetail: VehicleDetail) {
    val shareText = buildString {
        append("Detail Kendaraan:\n")
        append("Nomor Kontrak: ${vehicleDetail.nomor_kontrak}\n")
        append("Nama Konsumen: ${vehicleDetail.nama_konsumen}\n")
        append("Past Due: ${vehicleDetail.past_due}\n")
        append("Nomor Polisi: ${vehicleDetail.nomor_polisi}\n")
        append("Nomor Rangka: ${vehicleDetail.nomor_rangka}\n")
        append("Nomor Mesin: ${vehicleDetail.nomor_mesin}\n")
        append("Tipe: ${vehicleDetail.tipe_kendaraan}\n")
        append("Finance: ${vehicleDetail.finance_name}\n")
        append("Cabang: ${vehicleDetail.cabang}\n")
        append("Tahun: ${vehicleDetail.tahun_kendaraan}\n")
        append("Warna: ${vehicleDetail.warna_kendaraan}")
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        val packageManager = context.packageManager
        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(sendIntent, android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_ALL.toLong()))
        } else {
            packageManager.queryIntentActivities(sendIntent, android.content.pm.PackageManager.MATCH_ALL)
        }

        // Filter all WhatsApp variants (including clones)
        val whatsappIntents = resolveInfos.filter { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            packageName.startsWith("com.whatsapp")
        }.map { resolveInfo ->
            Intent(sendIntent).apply {
                setPackage(resolveInfo.activityInfo.packageName)
            }
        }

        if (whatsappIntents.isEmpty()) {
            Toast.makeText(context, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
            return
        }

        // If only one WhatsApp app, open it directly
        if (whatsappIntents.size == 1) {
            context.startActivity(whatsappIntents[0])
        } else {
            // Show chooser with only WhatsApp apps
            val chooser = Intent.createChooser(whatsappIntents[0], "Bagikan via WhatsApp")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, whatsappIntents.drop(1).toTypedArray())
            context.startActivity(chooser)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak dapat membuka WhatsApp: ${e.message}", Toast.LENGTH_LONG).show()
    }
}