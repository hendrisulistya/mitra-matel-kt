package app.mitra.matel.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.mitra.matel.network.ApiService
import app.mitra.matel.network.models.AnnouncementResponse
import app.mitra.matel.network.models.AnnouncementLatestResponse
import app.mitra.matel.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AnnouncementState {
    object Loading : AnnouncementState()
    data class Success(val items: List<AnnouncementResponse>, val index: Int) : AnnouncementState()
    data class Error(val message: String) : AnnouncementState()
}

class AnnouncementViewModel(context: Context) : ViewModel() {
    private val api = ApiService(context)
    private val ctx: Context = context
    private val _state = MutableStateFlow<AnnouncementState>(AnnouncementState.Loading)
    val state: StateFlow<AnnouncementState> = _state

    fun fetchAnnouncement() {
        viewModelScope.launch {
            _state.value = AnnouncementState.Loading
            api.getAnnouncement().fold(
                onSuccess = { bundle: AnnouncementLatestResponse ->
                    val allItems = listOfNotNull(bundle.promo, bundle.policy, bundle.update)
                    val dismissed = SessionManager.getInstance(ctx).getDismissedAnnouncementIds()
                    val items = allItems.filter { it.id !in dismissed }
                    if (items.isEmpty()) {
                        _state.value = AnnouncementState.Error("Tidak ada pengumuman")
                    } else {
                        _state.value = AnnouncementState.Success(items, 0)
                    }
                },
                onFailure = { _state.value = AnnouncementState.Error(it.message ?: "Gagal memuat pengumuman") }
            )
        }
    }

    fun next() {
        val s = _state.value
        if (s is AnnouncementState.Success) {
            val nextIndex = s.index + 1
            if (nextIndex < s.items.size) {
                _state.value = AnnouncementState.Success(s.items, nextIndex)
            } else {
                _state.value = AnnouncementState.Error("Selesai")
            }
        }
    }
}