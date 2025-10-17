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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import app.mitra.matel.ui.SignInScreen
import app.mitra.matel.ui.SignUpScreen
import app.mitra.matel.ui.WelcomeScreen
import app.mitra.matel.ui.DashboardScreen
import app.mitra.matel.ui.screens.MicSearchContent
import app.mitra.matel.ui.screens.VehicleDetailContent
import app.mitra.matel.utils.SessionManager
import app.mitra.matel.viewmodel.AuthViewModel
import app.mitra.matel.viewmodel.AuthState
import app.mitra.matel.network.NetworkDebugHelper

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        val context = LocalContext.current
        val sessionManager = remember { SessionManager(context) }
        val authViewModel = remember { AuthViewModel(context) }
        val navController = rememberAnimatedNavController()
        
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
            AnimatedNavHost(
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
                    onBack = { navController.popBackStack() }
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
            }
        }
    }
}