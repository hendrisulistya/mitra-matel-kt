package app.mitra.matel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun InputVehicleContent() {
    var plateNumber by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var engineNumber by remember { mutableStateOf("") }
    var chassisNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Input Data Kendaraan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Masukkan informasi kendaraan baru", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it },
                    label = { Text("Nomor Polisi *") },
                    placeholder = { Text("B 1234 ABC") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Merek *") },
                    placeholder = { Text("Toyota") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model/Tipe *") },
                    placeholder = { Text("Avanza") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Tahun Pembuatan *") },
                    placeholder = { Text("2023") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Warna *") },
                    placeholder = { Text("Hitam") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = engineNumber,
                    onValueChange = { engineNumber = it },
                    label = { Text("Nomor Mesin") },
                    placeholder = { Text("1NZ1234567") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = chassisNumber,
                    onValueChange = { chassisNumber = it },
                    label = { Text("Nomor Rangka") },
                    placeholder = { Text("MHKA12345678901234") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "* Wajib diisi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { /* Clear form */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Bersihkan")
            }
            Button(
                onClick = { /* Submit */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Simpan Data")
            }
        }
    }
}

@Preview
@Composable
fun InputVehicleContentPreview() {
    MaterialTheme {
        InputVehicleContent()
    }
}
