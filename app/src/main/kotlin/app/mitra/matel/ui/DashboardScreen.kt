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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.mitra.matel.ui.components.*
import app.mitra.matel.ui.screens.*
import androidx.compose.ui.platform.LocalContext
import app.mitra.matel.viewmodel.AuthViewModel
import app.mitra.matel.viewmodel.SearchViewModel
import app.mitra.matel.viewmodel.ProfileViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.mitra.matel.network.GrpcService
import app.mitra.matel.utils.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigateToMicSearch: () -> Unit = {},
    onNavigateToVehicleDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authViewModel = remember { AuthViewModel(context) }
    val grpcService = remember { GrpcService(context) }
    val searchViewModel = remember { SearchViewModel(grpcService) }
    val profileViewModel = remember { ProfileViewModel(context) }
    val searchUiState by searchViewModel.uiState.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()
    val profile by profileViewModel.profile.collectAsState()
    val avatarUploadState by profileViewModel.avatarUploadState.collectAsState()
    
    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Fetch profile data when dashboard is first loaded (only if not cached)
    LaunchedEffect(Unit) {
        if (profile == null) {
            profileViewModel.fetchProfile()
        }
    }
    
    // Handle avatar upload state with notifications
    LaunchedEffect(avatarUploadState) {
        when (val currentState = avatarUploadState) {
            is app.mitra.matel.viewmodel.AvatarUploadState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Avatar berhasil diupload!",
                    duration = SnackbarDuration.Short
                )
                profileViewModel.resetAvatarUploadState()
            }
            is app.mitra.matel.viewmodel.AvatarUploadState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Gagal upload avatar: ${currentState.message}",
                    duration = SnackbarDuration.Long
                )
                profileViewModel.resetAvatarUploadState()
            }
            else -> {}
        }
    }
    var selectedMenuItem by remember { mutableStateOf<String?>(null) }
    var isSidebarVisible by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAnnouncement by remember { mutableStateOf(!sessionManager.isAnnouncementDismissed()) }
    var keyboardLayout by remember { mutableStateOf(KeyboardLayout.QWERTY) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                                onRefresh = { profileViewModel.fetchProfile() },
                                onAvatarUpload = { avatarBase64 ->
                                    profileViewModel.uploadAvatar(avatarBase64)
                                }
                            )
                        }
                        "Riwayat Pencarian" -> SearchHistoryContent(
                            onVehicleClick = onNavigateToVehicleDetail
                        )
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

        // Announcement Dialog - only shown when user is logged in (in Dashboard)
        if (showAnnouncement) {
            AnnouncementDialog(
                onDismiss = { 
                    showAnnouncement = false
                    sessionManager.setAnnouncementDismissed(true)
                }
            )
        }
        }
    }
}

@Composable
fun AnnouncementDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Pengumuman",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                
                // Additional info (optional)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "DATA ADIRA OKTOBER SUDAH DITAMBAHAKAN",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK")
                }
            }
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
