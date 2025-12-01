package app.mitra.matel.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import app.mitra.matel.R
import app.mitra.matel.network.ApiService
import app.mitra.matel.network.models.ProfileResponse
import app.mitra.matel.network.models.VehicleCountResponse
import app.mitra.matel.utils.SessionManager
import app.mitra.matel.ui.theme.MitraMatelTheme
import app.mitra.matel.ui.theme.Purple40
import app.mitra.matel.AppConfig
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SidebarMenu(
    selectedItem: String,
    onMenuItemClick: (String) -> Unit,
    onClose: () -> Unit,
    profile: ProfileResponse? = null,
    apiService: ApiService? = null,
    onRefreshProfile: () -> Unit = {}
) {
    // State for vehicle count
    var vehicleCount by remember { mutableStateOf<Int?>(null) }
    
    // Fetch vehicle count
    LaunchedEffect(Unit) {
        apiService?.getVehicleCount()?.onSuccess { response ->
            vehicleCount = response.total
        }
    }
    val menuSections = listOf(
        MenuSection(
            items = listOf(
                MenuItem("Profil Saya", Icons.Default.Person),
                MenuItem("Riwayat Pencarian", Icons.AutoMirrored.Filled.List)
            )
        ),
        MenuSection(
            items = listOf(
                MenuItem("Data Kendaraan Saya", iconRes = R.drawable.ic_car_vector),
                MenuItem("Input Data Kendaraan", icon = Icons.Default.Add)
            )
        ),
        MenuSection(
            items = listOf(
                MenuItem("Aktivasi & Pembayaran", Icons.Default.ShoppingCart),
                MenuItem("Riwayat Pembayaran", Icons.Default.DateRange)
            )
        ),
        MenuSection(
            items = listOf(
                MenuItem("About", Icons.Default.Info),
                MenuItem("Setting", Icons.Default.Settings),
                MenuItem("Logout", Icons.AutoMirrored.Filled.ExitToApp)
            )
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Compact Profile Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Purple40.copy(alpha = 0.6f),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Minimal Avatar
                    SidebarAvatarImage(
                        avatarData = profile?.assets?.get("avatar")?.toString()?.removePrefix("\"")?.removeSuffix("\""),
                        modifier = Modifier.size(40.dp)
                    )

                    // Compact Info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = profile?.fullName ?: "Loading...",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Tier",
                                modifier = Modifier.size(16.dp),
                                tint = Purple40
                            )
                            Text(
                                text = profile?.tier?.replaceFirstChar { it.uppercase() } ?: "Free",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Purple40
                            )
                        }
                        // Active Status Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isActive = profile?.subscriptionStatus == "active"
                            Icon(
                                if (isActive) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = "Status",
                                modifier = Modifier.size(16.dp),
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (isActive) "Aktif" else "Tidak Aktif",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Refresh Button
                    IconButton(
                        onClick = onRefreshProfile,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Profile",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Menu List with Equal Height Distribution
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                menuSections.forEachIndexed { sectionIndex, section ->
                    // Menu items in this section
                    section.items.forEach { item ->
                        SidebarMenuItem(
                            item = item,
                            isSelected = selectedItem == item.title,
                            onClick = { onMenuItemClick(item.title) }
                        )
                    }
                    
                    // Add divider after each section except the last one
                    if (sectionIndex < menuSections.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            // Minimal Footer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (vehicleCount != null) {
                            "${NumberFormat.getNumberInstance(Locale.forLanguageTag("id")).format(vehicleCount)} Kendaraan"
                        } else {
                            "Loading Kendaraan..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Purple40
                    )
                    Text(
                        text = "${AppConfig.VERSION_NAME} (Build ${AppConfig.VERSION_BUILD})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple40.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun SidebarMenuItem(
    item: MenuItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        Purple40.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isSelected) {
        Purple40
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // Increased height for better touch targets
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp), // Larger icons
                    tint = contentColor
                )
            } else if (item.iconRes != null) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp), // Larger icons
                    tint = contentColor
                )
            }
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge, // Larger text
                fontSize = 16.sp, // Explicit larger font size
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

data class MenuItem(
    val title: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null
)

data class MenuSection(
    val items: List<MenuItem>
)

@Composable
private fun SidebarAvatarImage(
    avatarData: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(avatarData) {
        if (avatarData != null && avatarData.startsWith("data:image/")) {
            try {
                val base64Data = avatarData.substringAfter("base64,")
                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Profile Avatar",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        DefaultSidebarAvatar(modifier)
    }
}

@Composable
private fun DefaultSidebarAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Purple40),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = "Avatar",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SidebarMenuPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            SidebarMenu(
                selectedItem = "Profil Saya",
                onMenuItemClick = {},
                onClose = {},
                onRefreshProfile = {}
            )
        }
    }
}
