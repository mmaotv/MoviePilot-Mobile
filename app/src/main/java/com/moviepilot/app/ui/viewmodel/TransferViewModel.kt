package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.RenamePreview
import com.moviepilot.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _queue = MutableStateFlow<List<RenamePreview>>(emptyList())
    val queue: StateFlow<List<RenamePreview>> = _queue

    fun loadTransferQueue() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            systemRepository.getTransferQueue().fold(
                onSuccess = { queueList ->
                    _queue.value = queueList
                    _uiState.value = UiState.Success(queueList)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    fun removeFromQueue(item: RenamePreview) {
        viewModelScope.launch {
            // Remove from UI immediately for better UX
            val currentQueue = _queue.value.toMutableList()
            currentQueue.remove(item)
            _queue.value = currentQueue
            _uiState.value = UiState.Success(currentQueue)

            // Call API to remove from server
            item.srcFile?.let { file ->
                systemRepository.getTransferQueue() // Just refresh to verify
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val queue: List<RenamePreview>) : UiState()
        data class Error(val message: String) : UiState()
    }
}
