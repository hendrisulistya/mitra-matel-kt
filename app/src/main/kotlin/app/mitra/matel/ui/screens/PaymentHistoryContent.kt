package app.mitra.matel.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.mitra.matel.network.ApiService
import app.mitra.matel.network.models.PaymentHistoryItem
import app.mitra.matel.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

enum class PaymentStatus {
    SUCCESS, PENDING, FAILED
}

@Composable
fun PaymentHistoryContent() {
    val context = LocalContext.current
    val apiService = remember { ApiService(context) }
    var paymentHistory by remember { mutableStateOf<List<PaymentHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPayment by remember { mutableStateOf<PaymentHistoryItem?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val result = apiService.getPaymentHistory()
                result.fold(
                    onSuccess = { history ->
                        paymentHistory = history
                        isLoading = false
                    },
                    onFailure = { error ->
                        errorMessage = error.message
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message
                isLoading = false
            }
        }
    }

    // Show payment detail modal
    selectedPayment?.let { payment ->
        PaymentDetailModal(
            payment = payment,
            onDismiss = { selectedPayment = null }
        )
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
                Text("Riwayat Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Daftar transaksi pembayaran langganan", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total: ${paymentHistory.size} transaksi", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            paymentHistory.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Belum ada riwayat pembayaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Transaksi pembayaran akan muncul di sini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(paymentHistory) { payment ->
                        PaymentHistoryCard(
                            payment = payment,
                            onClick = { selectedPayment = payment }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(
    payment: PaymentHistoryItem,
    onClick: () -> Unit = {}
) {
    val paymentStatus = when (payment.status.lowercase()) {
        "completed" -> PaymentStatus.SUCCESS
        "pending" -> PaymentStatus.PENDING
        else -> PaymentStatus.FAILED
    }
    
    val formattedAmount = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(payment.amount)
    
    val planName = when (payment.subscriptionPlan.lowercase()) {
        "trial" -> "Trial"
        "regular" -> "Regular"
        "premium" -> "Premium"
        else -> payment.subscriptionPlan.replaceFirstChar { it.uppercase() }
    }
    
    val paymentMethodName = when (payment.paymentMethod.lowercase()) {
        "bank_transfer" -> "Transfer Bank"
        "free_trial" -> "Trial Gratis"
        "credit_card" -> "Kartu Kredit"
        "e_wallet" -> "E-Wallet"
        else -> payment.paymentMethod.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    Icons.Default.Menu,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = payment.orderId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paket $planName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = payment.paidAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = paymentMethodName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = when (paymentStatus) {
                        PaymentStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                        PaymentStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                        PaymentStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (paymentStatus) {
                            PaymentStatus.SUCCESS -> "Berhasil"
                            PaymentStatus.PENDING -> "Pending"
                            PaymentStatus.FAILED -> "Gagal"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (paymentStatus) {
                            PaymentStatus.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                            PaymentStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                            PaymentStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentDetailModal(
    payment: PaymentHistoryItem,
    onDismiss: () -> Unit
) {
    val paymentStatus = when (payment.status.lowercase()) {
        "completed" -> PaymentStatus.SUCCESS
        "pending" -> PaymentStatus.PENDING
        else -> PaymentStatus.FAILED
    }
    
    val formattedAmount = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(payment.amount)
    
    val planName = when (payment.subscriptionPlan.lowercase()) {
        "trial" -> "Trial"
        "regular" -> "Regular"
        "premium" -> "Premium"
        else -> payment.subscriptionPlan.replaceFirstChar { it.uppercase() }
    }
    
    val paymentMethodName = when (payment.paymentMethod.lowercase()) {
        "bank_transfer" -> "Transfer Bank"
        "free_trial" -> "Trial Gratis"
        "credit_card" -> "Kartu Kredit"
        "e_wallet" -> "E-Wallet"
        else -> payment.paymentMethod.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detail Pembayaran",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup"
                        )
                    }
                }

                Divider()

                // Payment Information
                DetailRow("Order ID", payment.orderId)
                DetailRow("Subscription ID", payment.subscriptionId)
                DetailRow("Jumlah", formattedAmount)
                DetailRow("Mata Uang", payment.currency.uppercase())
                DetailRow("Status", when (paymentStatus) {
                    PaymentStatus.SUCCESS -> "Berhasil"
                    PaymentStatus.PENDING -> "Pending"
                    PaymentStatus.FAILED -> "Gagal"
                })
                DetailRow("Metode Pembayaran", paymentMethodName)
                DetailRow("Paket Langganan", planName)
                DetailRow("Durasi Langganan", "${payment.subscriptionDays} hari")
                DetailRow("Status Langganan", payment.subscriptionStatus.replaceFirstChar { it.uppercase() })
                DetailRow("Tanggal Bayar", payment.paidAt)
                DetailRow("Mulai Langganan", payment.subscriptionStart)
                DetailRow("Berakhir Langganan", payment.subscriptionEnd)
                DetailRow("Dibuat", payment.createdAt)
                DetailRow("Diperbarui", payment.updatedAt)

                // Payment Data Details (if available)
                payment.paymentData.let { paymentData ->
                    if (paymentData.senderName != null || 
                        paymentData.transferAmount != null ||
                        paymentData.senderBankName != null) {
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Detail Transfer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Divider()
                        
                        paymentData.senderName?.let { DetailRow("Nama Pengirim", it) }
                        paymentData.senderPhone?.let { DetailRow("No. HP Pengirim", it) }
                        paymentData.senderBankName?.let { DetailRow("Bank Pengirim", it) }
                        paymentData.senderAccountNumber?.let { DetailRow("No. Rekening Pengirim", it) }
                        paymentData.receiverBankName?.let { DetailRow("Bank Penerima", it) }
                        paymentData.receiverAccountNumber?.let { DetailRow("No. Rekening Penerima", it) }
                        paymentData.receiverAccountName?.let { DetailRow("Nama Penerima", it) }
                        paymentData.transferAmount?.let { 
                            DetailRow("Jumlah Transfer", NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(it))
                        }
                        paymentData.transferDate?.let { DetailRow("Tanggal Transfer", it) }
                        paymentData.transferNote?.let { DetailRow("Catatan Transfer", it) }
                        paymentData.referenceNumber?.let { DetailRow("No. Referensi", it) }
                        paymentData.verificationStatus?.let { DetailRow("Status Verifikasi", it) }
                        paymentData.verifiedAt?.let { DetailRow("Diverifikasi Pada", it) }
                        paymentData.verifiedBy?.let { DetailRow("Diverifikasi Oleh", it) }
                        paymentData.verificationNotes?.let { DetailRow("Catatan Verifikasi", it) }
                        
                        if (paymentData.trialDays != null) {
                            DetailRow("Hari Trial", "${paymentData.trialDays} hari")
                        }
                        
                        paymentData.activatedAt?.let { DetailRow("Diaktivasi Pada", it) }
                        paymentData.activatedBy?.let { DetailRow("Diaktivasi Oleh", it) }
                    }
                }

                // Close button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview
@Composable
fun PaymentHistoryContentPreview() {
    MaterialTheme {
        PaymentHistoryContent()
    }
}
