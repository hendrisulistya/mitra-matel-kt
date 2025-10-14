# API Integration Guide

## Setup Complete ✅

### 1. Configuration
Edit `ApiConfig.kt` to toggle between environments:
```kotlin
private const val isProduction = false  // Change to true for production
```

- **Local**: `http://10.0.2.2:3000` (Android Emulator)
- **Production**: `https://api.mitra-matel.com`

### 2. Usage in SignInScreen

```kotlin
@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { AuthViewModel(context) }
    val loginState by viewModel.loginState.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Handle login state
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is AuthState.Success -> {
                // Token is automatically saved
                onSignInSuccess()
            }
            is AuthState.Error -> {
                errorMessage = state.message
            }
            else -> {}
        }
    }
    
    Column {
        // ... your UI fields ...
        
        // Show error message
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Sign In Button
        Button(
            onClick = {
                errorMessage = null
                viewModel.login(email, password)
            },
            enabled = loginState !is AuthState.Loading
        ) {
            if (loginState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign In")
            }
        }
    }
}
```

### 3. Device Information
Device ID and Model are automatically extracted:
- **Device ID**: Uses Android Secure ID (unique per app installation)
- **Model**: Combines manufacturer + model (e.g., "Samsung SM-G991B")

### 4. API Request Example
```json
POST http://10.0.2.2:3000/public/auth/user/login
Content-Type: application/json

{
    "email": "test@user.com",
    "password": "testtest",
    "device": {
        "device_id": "550e8400-e29b-41d4-a716-446655440110",
        "model": "Samsung SM-G991B"
    }
}
```

### 5. API Response Example
```json
{
  "token": "eyJhbGc......"
}
```

Token is automatically saved and added to all subsequent requests via:
```kotlin
header("Authorization", "Bearer $token")
```

### 6. Testing on Real Device
If using a real device (not emulator), update `ApiConfig.kt`:
```kotlin
private const val LOCAL_BASE_URL = "http://192.168.1.100:3000" // Your computer's local IP
```

### 7. Dependencies Added
- Ktor Client (Android engine)
- Content Negotiation (JSON)
- Kotlinx Serialization
- Logging

All HTTP requests are logged in Logcat with tag "HTTP Client".
