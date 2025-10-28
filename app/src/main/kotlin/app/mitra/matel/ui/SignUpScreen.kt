package app.mitra.matel.ui

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
        // Header
        Text(
            text = "DAFTAR",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Form fields in a scrollable column with weight to fill available space
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Full Name field - compact
            OutlinedTextField(
                value = fullName,
                onValueChange = { 
                    fullName = it
                    fullNameError = false // Clear error when user starts typing
                },
                label = { Text("Nama Lengkap", fontSize = 12.sp) },
                placeholder = { Text("Nama lengkap", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                isError = fullNameError,
                supportingText = if (fullNameError) {
                    { Text("* Nama lengkap harus diisi", fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                } else null
            )

            // Email field - compact with validation
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
                label = { Text("Email", fontSize = 12.sp) },
                placeholder = { Text("contoh@gmail.com", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                isError = emailError != null || emailEmptyError,
                supportingText = when {
                    emailEmptyError -> {
                        { Text("* Email harus diisi", fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                    }
                    emailError != null -> {
                        { Text(emailError!!, fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                    }
                    else -> {
                        { Text("Gunakan email dari Gmail, Yahoo, Outlook, dll", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            )

            // Phone Number field - compact
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { 
                    phoneNumber = it
                    phoneNumberError = false // Clear error when user starts typing
                },
                label = { Text("Nomor Telepon", fontSize = 12.sp) },
                placeholder = { Text("08xxxxxxxxxx", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                isError = phoneNumberError,
                supportingText = if (phoneNumberError) {
                    { Text("* Nomor telepon harus diisi", fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                } else null
            )

            // Password field - compact with validation
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordEmptyError = false // Clear empty error when user starts typing
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
                isError = passwordError != null || passwordEmptyError,
                supportingText = when {
                    passwordEmptyError -> {
                        { Text("* Kata sandi harus diisi", fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                    }
                    passwordError != null -> {
                        { Text(passwordError!!, fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                    }
                    else -> {
                        { Text("Gunakan minimal 6 karakter", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            )

            // Confirm Password field - compact with validation
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
                isError = confirmPasswordError != null || confirmPasswordEmptyError,
                supportingText = when {
                    confirmPasswordEmptyError -> {
                        { Text("* Konfirmasi kata sandi harus diisi", fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                    }
                    confirmPasswordError != null -> {
                        { Text(confirmPasswordError!!, fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                    }
                    else -> null
                }
            )
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

            // Agreement checkbox
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                
                // Error message for agreement checkbox
                if (agreementError) {
                    Text(
                        text = "* Anda harus menyetujui syarat dan ketentuan serta kebijakan privasi",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 40.dp, top = 4.dp)
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
                    
                    // Check for empty fields and highlight them
                    var hasEmptyFields = false
                    
                    if (fullName.isBlank()) {
                        fullNameError = true
                        hasEmptyFields = true
                    }
                    
                    if (phoneNumber.isBlank()) {
                        phoneNumberError = true
                        hasEmptyFields = true
                    }
                    
                    if (email.isBlank()) {
                        emailEmptyError = true
                        hasEmptyFields = true
                    }
                    
                    if (password.isBlank()) {
                        passwordEmptyError = true
                        hasEmptyFields = true
                    }
                    
                    if (confirmPassword.isBlank()) {
                        confirmPasswordEmptyError = true
                        hasEmptyFields = true
                    }
                    
                    if (!isAgreementChecked) {
                        agreementError = true
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

            // Sign In link
            Text(
                text = "Sudah Punya Akun? Masuk",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToSignIn() }
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