package app.mitra.matel.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import app.mitra.matel.ui.theme.MitraMatelTheme
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.mitra.matel.ui.SignInScreen
import app.mitra.matel.ui.SignUpScreen
import app.mitra.matel.ui.WelcomeScreen
import app.mitra.matel.ui.DashboardScreen
import app.mitra.matel.ui.screens.MicSearchContent
import app.mitra.matel.ui.screens.VehicleDetailContent
import app.mitra.matel.ui.screens.TermOfServiceScreen
import app.mitra.matel.ui.screens.PrivacyPolicyScreen
import app.mitra.matel.utils.SessionManager
import app.mitra.matel.viewmodel.AuthViewModel
import app.mitra.matel.viewmodel.AuthState
import app.mitra.matel.viewmodel.SearchViewModel
import app.mitra.matel.network.NetworkDebugHelper
import app.mitra.matel.network.GrpcService
import app.mitra.matel.network.HttpClientFactory
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import android.net.Network

@Composable
@Preview
fun app() {
    MitraMatelTheme {
        val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
        val authViewModel = remember { AuthViewModel(context) }
        val grpcService = remember { GrpcService(context) }
        val searchViewModel = remember { SearchViewModel(grpcService) }
        
        // Register gRPC service with MainActivity for lifecycle management
        LaunchedEffect(grpcService) {
            app.mitra.matel.MainActivity.currentGrpcService = grpcService
        }
        val navController = rememberNavController()
        val coroutineScope = rememberCoroutineScope()
        
        var isInitializing by remember { mutableStateOf(true) }
        var startDestination by remember { mutableStateOf("welcome") }
        
        // Auto-login logic on app start
        LaunchedEffect(Unit) {
            if (sessionManager.isLoggedIn()) {
                startDestination = "dashboard"
                isInitializing = false
            } else {
                // Check network connectivity before attempting auto-login
                if (NetworkDebugHelper.isNetworkAvailable(context)) {
                    // Check for saved credentials and attempt auto-login
                    val (savedEmail, savedPassword) = authViewModel.getSavedCredentials()
                    if (!savedEmail.isNullOrBlank() && !savedPassword.isNullOrBlank()) {
                        // Attempt auto-login with saved credentials
                        authViewModel.login(savedEmail, savedPassword, rememberCredentials = true)
                    } else {
                        startDestination = "welcome"
                        isInitializing = false
                    }
                } else {
                    // No network - skip auto-login and go to welcome
                    startDestination = "welcome"
                    isInitializing = false
                }
            }
        }
        
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (sessionManager.isLoggedIn()) {
                        val (email, password) = authViewModel.getSavedCredentials()
                        if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                            when {
                                sessionManager.isTokenExpired() -> {
                                    val (email, password) = authViewModel.getSavedCredentials()
                                    val hasCreds = !email.isNullOrBlank() && !password.isNullOrBlank()
                                    if (hasCreds) {
                                        if (NetworkDebugHelper.isNetworkAvailable(context)) {
                                            authViewModel.login(email!!, password!!, rememberCredentials = true)
                                        } else {
                                            // Stay logged in; attempt re-login when network becomes available
                                        }
                                    } else {
                                        sessionManager.clearSession()
                                        navController.navigate("welcome") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                                sessionManager.isInGracePeriod() -> {
                                    if (NetworkDebugHelper.isNetworkAvailable(context)) {
                                        authViewModel.login(email, password, rememberCredentials = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        
        DisposableEffect(Unit) {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val (email, password) = authViewModel.getSavedCredentials()
                    val hasCreds = !email.isNullOrBlank() && !password.isNullOrBlank()
                    if (hasCreds) {
                        if (sessionManager.isTokenExpired()) {
                            coroutineScope.launch {
                                var attempts = 0
                                var success = false
                                while (attempts < 3 && !success) {
                                    success = app.mitra.matel.network.HttpClientFactory.refreshTokenWithSavedCredentials(context)
                                    if (!success) {
                                        kotlinx.coroutines.delay(2000)
                                    }
                                    attempts++
                                }
                                if (!success) {
                                    sessionManager.clearSession()
                                }
                            }
                        } else if (sessionManager.isInGracePeriod()) {
                            coroutineScope.launch {
                                app.mitra.matel.network.HttpClientFactory.refreshTokenWithSavedCredentials(context)
                            }
                        }
                    }
                }
            }
            val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            cm.registerNetworkCallback(request, callback)
            onDispose { cm.unregisterNetworkCallback(callback) }
        }
        
        // Observe auth state changes for auto-login and schedule proactive refresh in grace period (exp - 1 hour)
        LaunchedEffect(authViewModel.loginState) {
            authViewModel.loginState.collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        startDestination = "dashboard"
                        isInitializing = false
                        val (email, password) = authViewModel.getSavedCredentials()
                        val timeUntilGrace = sessionManager.getTimeUntilGraceMillis()
                        if (!email.isNullOrBlank() && !password.isNullOrBlank() && timeUntilGrace != null && sessionManager.shouldScheduleGraceRefresh()) {
                            if (timeUntilGrace > 0) {
                                sessionManager.markGraceRefreshScheduled()
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(timeUntilGrace)
                                    if (sessionManager.isLoggedIn() && sessionManager.isInGracePeriod()) {
                                        var attempts = 0
                                        val maxAttempts = 5
                                        var waitMs = 30_000L
                                        val expEpoch = sessionManager.getTokenExpiryEpoch()
                                        val expDeadlineMs = expEpoch?.let { it * 1000 - System.currentTimeMillis() } ?: 0L
                                        val maxTotalWaitMs = if (expDeadlineMs > 0) minOf(900_000L, expDeadlineMs) else 900_000L
                                        var elapsedMs = 0L
                                        while (!NetworkDebugHelper.isNetworkAvailable(context) && attempts < maxAttempts && sessionManager.isInGracePeriod() && elapsedMs < maxTotalWaitMs) {
                                            kotlinx.coroutines.delay(waitMs)
                                            attempts++
                                            elapsedMs += waitMs
                                            waitMs = (waitMs * 2).coerceAtMost(300_000L)
                                        }
                                        if (sessionManager.isTokenExpired()) {
                                            val hasCreds = !email.isNullOrBlank() && !password.isNullOrBlank()
                                            if (hasCreds) {
                                                var refreshAttempts = 0
                                                var refreshSuccess = false
                                                while (NetworkDebugHelper.isNetworkAvailable(context) && refreshAttempts < 3 && !refreshSuccess) {
                                                    refreshSuccess = HttpClientFactory.refreshTokenWithSavedCredentials(context)
                                                    if (!refreshSuccess) {
                                                        kotlinx.coroutines.delay(2000)
                                                        refreshAttempts++
                                                    }
                                                }
                                                if (!refreshSuccess) {
                                                    sessionManager.clearSession()
                                                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                                                }
                                            } else {
                                                sessionManager.clearSession()
                                                navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                                            }
                                        } else if (NetworkDebugHelper.isNetworkAvailable(context) && sessionManager.isInGracePeriod()) {
                                            val refreshed = HttpClientFactory.refreshTokenWithSavedCredentials(context)
                                            if (!refreshed) {
                                                // keep session; another attempt will occur later or on resume
                                            }
                                        }
                                    }
                                }
                            } else {
                                sessionManager.markGraceRefreshScheduled()
                                val refreshed = HttpClientFactory.refreshTokenWithSavedCredentials(context)
                                if (!refreshed) {
                                    // keep session; will retry on resume or when network recovers
                                }
                            }
                        }
                    }
                    is AuthState.Error, is AuthState.Conflict -> {
                        startDestination = "welcome"
                        isInitializing = false
                    }
                    is AuthState.Loading -> {
                    }
                    else -> { }
                }
            }
        }

        // Set up session cleared listener for background logout navigation
        LaunchedEffect(Unit) {
            sessionManager.setOnSessionClearedListener {
                // Navigate to welcome screen when session is cleared from background
                navController.navigate("welcome") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        
        // Observe session state changes for background logout detection
        LaunchedEffect(sessionManager.sessionState) {
            sessionManager.sessionState.collect { isLoggedIn ->
                // If session was cleared in background, navigate to welcome
                if (!isLoggedIn && startDestination == "dashboard") {
                    startDestination = "welcome"
                    isInitializing = false
                }
            }
        }
        
        if (isInitializing) {
            // Show splash/loading screen while checking auto-login
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
            composable(
                route = "welcome",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 2 },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it / 2 },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) {
                WelcomeScreen(
                    onSignIn = { navController.navigate("signin") },
                    onSignUp = { navController.navigate("signup") },
                )
            }

            composable(
                route = "signin",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it / 2 },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) {
                SignInScreen(
                    onBack = { navController.popBackStack() },
                    onSignInSuccess = {
                        navController.navigate("dashboard") {
                            // Clear back stack to prevent going back to login
                            popUpTo("welcome") { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = { navController.navigate("signup") }
                )
            }

            composable(
                route = "signup",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it / 2 },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) {
                SignUpScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToSignIn = { navController.navigate("signin") }
                )
            }

            composable(
                route = "dashboard",
                enterTransition = {
                    fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(250))
                }
            ) {
                DashboardScreen(
                    searchViewModel = searchViewModel,
                    onLogout = {
                        // Clear session and saved credentials on logout
                        sessionManager.clearAll()  // Clear everything including saved credentials
                        authViewModel.resetState()

                        // Navigate to welcome and clear back stack
                        navController.navigate("welcome") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToMicSearch = {
                        navController.navigate("mic_search")
                    },
                    onNavigateToVehicleDetail = { vehicleId ->
                        navController.navigate("vehicle_detail/$vehicleId") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    onNavigateToTerms = {
                        navController.navigate("terms_of_service")
                    },
                    onNavigateToPrivacy = {
                        navController.navigate("privacy_policy")
                    }
                )
            }

            composable(
                route = "dashboard/about",
                enterTransition = {
                    fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(250))
                }
            ) {
                DashboardScreen(
                    searchViewModel = searchViewModel,
                    initialSelectedMenuItem = "About",
                    onLogout = {
                        // Clear session on logout
                        sessionManager.clearSession()
                        authViewModel.resetState()

                        // Navigate to welcome and clear back stack
                        navController.navigate("welcome") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToMicSearch = {
                        navController.navigate("mic_search")
                    },
                    onNavigateToVehicleDetail = { vehicleId ->
                        navController.navigate("vehicle_detail/$vehicleId")
                    },
                    onNavigateBack = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    onNavigateToTerms = {
                        navController.navigate("terms_of_service")
                    },
                    onNavigateToPrivacy = {
                        navController.navigate("privacy_policy")
                    }
                )
            }

            composable(
                route = "mic_search",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) {
                MicSearchContent(
                    onBack = { navController.popBackStack() },
                    onSearchResult = { searchText ->
                        // Keep user on MicSearch page to see results
                        // No navigation needed - results will be displayed on the same page
                    },
                    onVehicleClick = { vehicleId ->
                        navController.navigate("vehicle_detail/$vehicleId")
                    },
                    searchViewModel = searchViewModel
                )
            }

            composable(
                route = "vehicle_detail/{vehicleId}",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
                VehicleDetailContent(
                    vehicleId = vehicleId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "terms_of_service",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) {
                TermOfServiceScreen(
                    onBackClick = { 
                        navController.navigate("dashboard/about") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "privacy_policy",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) {
                PrivacyPolicyScreen(
                    onBackClick = { 
                        navController.navigate("dashboard/about") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            }
            }
        }
    }
}