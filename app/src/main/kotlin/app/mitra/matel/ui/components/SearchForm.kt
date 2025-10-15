package app.mitra.matel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import app.mitra.matel.R
import app.mitra.matel.network.GrpcConnectionStatus
import io.grpc.ConnectivityState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchForm(
    searchText: String,
    selectedSearchType: String,
    onSearchTextChange: (String) -> Unit,
    onSearchTypeChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    searchDurationMs: Long? = null,
    grpcConnectionStatus: GrpcConnectionStatus? = null,
    onMicClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    
    // Remove the LaunchedEffect completely - debouncing now handled in ViewModel
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(bottom = 1.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Space top - 0.5% of SearchForm height
            Spacer(modifier = Modifier.weight(0.005f / 0.13f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.12f / 0.13f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // StatusAndDropdown - takes 40% height
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Status indicator (left)
                    grpcConnectionStatus?.let { status ->
                        val (statusText, statusColor) = when {
                            status.isHealthy && status.state == ConnectivityState.READY -> "READY" to MaterialTheme.colorScheme.primary
                            status.state == ConnectivityState.CONNECTING -> "CONNECTING" to MaterialTheme.colorScheme.tertiary
                            status.state == ConnectivityState.TRANSIENT_FAILURE -> "RETRY" to MaterialTheme.colorScheme.error
                            status.state == ConnectivityState.IDLE -> "IDLE" to MaterialTheme.colorScheme.onSurfaceVariant
                            else -> "ERROR" to MaterialTheme.colorScheme.error
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    statusColor.copy(alpha = 0.1f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = statusColor,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                                
                                if (status.latencyMs > 0) {
                                    val latencyColor = when {
                                        status.latencyMs < 50 -> MaterialTheme.colorScheme.primary
                                        status.latencyMs < 150 -> MaterialTheme.colorScheme.tertiary
                                        status.latencyMs < 300 -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                    
                                    Text(
                                        text = "${status.latencyMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = latencyColor,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                }
                                
                                if (!status.errorMessage.isNullOrEmpty()) {
                                    Text(
                                        text = "ERR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                    }
                    
                    // 2. Search duration (center)
                    if (searchDurationMs != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${searchDurationMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                    } else {
                        // Empty spacer when no search duration
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    // 3. Dropdown (right)
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        Box(
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .width(120.dp)
                                .height(30.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedSearchType,
                                    fontSize = 12.sp
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            listOf("nopol", "noka", "nosin").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontSize = 12.sp) },
                                    onClick = {
                                        onSearchTypeChange(option)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Input row - takes 60% height
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onMicClick
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(R.raw.ic_mic)
                                .decoderFactory(SvgDecoder.Factory())
                                .build(),
                            contentDescription = "Microphone",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Custom TextField with debounced search
                    BasicTextField(
                        value = searchText,
                        onValueChange = onSearchTextChange,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        readOnly = false,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(start = 12.dp, end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchText.isEmpty()) {
                                        Text(
                                            "Pencarian $selectedSearchType",
                                            style = TextStyle(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )
                    
                    Button(
                        onClick = onClear,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("X")
                    }
                }
            }

            // Space bottom - 0.5% of SearchForm height
            Spacer(modifier = Modifier.weight(0.005f / 0.13f))
        }
    }
}

@Preview
@Composable
fun SearchFormPreview() {
    var searchText by remember { mutableStateOf("") }
    var selectedSearchType by remember { mutableStateOf("nopol") }
    
    MaterialTheme {
        SearchForm(
            searchText = searchText,
            selectedSearchType = selectedSearchType,
            onSearchTextChange = { searchText = it },
            onSearchTypeChange = { selectedSearchType = it },
            onSearch = {},
            onClear = { searchText = "" },
            modifier = Modifier.height(80.dp)
        )
    }
}
