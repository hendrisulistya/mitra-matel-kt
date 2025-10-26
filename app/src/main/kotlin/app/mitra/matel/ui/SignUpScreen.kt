package app.mitra.matel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mitra.matel.R
import app.mitra.matel.viewmodel.AuthViewModel
import app.mitra.matel.viewmodel.RegisterState

@Composable
fun SignUpScreen(
    onBack: () -> Unit = {},
    onNavigateToSignIn: () -> Unit = {},
    onSignUpSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { AuthViewModel(context) }
    val registerState by viewModel.registerState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Password validation states
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // Handle registration state changes
    LaunchedEffect(registerState) {
        when (val state = registerState) {
            is RegisterState.Success -> {
                onSignUpSuccess()
            }
            is RegisterState.Error -> {
                errorMessage = state.message
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.raw.mmi_logo),
                contentDescription = "MMI Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Header
        Text(
            text = "DAFTAR",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Form fields in a column with weight to fill available space
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Full Name field - compact
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nama Lengkap", fontSize = 12.sp) },
                placeholder = { Text("Nama lengkap", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )

            // Email field - compact
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", fontSize = 12.sp) },
                placeholder = { Text("contoh@email.com", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )

            // Phone Number field - compact
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Nomor Telepon", fontSize = 12.sp) },
                placeholder = { Text("08xxxxxxxxxx", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )

            // Password field - compact with validation
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = when {
                            it.isEmpty() -> null
                            it.length < 6 -> "Kata sandi minimal 6 karakter"
                            else -> null
                        }
                        // Re-validate confirm password if it's not empty
                        if (confirmPassword.isNotEmpty()) {
                            confirmPasswordError = if (confirmPassword != it) {
                                "Konfirmasi kata sandi tidak cocok"
                            } else null
                        }
                    },
                    label = { Text("Kata Sandi", fontSize = 12.sp) },
                    placeholder = { Text("Minimal 6 karakter", fontSize = 12.sp) },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "Hide" else "Show", fontSize = 10.sp)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { 
                        { Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                    } ?: {
                        Text("Gunakan minimal 6 karakter", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
            }

            // Confirm Password field - compact with validation
            Column {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        confirmPasswordError = when {
                            it.isEmpty() -> null
                            it != password -> "Konfirmasi kata sandi tidak cocok"
                            else -> null
                        }
                    },
                    label = { Text("Konfirmasi Kata Sandi", fontSize = 12.sp) },
                    placeholder = { Text("Konfirmasi kata sandi", fontSize = 12.sp) },
                    trailingIcon = {
                        TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Text(if (confirmPasswordVisible) "Hide" else "Show", fontSize = 10.sp)
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    isError = confirmPasswordError != null,
                    supportingText = confirmPasswordError?.let { 
                        { Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                    }
                )
            }
        }

        // Bottom section - compact
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Daftar Button
            Button(
                onClick = {
                    errorMessage = null
                    
                    // Validate all fields
                    when {
                        fullName.isBlank() || email.isBlank() || phoneNumber.isBlank() || 
                        password.isBlank() || confirmPassword.isBlank() -> {
                            errorMessage = "Semua field harus diisi"
                        }
                        password.length < 6 -> {
                            errorMessage = "Kata sandi minimal 6 karakter"
                        }
                        password != confirmPassword -> {
                            errorMessage = "Konfirmasi kata sandi tidak cocok"
                        }
                        passwordError != null || confirmPasswordError != null -> {
                            errorMessage = "Mohon perbaiki kesalahan pada form"
                        }
                        else -> {
                            viewModel.register(fullName, email, phoneNumber, password, confirmPassword)
                        }
                    }
                },
                enabled = registerState !is RegisterState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (registerState is RegisterState.Loading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Mendaftar...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Text(
                        text = "Daftar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Sign In link
            Text(
                text = "Sudah Punya Akun? Masuk",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToSignIn() }
            )

            // Disclaimer box - compact
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = "Dengan mendaftar, Anda menyetujui Syarat dan Ketentuan serta Kebijakan Privasi kami.",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Success Dialog
    if (registerState is RegisterState.Success) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.resetRegisterState()
                onNavigateToSignIn()
            },
            title = {
                Text(
                    text = "Registrasi Berhasil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Akun Anda telah berhasil dibuat. Silakan masuk dengan email dan password yang telah Anda daftarkan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetRegisterState()
                        onNavigateToSignIn()
                    }
                ) {
                    Text("Masuk Sekarang")
                }
            }
        )
    }
}

@Preview
@Composable
private fun SignUpPreview() {
    MaterialTheme {
        SignUpScreen(onBack = {}, onNavigateToSignIn = {}, onSignUpSuccess = {})
    }
}