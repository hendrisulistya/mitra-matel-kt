package app.mitra.matel.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import app.mitra.matel.ui.components.SearchResultList
import app.mitra.matel.utils.MicSearchUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicSearchContent(
    onBack: () -> Unit,
    onSearchResult: (String) -> Unit = {},
    onVehicleClick: (String) -> Unit = {},
    searchViewModel: app.mitra.matel.viewmodel.SearchViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // State variables
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Function to process voice search result and trigger gRPC search
    fun processVoiceSearchResult(spokenText: String) {
        if (spokenText.isBlank()) {
            errorMessage = "Tidak ada teks yang dikenali"
            return
        }
        
        // Clean and format the spoken text for nomor_polisi search
        val cleanedText = MicSearchUtils.cleanNomorPolisi(spokenText)
        
        if (cleanedText.isBlank()) {
            errorMessage = "Format nomor polisi tidak valid. Contoh: B1234ABC"
            return
        }
        
        // Update search text and trigger search via SearchViewModel
        searchViewModel?.let { viewModel ->
            // Set search type to nomor_polisi (nopol)
            viewModel.updateSearchType("nopol")
            // Update search text and trigger automatic search
            viewModel.updateSearchText(cleanedText, "voice")
            
            // Call the callback for any additional actions
            onSearchResult(cleanedText)
        } ?: run {
            // Fallback if no SearchViewModel provided
            onSearchResult(cleanedText)
        }
    }

    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            errorMessage = "Izin mikrofon diperlukan untuk pencarian suara"
        }
    }
    
    // Speech recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        when (result.resultCode) {
            android.app.Activity.RESULT_OK -> {
                val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) ?: emptyList()
                val confidenceScores = result.data?.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES)
                
                // Use MicSearchUtils to process speech results
                val bestResult = MicSearchUtils.processSpeechResults(results, confidenceScores)
                
                if (bestResult != null) {
                    val (bestMatch, bestCleanedText) = bestResult
                    recognizedText = bestMatch
                    errorMessage = null // Clear any previous errors
                    // Process the voice search result and trigger gRPC search
                    processVoiceSearchResult(bestMatch)
                } else {
                    // If no valid license plate found, show the best attempt
                    recognizedText = results.firstOrNull() ?: ""
                    errorMessage = "Format nomor polisi tidak valid. Contoh: B2, AB123, B1234ABC"
                }
            }
            android.app.Activity.RESULT_CANCELED -> {
                errorMessage = "Pengenalan suara dibatalkan"
            }
            else -> {
                // Handle speech recognition errors using MicSearchUtils
                errorMessage = MicSearchUtils.getSpeechRecognitionErrorMessage(result.resultCode)
            }
        }
    }
    
    // Function to start voice recognition
    fun startVoiceRecognition() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = "Pengenalan suara tidak tersedia di perangkat ini"
            return
        }
        
        val intent = MicSearchUtils.createSpeechRecognitionIntent()
        
        isListening = true
        errorMessage = null
        speechLauncher.launch(intent)
    }

    // Get search state for displaying results
    val searchUiState = searchViewModel?.uiState?.collectAsState()?.value
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Pencarian Suara") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Search results section (takes most of the space)
        SearchResultList(
            results = searchUiState?.results ?: emptyList(),
            error = searchUiState?.error,
            onVehicleClick = onVehicleClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        
        // Simple voice search button at the bottom
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show recognized text if available
                if (recognizedText.isNotEmpty()) {
                    Text(
                        text = "\"$recognizedText\"",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val cleanedText = MicSearchUtils.cleanNomorPolisi(recognizedText)
                    if (cleanedText != recognizedText && cleanedText.isNotBlank()) {
                        Text(
                            text = "→ $cleanedText",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Show error messages
                searchUiState?.error?.let { error ->
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Voice search button
                Button(
                    onClick = { 
                        if (hasPermission) {
                            startVoiceRecognition()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    enabled = !isListening,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isListening) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                            Text("Mendengarkan...")
                        }
                    } else {
                        Text(
                            if (hasPermission) "Rekam Suara" else "Izinkan Akses Mikrofon"
                        )
                    }
                }
            }
        }
    }
}



@Preview
@Composable
fun MicSearchContentPreview() {
    MaterialTheme {
        MicSearchContent(
            onBack = {}
        )
    }
}