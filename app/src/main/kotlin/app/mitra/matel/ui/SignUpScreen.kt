package app.mitra.matel.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mitra.matel.R
import app.mitra.matel.ui.screens.TermOfServiceContent
import app.mitra.matel.ui.screens.PrivacyPolicyContent
import app.mitra.matel.viewmodel.AuthViewModel
import app.mitra.matel.viewmodel.RegisterState

// Email validation functions
private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun isAllowedEmailDomain(email: String): Boolean {
    if (!isValidEmail(email)) return false
    
    val allowedDomains = setOf(
        "gmail.com", "googlemail.com",
        "yahoo.com", "yahoo.co.id", "ymail.com",
        "outlook.com", "hotmail.com", "live.com",
        "icloud.com", "me.com",
        "protonmail.com", "proton.me",
        "aol.com",
        "mail.com",
        "zoho.com"
    )
    
    val domain = email.substringAfter("@").lowercase()
    return allowedDomains.contains(domain)
}

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
    
    // Email validation state
    var emailError by remember { mutableStateOf<String?>(null) }
    
    // Field validation states for highlighting empty fields
    var fullNameError by remember { mutableStateOf(false) }
    var phoneNumberError by remember { mutableStateOf(false) }
    var emailEmptyError by remember { mutableStateOf(false) }
    var passwordEmptyError by remember { mutableStateOf(false) }
    var confirmPasswordEmptyError by remember { mutableStateOf(false) }
    
    // Agreement checkbox state
    var isAgreementChecked by remember { mutableStateOf(false) }
    var agreementError by remember { mutableStateOf(false) }
    
    // Modal dialog states
    var showTermsModal by remember { mutableStateOf(false) }
    var showPrivacyModal by remember { mutableStateOf(false) }
    
    // Shake animation states
    var shakeFullName by remember { mutableStateOf(false) }
    var shakeEmail by remember { mutableStateOf(false) }
    var shakePhoneNumber by remember { mutableStateOf(false) }
    var shakePassword by remember { mutableStateOf(false) }
    var shakeConfirmPassword by remember { mutableStateOf(false) }
    var shakeAgreement by remember { mutableStateOf(false) }
    
    // Shake animation values
    val shakeFullNameOffset by animateFloatAsState(
        targetValue = if (shakeFullName) 1f else 0f,
        animationSpec = if (shakeFullName) {
            keyframes {
                durationMillis = 500
                0f at 0
                -10f at 50
                10f at 100
                -8f at 150
                8f at 200
                -5f at 250
                5f at 300
                -2f at 350
                2f at 400
                0f at 500
            }
        } else {
            tween(0)
        },
        finishedListener = { shakeFullName = false }
    )
    
    val shakeEmailOffset by animateFloatAsState(
        targetValue = if (shakeEmail) 1f else 0f,
        animationSpec = if (shakeEmail) {
            keyframes {
                durationMillis = 500
                0f at 0
                -10f at 50
                10f at 100
                -8f at 150
                8f at 200
                -5f at 250
                5f at 300
                -2f at 350
                2f at 400
                0f at 500
            }
        } else {
            tween(0)
        },
        finishedListener = { shakeEmail = false }
    )
    
    val shakePhoneNumberOffset by animateFloatAsState(
        targetValue = if (shakePhoneNumber) 1f else 0f,
        animationSpec = if (shakePhoneNumber) {
            keyframes {
                durationMillis = 500
                0f at 0
                -10f at 50
                10f at 100
                -8f at 150
                8f at 200
                -5f at 250
                5f at 300
                -2f at 350
                2f at 400
                0f at 500
            }
        } else {
            tween(0)
        },
        finishedListener = { shakePhoneNumber = false }
    )
    
    val shakePasswordOffset by animateFloatAsState(
        targetValue = if (shakePassword) 1f else 0f,
        animationSpec = if (shakePassword) {
            keyframes {
                durationMillis = 500
                0f at 0
                -10f at 50
                10f at 100
                -8f at 150
                8f at 200
                -5f at 250
                5f at 300
                -2f at 350
                2f at 400
                0f at 500
            }
        } else {
            tween(0)
        },
        finishedListener = { shakePassword = false }
    )
    
    val shakeConfirmPasswordOffset by animateFloatAsState(
        targetValue = if (shakeConfirmPassword) 1f else 0f,
        animationSpec = if (shakeConfirmPassword) {
            keyframes {
                durationMillis = 500
                0f at 0
                -10f at 50
                10f at 100
                -8f at 150
                8f at 200
                -5f at 250
                5f at 300
                -2f at 350
                2f at 400
                0f at 500
            }
        } else {
            tween(0)
        },
        finishedListener = { shakeConfirmPassword = false }
    )
    
    val shakeAgreementOffset by animateFloatAsState(
        targetValue = if (shakeAgreement) 1f else 0f,
        animationSpec = if (shakeAgreement) {
            keyframes {
                durationMillis = 500
                0f at 0
                -10f at 50
                10f at 100
                -8f at 150
                8f at 200
                -5f at 250
                5f at 300
                -2f at 350
                2f at 400
                0f at 500
            }
        } else {
            tween(0)
        },
        finishedListener = { shakeAgreement = false }
    )

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
        // Header section with proportional spacing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.15f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DAFTAR",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Form fields in a scrollable column with proportional weight
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Full Name field - inline label with error messages as placeholder
            OutlinedTextField(
                value = fullName,
                onValueChange = { 
                    fullName = it
                    fullNameError = false // Clear error when user starts typing
                },
                label = { Text("Nama Lengkap", fontSize = 11.sp) },
                placeholder = { 
                    Text(
                        if (fullNameError) "* Nama lengkap harus diisi" else "Nama lengkap",
                        fontSize = 11.sp,
                        color = if (fullNameError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakeFullNameOffset * 10f
                    },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                isError = fullNameError
            )

            // Email field - inline label with error messages below field
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakeEmailOffset * 10f
                    }
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailEmptyError = false // Clear empty error when user starts typing
                        emailError = when {
                            it.isEmpty() -> null
                            !isValidEmail(it) -> "Format email tidak valid"
                            !isAllowedEmailDomain(it) -> "Hanya menerima email dari Gmail, Yahoo, Outlook, dan domain umum lainnya"
                            else -> null
                        }
                    },
                    label = { Text("Email", fontSize = 11.sp) },
                    placeholder = { 
                        Text(
                            if (emailEmptyError) "* Email harus diisi" else "contoh@gmail.com",
                            fontSize = 11.sp,
                            color = if (emailEmptyError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    isError = emailError != null || emailEmptyError
                )
                
                // Error message below field for email validation errors
                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Phone Number field - inline label with error messages as placeholder
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { 
                    phoneNumber = it
                    phoneNumberError = false // Clear error when user starts typing
                },
                label = { Text("Nomor Telepon", fontSize = 11.sp) },
                placeholder = { 
                    Text(
                        if (phoneNumberError) "* Nomor telepon harus diisi" else "08xxxxxxxxxx",
                        fontSize = 11.sp,
                        color = if (phoneNumberError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakePhoneNumberOffset * 10f
                    },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                isError = phoneNumberError
            )

            // Password field - inline label with error messages as placeholder
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordEmptyError = false // Clear error when user starts typing
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
                label = { Text("Kata Sandi", fontSize = 11.sp) },
                placeholder = { 
                    Text(
                        when {
                            passwordEmptyError -> "* Kata sandi harus diisi"
                            passwordError != null -> passwordError!!
                            else -> "Minimal 6 karakter"
                        },
                        fontSize = 11.sp,
                        color = when {
                            passwordEmptyError || passwordError != null -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Hide" else "Show", fontSize = 9.sp)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakePasswordOffset * 10f
                    },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                isError = passwordError != null || passwordEmptyError
            )

            // Confirm Password field - inline label with error messages as placeholder
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    confirmPasswordEmptyError = false // Clear empty error when user starts typing
                    confirmPasswordError = when {
                        it.isEmpty() -> null
                        it != password -> "Konfirmasi kata sandi tidak cocok"
                        else -> null
                    }
                },
                label = { Text("Konfirmasi Kata Sandi", fontSize = 11.sp) },
                placeholder = { 
                    Text(
                        when {
                            confirmPasswordEmptyError -> "* Konfirmasi kata sandi harus diisi"
                            confirmPasswordError != null -> confirmPasswordError!!
                            else -> "Konfirmasi kata sandi"
                        },
                        fontSize = 11.sp,
                        color = when {
                            confirmPasswordEmptyError || confirmPasswordError != null -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                trailingIcon = {
                    TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Text(if (confirmPasswordVisible) "Hide" else "Show", fontSize = 9.sp)
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakeConfirmPasswordOffset * 10f
                    },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                isError = confirmPasswordError != null || confirmPasswordEmptyError
            )
        }

        // Bottom section with increased weight for visibility
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.25f),
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

            // Agreement checkbox
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = shakeAgreementOffset * 10f
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = isAgreementChecked,
                        onCheckedChange = { 
                            isAgreementChecked = it
                            agreementError = false // Clear error when checked
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = CheckboxDefaults.colors(
                            uncheckedColor = if (agreementError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    val annotatedText = buildAnnotatedString {
                        append("Dengan ini saya bersedia mematuhi ")
                        
                        pushStringAnnotation(tag = "terms", annotation = "terms")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("syarat dan ketentuan")
                        }
                        pop()
                        
                        append(" serta ")
                        
                        pushStringAnnotation(tag = "privacy", annotation = "privacy")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("kebijakan privasi")
                        }
                        pop()
                    }
                    
                    ClickableText(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(tag = "terms", start = offset, end = offset)
                                .firstOrNull()?.let {
                                    showTermsModal = true
                                }
                            annotatedText.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                                .firstOrNull()?.let {
                                    showPrivacyModal = true
                                }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daftar Button
            Button(
                onClick = {
                    errorMessage = null
                    
                    // Reset all field errors
                    fullNameError = false
                    phoneNumberError = false
                    emailEmptyError = false
                    passwordEmptyError = false
                    confirmPasswordEmptyError = false
                    agreementError = false
                    
                    // Check for empty fields and highlight them with shake animation
                    var hasEmptyFields = false
                    
                    if (fullName.isBlank()) {
                        fullNameError = true
                        shakeFullName = true
                        hasEmptyFields = true
                    }
                    
                    if (phoneNumber.isBlank()) {
                        phoneNumberError = true
                        shakePhoneNumber = true
                        hasEmptyFields = true
                    }
                    
                    if (email.isBlank()) {
                        emailEmptyError = true
                        shakeEmail = true
                        hasEmptyFields = true
                    }
                    
                    if (password.isBlank()) {
                        passwordEmptyError = true
                        shakePassword = true
                        hasEmptyFields = true
                    }
                    
                    if (confirmPassword.isBlank()) {
                        confirmPasswordEmptyError = true
                        shakeConfirmPassword = true
                        hasEmptyFields = true
                    }
                    
                    if (!isAgreementChecked) {
                        agreementError = true
                        shakeAgreement = true
                        hasEmptyFields = true
                    }
                    
                    // Check for validation errors
                    val hasValidationErrors = emailError != null || passwordError != null || confirmPasswordError != null
                    
                    if (hasEmptyFields || hasValidationErrors) {
                        // Don't proceed if any field has errors
                        return@Button
                    } else {
                        // All validations passed
                        viewModel.register(fullName, email, phoneNumber, password, confirmPassword)
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

            // Sign In link - positioned below Daftar button
            Text(
                text = "Sudah Punya Akun? Masuk",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToSignIn() }
            )

            // Contact admin link
            Text(
                text = "Kendala untuk mendaftar? Hubungi admin",
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = TextDecoration.Underline
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val message = "Saya kesulitan untuk mendaftar, mohon Bantuan nya"
                    val adminPhone = "6281936706368"
                    openAdminWhatsApp(context, adminPhone, message)
                }
            )
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

    // Terms of Service Modal
    if (showTermsModal) {
        AlertDialog(
            onDismissRequest = { showTermsModal = false },
            title = {
                Text(
                    text = "Syarat dan Ketentuan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    TermOfServiceContent()
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsModal = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Privacy Policy Modal
    if (showPrivacyModal) {
        AlertDialog(
            onDismissRequest = { showPrivacyModal = false },
            title = {
                Text(
                    text = "Kebijakan Privasi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    PrivacyPolicyContent()
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyModal = false }) {
                    Text("Tutup")
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