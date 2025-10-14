package app.mitra.matel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class SubscriptionPlan(
    val name: String,
    val price: String,
    val duration: String,
    val features: List<String>
)

@Composable
fun PaymentAndActivationContent() {
    val plans = listOf(
        SubscriptionPlan(
            "Basic",
            "Rp 50.000",
            "1 Bulan",
            listOf("Akses 100 pencarian", "Data dasar kendaraan", "Support email")
        ),
        SubscriptionPlan(
            "Premium",
            "Rp 150.000",
            "3 Bulan",
            listOf("Akses unlimited pencarian", "Data lengkap kendaraan", "Riwayat pencarian", "Support prioritas")
        ),
        SubscriptionPlan(
            "Enterprise",
            "Rp 500.000",
            "1 Tahun",
            listOf("Akses unlimited pencarian", "Data lengkap kendaraan", "API Access", "Multi-user", "Support 24/7")
        )
    )

    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Aktivasi dan Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pilih paket langganan yang sesuai dengan kebutuhan Anda", style = MaterialTheme.typography.bodyMedium)
            }
        }

        plans.forEach { plan ->
            PlanCard(
                plan = plan,
                isSelected = selectedPlan == plan,
                onSelect = { selectedPlan = plan }
            )
        }

        if (selectedPlan != null) {
            Button(
                onClick = { /* Process payment */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lanjutkan Pembayaran - ${selectedPlan?.price}")
            }
        }
    }
}

@Composable
fun PlanCard(plan: SubscriptionPlan, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plan.duration,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = plan.price,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            plan.features.forEach { feature ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = feature, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Terpilih",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PaymentAndActivationContentPreview() {
    MaterialTheme {
        PaymentAndActivationContent()
    }
}
