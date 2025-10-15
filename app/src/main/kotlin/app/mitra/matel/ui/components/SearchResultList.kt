package app.mitra.matel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    onVehicleClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background SVG image
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
        
        // Content overlay
        when {
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            results.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Data kendaraan tidak ditemukan",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results.size) { index ->
                        VehicleResultCard(
                            vehicle = results[index],
                            onClick = { onVehicleClick(results[index].id) }
                        )
                        
                        // Add separator line after each item except the last one
                        if (index < results.size - 1) {
                            HorizontalDivider(
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

@Composable
private fun VehicleResultCard(
    vehicle: VehicleResult,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: nomor_polisi (60%) | finance_name (40%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = vehicle.nomorPolisi,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
            Text(
                text = vehicle.financeName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                modifier = Modifier.weight(0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
        
        // Row 2: tipe_kendaraan (60%) | dataVersion (40%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = vehicle.tipeKendaraan,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                modifier = Modifier.weight(0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            )
            Text(
                text = vehicle.dataVersion,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier.weight(0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}

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
            onVehicleClick = { vehicleId -> 
                // Preview click handler - in real app this would navigate to detail screen
                println("Clicked vehicle with ID: $vehicleId")
            },
            modifier = Modifier.height(300.dp)
        )
    }
}
