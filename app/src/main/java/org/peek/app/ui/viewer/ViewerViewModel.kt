package org.peek.app.ui.viewer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.peek.app.data.repository.RepositoryFactory
import org.peek.app.domain.model.ExtractionError
import org.peek.app.domain.model.ExtractionException
import org.peek.app.domain.model.ExtractionResult

class ViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryFactory.mediaRepository(application)
    private val historyRepository = RepositoryFactory.historyRepository(application)
    private val _state = MutableStateFlow<ViewerState>(ViewerState.Idle)
    val state: StateFlow<ViewerState> = _state.asStateFlow()
    private var extractionJob: Job? = null
    private var recordedResult: ExtractionResult? = null

    fun retry() {
        state.value.sourceUrl.takeIf(String::isNotBlank)?.let(::open)
    }

    fun open(url: String) {
        extractionJob?.cancel()
        recordedResult = null
        extractionJob = viewModelScope.launch {
            _state.value = ViewerState.Loading(url)
            _state.value = try {
                val result = repository.open(url)
                if (result.media.isEmpty()) {
                    throw ExtractionException(ExtractionError.MediaUnavailable)
                }
                ViewerState.Ready(result)
            } catch (error: ExtractionException) {
                ViewerState.Failed(url, error.error)
            }
        }
    }

    fun recordView(result: ExtractionResult) {
        val currentResult = (state.value as? ViewerState.Ready)?.extraction
        if (currentResult != result || recordedResult == result) return
        recordedResult = result
        viewModelScope.launch {
            runCatching { historyRepository.recordView(result) }
                .onFailure {
                    if (recordedResult == result) recordedResult = null
                    Log.e(TAG, "Could not save local history entry")
                }
        }
    }

    fun cancel() {
        extractionJob?.cancel()
        extractionJob = null
        recordedResult = null
        _state.value = ViewerState.Idle
    }

    companion object {
        private const val TAG = "ViewerViewModel"
    }
}

sealed interface ViewerState {
    val sourceUrl: String

    data object Idle : ViewerState {
        override val sourceUrl: String = ""
    }

    data class Loading(override val sourceUrl: String) : ViewerState

    data class Ready(
        val extraction: ExtractionResult,
    ) : ViewerState {
        override val sourceUrl: String = extraction.sourceUrl
    }

    data class Failed(
        override val sourceUrl: String,
        val error: ExtractionError,
    ) : ViewerState
}
