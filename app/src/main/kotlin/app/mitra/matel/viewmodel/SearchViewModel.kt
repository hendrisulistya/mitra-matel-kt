package app.mitra.matel.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mitra.matel.network.GrpcService
import app.mitra.matel.network.VehicleResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoading: Boolean = false,
    val results: List<VehicleResult> = emptyList(),
    val error: String? = null,
    val searchText: String = "",
    val searchType: String = "nopol"
)

class SearchViewModel(
    private val context: Context
) : ViewModel() {
    
    private val grpcService = GrpcService(context)
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun updateSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
    }

    fun updateSearchType(type: String) {
        _uiState.value = _uiState.value.copy(searchType = type)
    }

    fun performSearch() {
        val currentState = _uiState.value
        if (currentState.searchText.isBlank()) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            grpcService.searchVehicle(
                searchType = currentState.searchType,
                searchValue = currentState.searchText
            ).fold(
                onSuccess = { results ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        results = results,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Search failed"
                    )
                }
            )
        }
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            results = emptyList(),
            error = null,
            searchText = ""
        )
    }

    override fun onCleared() {
        super.onCleared()
        grpcService.close()
    }
}