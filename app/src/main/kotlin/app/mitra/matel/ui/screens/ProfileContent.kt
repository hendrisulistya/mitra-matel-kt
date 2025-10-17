package app.mitra.matel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.mitra.matel.network.models.ProfileDevice
import app.mitra.matel.network.models.ProfileResponse
import app.mitra.matel.utils.SessionManager
import kotlinx.serialization.json.JsonObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileContent(
    profile: ProfileResponse? = null,
    isLoading: Boolean = false,
    onEditProfile: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Avatar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Avatar",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (profile != null) {
            // Informasi Dasar Card
            ProfileInfoCard(
                title = "Informasi Dasar",
                icon = Icons.Default.Person,
                items = listOf(
                    ProfileInfoItem("Nama Lengkap", profile.fullName),
                    ProfileInfoItem("Email", profile.email),
                    ProfileInfoItem("Nomor Telepon", profile.telephone ?: "Tidak tersedia"),
                    ProfileInfoItem("Tier", profile.tier.uppercase()),
                    ProfileInfoItem("Status Aktivasi", profile.subscriptionStatus)
                ),
                modifier = Modifier.padding(top = 16.dp)
            )

            // Info Perangkat Card
            profile.device?.let { device ->
                ProfileInfoCard(
                    title = "Info Perangkat",
                    icon = Icons.Default.Phone,
                    items = listOf(
                        ProfileInfoItem("Model", device.model ?: "Tidak tersedia"),
                        ProfileInfoItem("Login Terakhir", device.lastLogin?.let { formatDateTime(it) } ?: "Tidak tersedia")
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Informasi Akun Card
            ProfileInfoCard(
                title = "Informasi Akun",
                icon = Icons.Default.Info,
                items = listOf(
                    ProfileInfoItem("Dibuat Pada", profile.createdAt?.let { formatDateTime(it) } ?: "Tidak tersedia")
                ),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onEditProfile,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }
            
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    title: String,
    icon: ImageVector,
    items: List<ProfileInfoItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            items.forEachIndexed { index, item ->
                ProfileInfoRow(
                    label = item.label,
                    value = item.value
                )
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal
        )
    }
}

private data class ProfileInfoItem(
    val label: String,
    val value: String
)

private fun formatDateTime(dateTimeString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        val date = inputFormat.parse(dateTimeString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateTimeString
    }
}

@Preview
@Composable
fun ProfileContentPreview() {
    MaterialTheme {
        val sampleProfile = ProfileResponse(
            id = "54dd9b61-0676-474b-8f11-8ae3faa9efc4",
            email = "anto@user.com",
            fullName = "Pak Anto",
            telephone = "0818888888",
            tier = "premium",
            assets = JsonObject(emptyMap()),
            device = ProfileDevice(
                id = "335b6473-4207-454b-9381-422f869fa3a5",
                uuid = "550e8400-e29b-41d4-a716-446655440100",
                model = "Generic Phone X",
                lastLogin = "2025-10-17T14:32:10.426508+07:00"
            ),
            subscriptionStatus = "none",
            createdAt = "2025-10-05T05:44:06.428266+07:00",
            updatedAt = "2025-10-17T14:32:10.158658+07:00"
        )
        
        ProfileContent(profile = sampleProfile)
    }
}
