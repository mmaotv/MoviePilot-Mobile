package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.DownloadTask
import com.moviepilot.app.data.model.TransferHistory
import com.moviepilot.app.data.model.TransferHistoryWrapper
import com.moviepilot.app.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Loading)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val historyState: StateFlow<HistoryState> = _historyState

    init {
        loadDownloads()
        loadHistory()
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Loading
            try {
                val tasks = downloadRepository.getDownloadTasks()
                _downloadState.value = DownloadState.Success(tasks)
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun loadHistory(page: Int = 1) {
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading
            try {
                val wrapper: TransferHistoryWrapper = downloadRepository.getTransferHistory(page)
                val historyList = wrapper.data?.list ?: emptyList()
                _historyState.value = HistoryState.Success(historyList)
            } catch (e: Exception) {
                _historyState.value = HistoryState.Error(e.message ?: "加载失败")
            }
        }
    }

    sealed class DownloadState {
        object Loading : DownloadState()
        data class Success(val tasks: List<DownloadTask>) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    sealed class HistoryState {
        object Loading : HistoryState()
        data class Success(val history: List<TransferHistory>) : HistoryState()
        data class Error(val message: String) : HistoryState()
    }
}
