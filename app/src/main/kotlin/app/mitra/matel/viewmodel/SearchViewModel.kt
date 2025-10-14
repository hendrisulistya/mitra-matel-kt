package app.mitra.matel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mitra.matel.network.GrpcService
import app.mitra.matel.network.VehicleResult
import app.mitra.matel.network.GrpcConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val searchText: String = "",
    val searchType: String = "nopol",
    val results: List<VehicleResult> = emptyList(),
    val error: String? = null,
    val searchDurationMs: Long? = null,
    val grpcConnectionStatus: GrpcConnectionStatus? = null
)

class SearchViewModel(private val grpcService: GrpcService) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Collect gRPC connection status
        viewModelScope.launch {
            grpcService.healthService.connectionStatus.collect { status ->
                _uiState.value = _uiState.value.copy(grpcConnectionStatus = status)
            }
        }
    }

    fun updateSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
    }

    fun updateSearchType(type: String) {
        _uiState.value = _uiState.value.copy(searchType = type)
    }

    fun performSearch() {
        val currentState = _uiState.value
        if (currentState.searchText.isBlank()) return

        val startTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            try {
                val results = grpcService.searchVehicle(
                    searchType = currentState.searchType,
                    searchValue = currentState.searchText
                )
                
                val duration = System.currentTimeMillis() - startTime
                
                // SINGLE state update to minimize recomposition
                _uiState.value = currentState.copy(
                    results = results,
                    error = null,
                    searchDurationMs = duration
                )
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                _uiState.value = currentState.copy(
                    results = emptyList(),
                    error = e.message ?: "Search failed",
                    searchDurationMs = duration
                )
            }
        }
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            results = emptyList(),
            error = null,
            searchText = "",
            searchDurationMs = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        grpcService.close()
    }
}