package app.mitra.matel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.mitra.matel.network.ApiService
import app.mitra.matel.network.ApiConfig
import app.mitra.matel.network.VehicleDetail
import app.mitra.matel.network.models.ApiResponse
import app.mitra.matel.network.HttpClientFactory
import app.mitra.matel.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun VehicleDetailContent(
    vehicleId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val apiService = remember { ApiService(context = context) }
    
    var vehicleDetail by remember { mutableStateOf<VehicleDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Cache token once and reuse - performance optimization
    val authToken = remember { sessionManager.getToken() }
    
    // Single LaunchedEffect for both auth setup and data fetching - eliminates redundant calls
    LaunchedEffect(vehicleId, authToken) {
        try {
            isLoading = true
            error = null
            
            // Quick auth check and setup
            if (authToken == null) {
                error = "Authentication required. Please login again."
                return@LaunchedEffect
            }
            
            // Set auth token once
            HttpClientFactory.setAuthToken(authToken)
            
            // Fetch vehicle detail immediately after auth setup
            val result = apiService.getVehicleDetail(vehicleId)
            result.fold(
                onSuccess = { vehicleDetail = it },
                onFailure = { error = it.message }
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
            isLoading -> {
                LoadingContent()
            }
            error != null -> {
                ErrorContent(
                    error = error!!,
                    onRetry = {
                        // Simplified retry logic - reuse cached token and avoid redundant operations
                        if (authToken == null) {
                            error = "Authentication required. Please login again."
                            return@ErrorContent
                        }
                        
                        // Direct API call without additional coroutine scope
                        kotlinx.coroutines.MainScope().launch {
                            try {
                                isLoading = true
                                error = null
                                
                                val result = apiService.getVehicleDetail(vehicleId)
                                result.fold(
                                    onSuccess = { vehicleDetail = it },
                                    onFailure = { error = it.message }
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
                    onBack = onBack
                )
            }
        }
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
    onBack: () -> Unit
) {
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
            onBack = { /* Preview - no action */ }
        )
    }
}