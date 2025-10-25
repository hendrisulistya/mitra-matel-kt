package app.mitra.matel.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mitra.matel.network.models.MyVehicleDataItem
import app.mitra.matel.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class VehicleDataState {
    object Idle : VehicleDataState()
    object Loading : VehicleDataState()
    data class Success(val vehicles: List<MyVehicleDataItem>) : VehicleDataState()
    data class Error(val message: String) : VehicleDataState()
    data class AuthenticationError(val message: String) : VehicleDataState()
    data class ServerError(val message: String) : VehicleDataState()
}

class VehicleViewModel(private val context: Context) : ViewModel() {

    private val apiService = ApiService(context = context)

    private val _vehicleDataState = MutableStateFlow<VehicleDataState>(VehicleDataState.Idle)
    val vehicleDataState: StateFlow<VehicleDataState> = _vehicleDataState.asStateFlow()

    private val _vehicles = MutableStateFlow<List<MyVehicleDataItem>>(emptyList())
    val vehicles: StateFlow<List<MyVehicleDataItem>> = _vehicles.asStateFlow()

    /**
     * Fetch vehicle data from API
     */
    fun fetchVehicleData() {
        viewModelScope.launch {
            _vehicleDataState.value = VehicleDataState.Loading
            
            apiService.getMyVehicleData().fold(
                onSuccess = { vehicleData ->
                    _vehicles.value = vehicleData
                    _vehicleDataState.value = VehicleDataState.Success(vehicleData)
                },
                onFailure = { exception ->
                    val errorMessage = exception.message ?: "Failed to fetch vehicle data"
                    
                    // Categorize errors based on message content
                    _vehicleDataState.value = when {
                        errorMessage.contains("login again", ignoreCase = true) ||
                        errorMessage.contains("Session expired", ignoreCase = true) ||
                        errorMessage.contains("Authentication error", ignoreCase = true) ||
                        errorMessage.contains("User account not found", ignoreCase = true) -> {
                            VehicleDataState.AuthenticationError(errorMessage)
                        }
                        errorMessage.contains("Server error", ignoreCase = true) ||
                        errorMessage.contains("try again later", ignoreCase = true) -> {
                            VehicleDataState.ServerError(errorMessage)
                        }
                        else -> {
                            VehicleDataState.Error(errorMessage)
                        }
                    }
                }
            )
        }
    }

    /**
     * Reset state to idle
     */
    fun resetState() {
        _vehicleDataState.value = VehicleDataState.Idle
    }

    /**
     * Get current vehicles list
     */
    fun getCurrentVehicles(): List<MyVehicleDataItem> {
        return _vehicles.value
    }
}