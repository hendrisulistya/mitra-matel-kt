package app.mitra.matel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.mitra.matel.network.ApiConfig
import app.mitra.matel.viewmodel.AuthState
import app.mitra.matel.viewmodel.AuthViewModel
import android.util.Log

@Composable
fun SignInScreen(
    onBack: () -> Unit = {},
    onSignInSuccess: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { AuthViewModel(context) }
    val loginState by viewModel.loginState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Debug: Log API URL on screen load
    LaunchedEffect(Unit) {
        Log.d("SignInScreen", "API Base URL: ${ApiConfig.BASE_URL}")
        Log.d("SignInScreen", "Login Endpoint: ${ApiConfig.BASE_URL}${ApiConfig.Endpoints.LOGIN}")
    }

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is AuthState.Success -> {
                onSignInSuccess()
            }
            is AuthState.Error -> {
                errorMessage = state.message
            }
            is AuthState.Conflict -> {
                errorMessage = null
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Logo placeholder
        Surface(
            modifier = Modifier.size(120.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.large
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "MM",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Masuk",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        errorMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email icon")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password icon")
            },
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sign In Button
        Button(
            onClick = {
                errorMessage = null
                if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.login(email, password)
                } else {
                    errorMessage = "Please enter email and password"
                }
            },
            enabled = loginState !is AuthState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (loginState is AuthState.Loading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Text("Masuk...", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text("Masuk", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Registration link
        Text(
            text = "Belum Punya Akun. Silahkan Daftar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onNavigateToSignUp() }
        )
    }

    // Conflict Modal
    if (loginState is AuthState.Conflict) {
        val conflict = (loginState as AuthState.Conflict).data
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = {
                Column {
                    Text(
                        text = "Perangkat Ganda Terdeteksi",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = conflict.message ?: "Anda telah login di perangkat lain.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Informasi Perangkat",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = conflict.currentDevice.model,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Login Terakhir: ${conflict.currentDevice.lastLogin}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "IP: ${conflict.currentDevice.lastIp}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Anda dapat melanjutkan login di perangkat ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.forceLogin(email, password) },
                    enabled = loginState !is AuthState.Loading
                ) {
                    Text("Tetap Masuk")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.resetState() }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Preview
@Composable
private fun SignInPreview() {
    MaterialTheme {
        SignInScreen(onBack = {}, onSignInSuccess = {}, onNavigateToSignUp = {})
    }
}