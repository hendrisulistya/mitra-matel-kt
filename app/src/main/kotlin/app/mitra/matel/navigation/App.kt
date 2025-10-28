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

@Composable
@Preview
fun App() {
    MitraMatelTheme {
        val context = LocalContext.current
        val sessionManager = remember { SessionManager(context) }
        val authViewModel = remember { AuthViewModel(context) }
        val grpcService = remember { GrpcService(context) }
        val searchViewModel = remember { SearchViewModel(grpcService) }
        
        // Register gRPC service with MainActivity for lifecycle management
        LaunchedEffect(grpcService) {
            app.mitra.matel.MainActivity.currentGrpcService = grpcService
        }
        val navController = rememberNavController()
        
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
        
        // Observe auth state changes for auto-login
        LaunchedEffect(authViewModel.loginState) {
            authViewModel.loginState.collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        startDestination = "dashboard"
                        isInitializing = false
                    }
                    is AuthState.Error, is AuthState.Conflict -> {
                        startDestination = "welcome"
                        isInitializing = false
                    }
                    is AuthState.Loading -> {
                        // Keep showing loading screen during auto-login
                    }
                    else -> { /* Keep waiting */ }
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