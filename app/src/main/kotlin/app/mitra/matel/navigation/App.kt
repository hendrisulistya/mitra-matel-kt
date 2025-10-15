package app.mitra.matel.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        val context = LocalContext.current
        val sessionManager = remember { SessionManager(context) }
        val navController = rememberAnimatedNavController()

        // Determine start destination based on login status
        val startDestination = if (sessionManager.isLoggedIn()) "dashboard" else "welcome"

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