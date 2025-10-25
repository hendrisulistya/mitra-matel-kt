package app.mitra.matel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.mitra.matel.R
import androidx.lifecycle.viewmodel.compose.viewModel
import app.mitra.matel.network.models.MyVehicleDataItem
import app.mitra.matel.viewmodel.VehicleViewModel
import app.mitra.matel.viewmodel.VehicleDataState

@Composable
fun MyVehicleDataContent(
    onNavigateToAddVehicle: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: VehicleViewModel = viewModel { VehicleViewModel(context) }
    
    val vehicleDataState by viewModel.vehicleDataState.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()

    // Fetch data when the composable is first created
    LaunchedEffect(Unit) {
        viewModel.fetchVehicleData()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kelola data kendaraan yang telah terdaftar", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total: ${vehicles.size} kendaraan", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = { viewModel.fetchVehicleData() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        }

        when (vehicleDataState) {
            is VehicleDataState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is VehicleDataState.Error -> {
                val errorState = vehicleDataState as VehicleDataState.Error
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = errorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.fetchVehicleData() }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is VehicleDataState.AuthenticationError -> {
                val authErrorState = vehicleDataState as VehicleDataState.AuthenticationError
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Authentication Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = authErrorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.fetchVehicleData() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry")
                            }
                            OutlinedButton(
                                onClick = { /* Navigate to login - this should be handled by parent */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Login")
                            }
                        }
                    }
                }
            }
            is VehicleDataState.ServerError -> {
                val serverErrorState = vehicleDataState as VehicleDataState.ServerError
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Server Issue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = serverErrorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.fetchVehicleData() }
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
            is VehicleDataState.Success -> {
                if (vehicles.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Belum ada kendaraan terdaftar",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tambahkan kendaraan pertama Anda untuk mulai menggunakan layanan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onNavigateToAddVehicle,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tambah Kendaraan Baru")
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(vehicles) { vehicle ->
                            VehicleCard(vehicle)
                        }
                    }
                }
            }
            is VehicleDataState.Idle -> {
                // Initial state, show nothing or a placeholder
            }
        }
    }
}

@Composable
fun VehicleCard(vehicle: MyVehicleDataItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_car_vector),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = vehicle.nomorPolisi.takeIf { it.isNotBlank() } ?: "-",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = vehicle.nomorRangka.takeIf { it.isNotBlank() } ?: "-",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = if (vehicle.isPublic) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (vehicle.isPublic) "Public" else "Private",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (vehicle.isPublic) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            IconButton(onClick = { /* Edit */ }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}

@Preview
@Composable
fun MyVehicleDataContentPreview() {
    MaterialTheme {
        MyVehicleDataContent(
            onNavigateToAddVehicle = {}
        )
    }
}

@Preview
@Composable
fun VehicleCardPreview() {
    MaterialTheme {
        VehicleCard(
            vehicle = MyVehicleDataItem(
                id = "90a9e6ca-ca41-48a2-8999-0c51111a2322",
                nomorPolisi = "B111GG",
                nomorRangka = "MHKA38RURIE8EUE9E",
                isPublic = false
            )
        )
    }
}
