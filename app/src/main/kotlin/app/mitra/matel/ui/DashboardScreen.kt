package app.mitra.matel.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.mitra.matel.ui.components.*
import app.mitra.matel.ui.screens.*
import androidx.compose.ui.platform.LocalContext
import app.mitra.matel.viewmodel.AuthViewModel
import app.mitra.matel.viewmodel.SearchViewModel
import app.mitra.matel.viewmodel.ProfileViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.mitra.matel.network.GrpcService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigateToMicSearch: () -> Unit = {},
    onNavigateToVehicleDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val authViewModel = remember { AuthViewModel(context) }
    val grpcService = remember { GrpcService(context) }
    val searchViewModel = remember { SearchViewModel(grpcService) }
    val profileViewModel = remember { ProfileViewModel(context) }
    val searchUiState by searchViewModel.uiState.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()
    val profile by profileViewModel.profile.collectAsState()
    
    // Fetch profile data when dashboard is first loaded (only if not cached)
    LaunchedEffect(Unit) {
        if (profile == null) {
            profileViewModel.fetchProfile()
        }
    }
    var selectedMenuItem by remember { mutableStateOf<String?>(null) }
    var isSidebarVisible by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var keyboardLayout by remember { mutableStateOf(KeyboardLayout.QWERTY) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Main Dashboard Content (always visible)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top App Bar with hamburger menu and keyboard toggle
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { isSidebarVisible = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // Keyboard layout toggle
                    IconButton(
                        onClick = {
                            keyboardLayout = when (keyboardLayout) {
                                KeyboardLayout.NUMERIC -> KeyboardLayout.QWERTY
                                KeyboardLayout.QWERTY -> KeyboardLayout.NUMERIC
                            }
                        }
                    ) {
                        Text(
                            text = "⌨",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    // Settings icon (for future keyboard settings)
                    IconButton(
                        onClick = { /* TODO: Open keyboard settings */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Keyboard Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            // Dashboard content - 3 sections (total 100% height)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // 1. SearchResultList - 39% height (increased from 37%)
                SearchResultList(
                    results = searchUiState.results,
                    error = searchUiState.error,
                    onVehicleClick = onNavigateToVehicleDetail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.39f)
                )

                // 2. SearchForm - 13% height (includes internal spacing)
                SearchForm(
                    searchText = searchUiState.searchText,
                    selectedSearchType = searchUiState.searchType,
                    onSearchTextChange = { searchViewModel.updateSearchText(it) },
                    onSearchTypeChange = { searchViewModel.updateSearchType(it) },
                    onSearch = { searchViewModel.performSearch() },
                    onClear = { searchViewModel.clearResults() },
                    searchDurationMs = searchUiState.searchDurationMs,
                    grpcConnectionStatus = searchUiState.grpcConnectionStatus,
                    onMicClick = onNavigateToMicSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.13f)
                )

                // 3. SearchKeyboard - 48% height (reduced from 50%)
                // In DashboardScreen, modify the SearchKeyboard handler:
                SearchKeyboard(
                    keyboardLayout = keyboardLayout,
                    onKeyClick = { key ->
                        if (key == "⌫") {
                            // Use current ViewModel state instead of UI state
                            val currentText = searchViewModel.uiState.value.searchText
                            if (currentText.isNotEmpty()) {
                                searchViewModel.updateSearchText(currentText.dropLast(1))
                            }
                        } else {
                            val currentText = searchViewModel.uiState.value.searchText
                            searchViewModel.updateSearchText(currentText + key)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.48f)
                )
            }
        }

        // Overlay screens (Profile, Analytics, Messages, Settings)
        selectedMenuItem?.let { screen ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Top App Bar with back button
                TopAppBar(
                    title = { Text(screen) },
                    navigationIcon = {
                        IconButton(onClick = { selectedMenuItem = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Screen content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    when (screen) {
                        "Profil Saya" -> {
                            ProfileContent(
                                profile = profile,
                                isLoading = profileState is app.mitra.matel.viewmodel.ProfileState.Loading,
                                onRefresh = { profileViewModel.fetchProfile() }
                            )
                        }
                        "Riwayat Pencarian" -> SearchHistoryContent()
                        "Data Kendaraan saya" -> MyVehicleDataContent()
                        "Input Data Kendaraan" -> InputVehicleContent()
                        "Aktivasi dan Pembayaran" -> PaymentAndActivationContent()
                        "Riwayat Pembayaran" -> PaymentHistoryContent()
                        "About" -> AboutContent()
                        "Setting" -> SettingsContent()
                    }
                }
            }
        }

        // Sidebar Drawer with slide animation
        // Overlay background
        if (isSidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    .clickable { isSidebarVisible = false }
            )
        }

        // Sidebar with slide animation
        val sidebarOffset by animateDpAsState(
            targetValue = if (isSidebarVisible) 0.dp else (-320).dp,
            animationSpec = tween(durationMillis = 300),
            label = "sidebar_slide"
        )

        Box(
            modifier = Modifier
                .offset(x = sidebarOffset)
                .fillMaxHeight()
        ) {
            SidebarMenu(
                selectedItem = selectedMenuItem ?: "",
                onMenuItemClick = { menuItem ->
                    if (menuItem == "Logout") {
                        showLogoutDialog = true
                        isSidebarVisible = false
                    } else {
                        selectedMenuItem = menuItem
                        isSidebarVisible = false
                    }
                },
                onClose = { isSidebarVisible = false },
                profile = profile
            )
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            LogoutDialog(
                onDismiss = { showLogoutDialog = false },
                onConfirm = {
                    showLogoutDialog = false
                    authViewModel.logout { _: Boolean, _: String? ->
                        onLogout()
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    MaterialTheme {
        DashboardScreen(
            onLogout = {},
            onNavigateToMicSearch = {}
        )
    }
}
