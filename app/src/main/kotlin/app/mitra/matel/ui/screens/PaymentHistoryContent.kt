package app.mitra.matel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class PaymentHistory(
    val id: String,
    val plan: String,
    val amount: String,
    val date: String,
    val status: PaymentStatus,
    val paymentMethod: String
)

enum class PaymentStatus {
    SUCCESS, PENDING, FAILED
}

@Composable
fun PaymentHistoryContent() {
    val payments = listOf(
        PaymentHistory("INV-2025-001", "Premium", "Rp 150.000", "14 Okt 2025", PaymentStatus.SUCCESS, "Transfer Bank"),
        PaymentHistory("INV-2025-002", "Basic", "Rp 50.000", "10 Sep 2025", PaymentStatus.SUCCESS, "E-Wallet"),
        PaymentHistory("INV-2025-003", "Enterprise", "Rp 500.000", "05 Agt 2025", PaymentStatus.PENDING, "Transfer Bank"),
        PaymentHistory("INV-2025-004", "Basic", "Rp 50.000", "20 Jul 2025", PaymentStatus.FAILED, "Credit Card")
    )

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
                Text("Total: ${payments.size} transaksi", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(payments) { payment ->
                PaymentHistoryCard(payment)
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(payment: PaymentHistory) {
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
                    Icons.Default.Menu,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = payment.id,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paket ${payment.plan}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = payment.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = payment.paymentMethod,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = payment.amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = when (payment.status) {
                        PaymentStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                        PaymentStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                        PaymentStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (payment.status) {
                            PaymentStatus.SUCCESS -> "Berhasil"
                            PaymentStatus.PENDING -> "Pending"
                            PaymentStatus.FAILED -> "Gagal"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (payment.status) {
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

@Preview
@Composable
fun PaymentHistoryContentPreview() {
    MaterialTheme {
        PaymentHistoryContent()
    }
}
