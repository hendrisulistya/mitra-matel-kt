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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

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

    private var searchJob: Job? = null
    private var lastInputSource: String = ""
    private var lastSearchText: String = ""
    private var searchRequestId = 0

    fun updateSearchText(text: String, source: String = "keyboard") {
        // Update UI immediately - no delay for text display
        _uiState.value = _uiState.value.copy(searchText = text)
        
        // Instant search logic - no debouncing for gRPC
        when {
            text.isBlank() -> {
                // Clear immediately for empty text
                searchJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    results = emptyList(),
                    error = null
                )
                lastSearchText = ""
            }
            text == lastSearchText -> {
                // Skip duplicate searches
                return
            }
            text.length >= 1 -> {
                // Direct immediate search - launch coroutine for suspend function
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    performSearchInternal()
                }
            }
        }
        
        lastSearchText = text
        lastInputSource = source
    }

    fun updateSearchType(type: String) {
        _uiState.value = _uiState.value.copy(searchType = type)
    }

    // Public non-suspend function for external calls
    fun performSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearchInternal()
        }
    }

    // Private suspend function for internal use
    private suspend fun performSearchInternal() {
        val currentState = _uiState.value
        if (currentState.searchText.isBlank()) return

        val currentRequestId = ++searchRequestId
        val startTime = System.currentTimeMillis()
        
        // ✅ CHECK CONNECTION STATUS: Provide user feedback if not ready
        val connectionStatus = currentState.grpcConnectionStatus
        if (connectionStatus != null && !connectionStatus.isHealthy) {
            _uiState.value = currentState.copy(
                error = "Connecting to server...",
                results = emptyList()
            )
            // Add small delay to show connecting message
            delay(100)
        }
        
        try {
            // ✅ UNARY: Now using fast unary gRPC method with connection readiness check
            val results = grpcService.searchVehicle(
                searchType = currentState.searchType,
                searchValue = currentState.searchText
            )
            
            val duration = System.currentTimeMillis() - startTime
            
            // Only update if this is still the latest request and text hasn't changed
            if (currentRequestId == searchRequestId && 
                _uiState.value.searchText == currentState.searchText) {
                _uiState.value = currentState.copy(
                    results = results,
                    error = null,
                    searchDurationMs = duration
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            if (currentRequestId == searchRequestId && 
                _uiState.value.searchText == currentState.searchText) {
                _uiState.value = currentState.copy(
                    results = emptyList(),
                    error = e.message ?: "Search failed",
                    searchDurationMs = duration
                )
            }
        }
    }

    fun clearResults() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            searchText = "",
            results = emptyList(),
            error = null,
            searchDurationMs = null
        )
        lastSearchText = ""
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}