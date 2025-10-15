package app.mitra.matel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import app.mitra.matel.R
import app.mitra.matel.network.VehicleResult

@Composable
fun SearchResultList(
    results: List<VehicleResult> = emptyList(),
    error: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background SVG image - single background
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.raw.ic_background)
                    .decoderFactory(SvgDecoder.Factory())
                    .crossfade(true)
                    .build(),
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
                alpha = 0.15f
            )
            
            // Content overlay - reduced opacity
            when {
                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Data kendaraan tidak ditemukan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results.size) { index ->
                            VehicleResultCard(vehicle = results[index])
                            
                            // Add separator line after each item except the last one
                            if (index < results.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    thickness = 1.dp,
                                    color = Color.Black.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleResultCard(
    vehicle: VehicleResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f) // Reduced opacity to show background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: nomor_polisi (left) | finance_name (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vehicle.nomorPolisi,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = vehicle.financeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
            
            // Row 2: tipe_kendaraan (left) | dataVersion (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vehicle.tipeKendaraan,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Text(
                    text = vehicle.dataVersion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    } // Added missing closing brace for Card
} // Existing closing brace for function

@Preview
@Composable
fun SearchResultListPreview() {
    MaterialTheme {
        SearchResultList(
            results = listOf(
                VehicleResult(
                    id = "1",
                    nomorPolisi = "B 1234 ABC",
                    tipeKendaraan = "Sedan",
                    dataVersion = "v1.0",
                    financeName = "Bank ABC"
                ),
                VehicleResult(
                    id = "2",
                    nomorPolisi = "D 5678 XYZ",
                    tipeKendaraan = "SUV",
                    dataVersion = "v1.1",
                    financeName = "Finance XYZ"
                )
            ),
            modifier = Modifier.height(300.dp)
        )
    }
}
