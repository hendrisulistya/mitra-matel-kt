package app.mitra.matel.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import app.mitra.matel.R
import app.mitra.matel.utils.SessionManager

data class SubscriptionPlan(
    val name: String,
    val basePrice: String,
    val suffix: String = ""
)

data class BankAccount(
    val bankName: String,
    val accountNumber: String,
    val accountName: String,
    val logoRes: Int
)

@Composable
fun BankAccountCard(account: BankAccount) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bank Logo using AsyncImage for SVG support
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(account.logoRes)
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = "${account.bankName} Logo",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.bankName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = account.accountNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = account.accountName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PaymentAndActivationContent() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    // Get user phone number for account number generation
    val userProfile = sessionManager.getProfile()
    val phoneNumber = userProfile?.telephone ?: "000"
    val lastThreeDigits = phoneNumber.takeLast(3)
    
    // Bank accounts data
    val bankAccounts = listOf(
        BankAccount("BRI", "051501015186507", "Yanu Marwanto", R.raw.ic_bank_bri),
        BankAccount("BCA", "0132553848", "Yanu Marwanto", R.raw.ic_bank_bca),
        BankAccount("Mandiri", "1360017368678", "Yanu Marwanto", R.raw.ic_bank_mandiri)
    )
    
    // Unified subscription plans
    val weeklyPlan = SubscriptionPlan(
        "Mingguan",
        "50000",
        lastThreeDigits
    )
    
    val monthlyPlan = SubscriptionPlan(
        "Bulanan",
        "100000",
        lastThreeDigits
    )
    
    val yearlyPlan = SubscriptionPlan(
        "Tahunan",
        "1500000",
        lastThreeDigits
    )
    
    val plans = arrayOf(weeklyPlan, monthlyPlan, yearlyPlan)

    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // First Row: Bank Accounts
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Rekening Bank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                bankAccounts.forEach { account ->
                    BankAccountCard(account = account)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Second Row: Subscription Plans
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Paket Berlangganan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                plans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        isSelected = selectedPlan == plan,
                        onSelect = { selectedPlan = plan }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (selectedPlan != null) {
            Button(
                onClick = { /* Process payment */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                // Calculate final price for display
                val plan = selectedPlan!!
                val finalPrice = if (plan.suffix.isNotEmpty()) {
                    val basePriceNumber = plan.basePrice.replace(".", "")
                    val lastThreeDigits = basePriceNumber.takeLast(3)
                    val priceWithoutLastThree = basePriceNumber.dropLast(3)
                    val formattedPrice = "${priceWithoutLastThree}.${plan.suffix}"
                    "Rp $formattedPrice"
                } else {
                    "Rp ${plan.basePrice}"
                }
                Text("Lanjutkan Pembayaran - $finalPrice")
            }
        }

        // Third Row/Footer: WhatsApp Contact Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Butuh Bantuan?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(
                    onClick = {
                        val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/6281936706368?text=Halo,%20saya%20butuh%20bantuan%20dengan%20aplikasi%20Mitra%20Matel")
                        }
                        context.startActivity(whatsappIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366) // WhatsApp green color
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "WhatsApp",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Hubungi Admin via WhatsApp")
                }
            }
        }
    }
}

@Composable
fun PlanCard(plan: SubscriptionPlan, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Calculate final price by combining basePrice with suffix
                val finalPrice = if (plan.suffix.isNotEmpty()) {
                    val basePriceNumber = plan.basePrice.replace(".", "")
                    val lastThreeDigits = basePriceNumber.takeLast(3)
                    val priceWithoutLastThree = basePriceNumber.dropLast(3)
                    val formattedPrice = "${priceWithoutLastThree}.${plan.suffix}"
                    "Rp $formattedPrice"
                } else {
                    "Rp ${plan.basePrice}"
                }
                
                Text(
                    text = finalPrice,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
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
