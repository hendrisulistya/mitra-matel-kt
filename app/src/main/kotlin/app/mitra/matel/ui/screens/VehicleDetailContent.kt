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
                onClick = { 
                    // Share vehicle data to WhatsApp
                    val shareText = "*=================*\n" +
                    "*MITRA MATEL APP*\n" +
                    "*=================*\n" +
                            "Nomor Polisi: ${vehicleDetail.nomor_polisi.ifEmpty { "-" }}\n" +
                            "Nomor Mesin: ${vehicleDetail.nomor_mesin.ifEmpty { "-" }}\n" +
                            "Nomor Rangka: ${vehicleDetail.nomor_rangka.ifEmpty { "-" }}\n" +
                            "Tipe Kendaraan: ${vehicleDetail.tipe_kendaraan.ifEmpty { "-" }}\n" +
                            "Finance: ${vehicleDetail.finance_name.ifEmpty { "-" }}\n" +
                            "Cabang: ${vehicleDetail.cabang.ifEmpty { "-" }}\n" +
                            "*PERHATIAN*: _Aplikasi Mitra Matel bukanlah alat sah untuk penarikan. Selalu ikuti SOP yang berlaku dan konfirmasi ke kantor finance terkait._"
                    
                    val whatsappIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        setPackage("com.whatsapp") // Try WhatsApp first
                    }
                    
                    val whatsappBusinessIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        setPackage("com.whatsapp.w4b") // WhatsApp Business
                    }
                    
                    try {
                        // Try WhatsApp first
                        context.startActivity(whatsappIntent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        try {
                            // Try WhatsApp Business
                            context.startActivity(whatsappBusinessIntent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            // Fallback to general share
                            val generalShareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(generalShareIntent, "Bagikan Data Kendaraan"))
                        }
                    }
                },
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
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // GPS Icon
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "GPS Permission",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Title
                Text(
                    text = "Izin Lokasi Diperlukan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                // Description
                Text(
                    text = "Aplikasi memerlukan akses lokasi GPS untuk menampilkan detail kendaraan. Fitur ini diperlukan untuk keamanan dan verifikasi lokasi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
                
                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Berikan Izin Lokasi")
                    }
                    
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kembali")
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