package org.peek.app.ui.history

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.peek.app.data.repository.RepositoryFactory
import org.peek.app.domain.model.HistoryEntry

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryFactory.historyRepository(application)
    private val _state = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeHistory()
                .catch { error ->
                    Log.e(TAG, "Could not read local history", error)
                    _state.value = HistoryState.Failed
                }
                .collect { entries ->
                    _state.value = HistoryState.Ready(entries)
                }
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            runCatching { repository.remove(id) }
                .onFailure { Log.e(TAG, "Could not remove local history entry") }
        }
    }

    fun clear() {
        viewModelScope.launch {
            runCatching { repository.clear() }
                .onFailure { Log.e(TAG, "Could not clear local history") }
        }
    }

    private companion object {
        const val TAG = "HistoryViewModel"
    }
}

sealed interface HistoryState {
    data object Loading : HistoryState

    data class Ready(val entries: List<HistoryEntry>) : HistoryState

    data object Failed : HistoryState
}
