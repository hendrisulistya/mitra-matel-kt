package app.mitra.matel.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.mitra.matel.network.ApiService
import app.mitra.matel.network.models.AddVehicleRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputVehicleContent(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val apiService = remember { ApiService(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var namaKonsumen by remember { mutableStateOf("") }
    var financeName by remember { mutableStateOf("") }
    var nomorPolisi by remember { mutableStateOf("") }
    var nomorRangka by remember { mutableStateOf("") }
    var nomorMesin by remember { mutableStateOf("") }
    var tipeKendaraan by remember { mutableStateOf("") }
    var warnaKendaraan by remember { mutableStateOf("") }
    var tahunKendaraan by remember { mutableStateOf("") }
    var isShared by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    
    // Validation function
    fun isFormValid(): Boolean {
        val hasValidNomorPolisi = nomorPolisi.isNotBlank() && nomorPolisi.length >= 3
        val hasValidNomorRangka = nomorRangka.isNotBlank() && nomorRangka.length >= 6 && nomorRangka.length <= 20
        val hasValidNomorMesin = nomorMesin.isNotBlank() && nomorMesin.length >= 4
        
        return hasValidNomorPolisi || hasValidNomorRangka || hasValidNomorMesin
    }
    
    // Clear form function
    fun clearForm() {
        namaKonsumen = ""
        financeName = ""
        nomorPolisi = ""
        nomorRangka = ""
        nomorMesin = ""
        tipeKendaraan = ""
        warnaKendaraan = ""
        tahunKendaraan = ""
        isShared = false
    }
    
    // Submit function
    fun submitVehicle() {
        if (!isFormValid()) {
            dialogMessage = "Mohon isi minimal satu dari: Nomor Polisi (min 3 karakter), Nomor Rangka (6-20 karakter), atau Nomor Mesin (min 4 karakter) dengan format yang benar"
            showErrorDialog = true
            return
        }
        
        isLoading = true
        
        coroutineScope.launch {
            try {
                val request = AddVehicleRequest(
                    isShared = isShared,
                    namaKonsumen = namaKonsumen.takeIf { it.isNotBlank() } ?: "",
                    financeName = financeName.takeIf { it.isNotBlank() } ?: "",
                    nomorPolisi = nomorPolisi.takeIf { it.isNotBlank() },
                    nomorRangka = nomorRangka.takeIf { it.isNotBlank() },
                    nomorMesin = nomorMesin.takeIf { it.isNotBlank() },
                    tipeKendaraan = tipeKendaraan.takeIf { it.isNotBlank() } ?: "",
                    warnaKendaraan = warnaKendaraan.takeIf { it.isNotBlank() } ?: "",
                    tahunKendaraan = tahunKendaraan.takeIf { it.isNotBlank() } ?: ""
                )
                
                val result = apiService.addVehicle(request)
                result.fold(
                    onSuccess = { response ->
                        dialogMessage = response.message
                        showSuccessDialog = true
                        clearForm()
                    },
                    onFailure = { exception ->
                        dialogMessage = exception.message ?: "Terjadi kesalahan saat menyimpan data"
                        showErrorDialog = true
                    }
                )
            } catch (e: Exception) {
                dialogMessage = e.message ?: "Terjadi kesalahan saat menyimpan data"
                showErrorDialog = true
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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
                    value = namaKonsumen,
                    onValueChange = { namaKonsumen = it.uppercase() },
                    label = { Text("Nama Konsumen") },
                    placeholder = { Text("JOHN DOE") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = financeName,
                    onValueChange = { financeName = it.uppercase() },
                    label = { Text("Nama Finance") },
                    placeholder = { Text("BCA FINANCE") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                var isSharedMenuExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = isSharedMenuExpanded,
                    onExpandedChange = { if (!isLoading) isSharedMenuExpanded = !isSharedMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = if (isShared) "Publik" else "Private",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Jenis Sharing") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSharedMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = isSharedMenuExpanded,
                        onDismissRequest = { isSharedMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Publik") },
                            onClick = {
                                isShared = true
                                isSharedMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Private") },
                            onClick = {
                                isShared = false
                                isSharedMenuExpanded = false
                            }
                        )
                    }
                }

                Text(
                    text = "Minimal satu dari field berikut harus diisi:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = nomorPolisi,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isLetterOrDigit() }.uppercase()
                        if (filtered.length <= 10) {
                            nomorPolisi = filtered
                        }
                    },
                    label = { Text("Nomor Polisi") },
                    placeholder = { Text("B1234XYZ") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    isError = nomorPolisi.isNotEmpty() && nomorPolisi.length < 3,
                    supportingText = {
                        if (nomorPolisi.isNotEmpty() && nomorPolisi.length < 3) {
                            Text(
                                text = "Minimal 3 karakter",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("3-10 karakter, huruf dan angka saja")
                        }
                    }
                )

                OutlinedTextField(
                    value = nomorRangka,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isLetterOrDigit() }.uppercase()
                        if (filtered.length <= 20) {
                            nomorRangka = filtered
                        }
                    },
                    label = { Text("Nomor Rangka") },
                    placeholder = { Text("MHKA1BA1A0A123456") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    isError = nomorRangka.isNotEmpty() && nomorRangka.length < 6,
                    supportingText = {
                        if (nomorRangka.isNotEmpty() && nomorRangka.length < 6) {
                            Text(
                                text = "Minimal 6 karakter",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("6-20 karakter, huruf dan angka saja")
                        }
                    }
                )

                OutlinedTextField(
                    value = nomorMesin,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isLetterOrDigit() }.uppercase()
                        if (filtered.length <= 16) {
                            nomorMesin = filtered
                        }
                    },
                    label = { Text("Nomor Mesin") },
                    placeholder = { Text("4G63T123456") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    isError = nomorMesin.isNotEmpty() && nomorMesin.length < 4,
                    supportingText = {
                        if (nomorMesin.isNotEmpty() && nomorMesin.length < 4) {
                            Text(
                                text = "Minimal 4 karakter",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("4-16 karakter, huruf dan angka saja")
                        }
                    }
                )

                OutlinedTextField(
                    value = tipeKendaraan,
                    onValueChange = { tipeKendaraan = it.uppercase() },
                    label = { Text("Tipe Kendaraan") },
                    placeholder = { Text("SUV") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = warnaKendaraan,
                    onValueChange = { warnaKendaraan = it.uppercase() },
                    label = { Text("Warna Kendaraan") },
                    placeholder = { Text("PUTIH") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = tahunKendaraan,
                    onValueChange = { tahunKendaraan = it },
                    label = { Text("Tahun Kendaraan") },
                    placeholder = { Text("2023") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Text(
                    text = "Semua field lainnya bersifat opsional",
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
                onClick = { clearForm() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Bersihkan")
            }
            Button(
                onClick = { submitVehicle() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && isFormValid()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Simpan Data")
                }
            }
        }
    }
    
    // Success Dialog
    if (isLoading) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "Menyimpan Data",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Mohon tunggu, data sedang diproses...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = { }
        )
    }
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "Berhasil!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = dialogMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("OK")
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
    
    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "Terjadi Kesalahan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = dialogMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@Preview
@Composable
fun InputVehicleContentPreview() {
    MaterialTheme {
        InputVehicleContent(onNavigateBack = {})
    }
}
