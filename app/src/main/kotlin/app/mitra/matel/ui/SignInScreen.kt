package app.mitra.matel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.mitra.matel.network.ApiConfig
import app.mitra.matel.viewmodel.AuthState
import app.mitra.matel.viewmodel.AuthViewModel
import app.mitra.matel.R
import android.util.Log
import app.mitra.matel.AppConfig
import java.util.Locale

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = app.mitra.matel.viewmodel.AuthViewModelFactory(context))
    val loginState by viewModel.loginState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableStateOf(0) }

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is AuthState.Success -> {
                onSignInSuccess()
            }
            is AuthState.Error -> {
                errorMessage = state.message
                failedAttempts += 1
            }
            is AuthState.Conflict -> {
                errorMessage = null
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // Logo
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.raw.mmi_logo),
                contentDescription = "MMI Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Masuk",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        // Show the card if we have a message OR we already crossed attempts threshold
        val showErrorCard = (errorMessage != null) || (failedAttempts >= 3)
        if (showErrorCard) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = errorMessage ?: "Terjadi kesalahan saat login",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (failedAttempts >= 3) {
                        Text(
                            text = "Hubungi admin untuk bantuan",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            modifier = Modifier.clickable {
                                val message = "Halo admin, saya butuh bantuan untuk login."
                                val adminPhone = "6281936706368"
                                openAdminWhatsApp(context, adminPhone, message)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim().lowercase(Locale.ROOT) },
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
            label = { Text("Kata Sandi") },
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
                val sanitizedEmail = email.trim().lowercase(Locale.ROOT)
                if (sanitizedEmail.isNotBlank() && password.isNotBlank()) {
                    if (!isAllowedEmail(sanitizedEmail)) {
                        errorMessage = "Format email tidak valid"
                    } else {
                        viewModel.login(sanitizedEmail, password)
                    }
                } else {
                    errorMessage = "Mohon isi email dan kata sandi"
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

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Version ${AppConfig.getAppVersion()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

    // Conflict Modal
    if (loginState is AuthState.Conflict) {
        val conflict = (loginState as AuthState.Conflict).data
        
        val conflictData = conflict.data
        
        AlertDialog(
            onDismissRequest = { },
            title = {
                Column {
                    Text(
                        text = "Login Conflict",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = conflict.message ?: conflict.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Show device information card
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
                                text = "Informasi Tambahan",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                        text = if (conflictData.email != null) {
                            "Akun terhubung dengan perangkat (${conflictData.model}). Perangkat ini terdaftar untuk akun lain (${conflictData.email}). Login hanya dapat dilakukan dari perangkat yang terdaftar."
                        } else {
                            "Akun Anda terhubung dengan perangkat terdaftar (${conflictData.model}). Login hanya dapat dilakukan dari perangkat tersebut."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                        }
                    }
                    
                    
                    Text(
                        text = "Login tidak dapat dilanjutkan pada perangkat ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
            },
            dismissButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    OutlinedButton(onClick = { viewModel.resetState() }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }
}

@Preview
@Composable
private fun SignInPreview() {
    MaterialTheme {
        SignInScreen(onSignInSuccess = {}, onNavigateToSignUp = {})
    }
}

private fun isAllowedEmail(email: String): Boolean {
    val sanitized = email.trim()
    val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return emailPattern.matches(sanitized)
}

private fun openAdminWhatsApp(
    context: android.content.Context,
    phone: String?,
    message: String
) {
    if (phone.isNullOrBlank()) {
        android.widget.Toast.makeText(
            context,
            "Nomor admin tidak tersedia",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        return
    }

    try {
        val encodedMessage = android.net.Uri.encode(message)

        // Try WhatsApp deep link with phone number first
        val whatsappUri = "https://wa.me/$phone?text=$encodedMessage"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(whatsappUri))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Tidak dapat membuka WhatsApp: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}