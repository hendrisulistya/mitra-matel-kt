package app.mitra.matel.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mitra.matel.network.models.ProfileResponse
import app.mitra.matel.network.ApiService
import app.mitra.matel.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val profile: ProfileResponse) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(private val context: Context) : ViewModel() {

    private val apiService = ApiService(context = context)
    private val sessionManager = SessionManager(context)

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile: StateFlow<ProfileResponse?> = _profile.asStateFlow()

    init {
        // Load profile from SessionManager on initialization
        loadProfileFromCache()
    }

    /**
     * Load profile from SessionManager cache
     */
    private fun loadProfileFromCache() {
        val cachedProfile = sessionManager.getProfile()
        if (cachedProfile != null) {
            _profile.value = cachedProfile
            _profileState.value = ProfileState.Success(cachedProfile)
        }
    }

    /**
     * Fetch profile from API and save to SessionManager
     */
    fun fetchProfile() {
        // Don't fetch if already loading or if we have fresh data
        if (_profileState.value is ProfileState.Loading) return
        
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            val result = apiService.getProfile()

            result.onSuccess { profileResponse ->
                // Save to SessionManager
                sessionManager.saveProfile(profileResponse)
                
                // Update state
                _profile.value = profileResponse
                _profileState.value = ProfileState.Success(profileResponse)
            }.onFailure { exception ->
                _profileState.value = ProfileState.Error(
                    exception.message ?: "Failed to fetch profile"
                )
            }
        }
    }

    /**
     * Get cached profile without API call
     */
    fun getCachedProfile(): ProfileResponse? {
        return _profile.value ?: sessionManager.getProfile()
    }

    /**
     * Clear profile data
     */
    fun clearProfile() {
        sessionManager.clearProfile()
        _profile.value = null
        _profileState.value = ProfileState.Idle
    }

    /**
     * Reset state to idle
     */
    fun resetState() {
        _profileState.value = ProfileState.Idle
    }
}