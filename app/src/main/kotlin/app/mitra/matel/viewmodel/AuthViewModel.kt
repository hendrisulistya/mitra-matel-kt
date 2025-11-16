package app.mitra.matel.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mitra.matel.network.ApiService
import app.mitra.matel.network.models.LoginResponse
import app.mitra.matel.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.mitra.matel.network.models.LoginConflictException
import app.mitra.matel.network.models.LoginConflictResponse
import app.mitra.matel.network.models.RegisterResponse

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val response: LoginResponse) : AuthState()
    data class Error(val message: String) : AuthState()
    data class Conflict(val data: LoginConflictResponse) : AuthState()
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val response: RegisterResponse) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class AuthViewModel(private val context: Context) : ViewModel() {

    private val apiService = ApiService(context = context)
    private val sessionManager = SessionManager.getInstance(context)

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    /**
     * Check if user is already logged in
     */
    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    /**
     * Login with email and password
     */
    fun login(email: String, password: String, rememberCredentials: Boolean = true) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading

            val result = apiService.login(email, password)

            result.onSuccess { response ->
                sessionManager.saveToken(response.token)
                if (rememberCredentials) {
                    sessionManager.saveCredentials(email, password)
                }
                _loginState.value = AuthState.Success(response)
            }.onFailure { exception ->
                if (exception is LoginConflictException) {
                    _loginState.value = AuthState.Conflict(exception.data)
                } else {
                    _loginState.value = AuthState.Error(
                        exception.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    /**
     * Logout - call API, then clear session
     */
    fun logout(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = apiService.logout()
            result.onSuccess {
                sessionManager.clearSession()
                onResult(true, null)
            }.onFailure { e ->
                sessionManager.clearSession()
                onResult(false, e.message)
            }
        }
    }

    /**
     * Get saved credentials for auto-login
     */
    fun getSavedCredentials(): Pair<String?, String?> {
        return Pair(sessionManager.getEmail(), sessionManager.getPassword())
    }

    fun resetState() {
        _loginState.value = AuthState.Idle
    }



    /**
     * Register new user
     */
    fun register(
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            // Validate inputs
            if (fullName.isBlank()) {
                _registerState.value = RegisterState.Error("Nama lengkap tidak boleh kosong")
                return@launch
            }
            
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _registerState.value = RegisterState.Error("Email tidak valid")
                return@launch
            }
            
            if (phoneNumber.isBlank()) {
                _registerState.value = RegisterState.Error("Nomor telepon tidak boleh kosong")
                return@launch
            }
            
            if (password.length < 6) {
                _registerState.value = RegisterState.Error("Password minimal 6 karakter")
                return@launch
            }
            
            if (password != confirmPassword) {
                _registerState.value = RegisterState.Error("Password dan konfirmasi password tidak sama")
                return@launch
            }

            _registerState.value = RegisterState.Loading

            val result = apiService.registerUser(fullName, email, phoneNumber, password)

            result.onSuccess { response ->
                _registerState.value = RegisterState.Success(response)
            }.onFailure { exception ->
                _registerState.value = RegisterState.Error(
                    exception.message ?: "Registrasi gagal"
                )
            }
        }
    }

    /**
     * Reset register state
     */
    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }
}
