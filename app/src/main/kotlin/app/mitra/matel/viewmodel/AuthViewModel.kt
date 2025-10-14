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
import app.mitra.matel.network.models.DeviceConflictException
import app.mitra.matel.network.models.DeviceConflictResponse

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val response: LoginResponse) : AuthState()
    data class Error(val message: String) : AuthState()
    data class Conflict(val data: DeviceConflictResponse) : AuthState()
}

class AuthViewModel(private val context: Context) : ViewModel() {

    private val apiService = ApiService(context = context)
    private val sessionManager = SessionManager(context)

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

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
                if (exception is DeviceConflictException) {
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
     * Logout - clear session
     */
    fun logout() {
        sessionManager.clearSession()
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

    fun forceLogin(email: String, password: String, rememberCredentials: Boolean = true) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading

            val result = apiService.forceLogin(email, password)

            result.onSuccess { response ->
                sessionManager.saveToken(response.token)
                if (rememberCredentials) {
                    sessionManager.saveCredentials(email, password)
                }
                _loginState.value = AuthState.Success(response)
            }.onFailure { exception ->
                _loginState.value = AuthState.Error(
                    exception.message ?: "Force login failed"
                )
            }
        }
    }
}
