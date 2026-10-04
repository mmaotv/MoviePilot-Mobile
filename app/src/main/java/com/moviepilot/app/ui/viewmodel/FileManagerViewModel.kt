package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.FileItem
import com.moviepilot.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath

    private val _currentStorage = MutableStateFlow("local")
    private val storageName = "local"

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            systemRepository.listFiles(path, storageName).fold(
                onSuccess = { content ->
                    _files.value = content.files.sortedWith(
                        compareBy({ it.type != "folder" }, { it.name?.lowercase() })
                    )
                    _currentPath.value = path
                    _uiState.value = UiState.Success(content.files)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    fun navigateTo(path: String) {
        loadFiles(path)
    }

    fun navigateUp() {
        val current = _currentPath.value
        if (current.isNotEmpty()) {
            val parentPath = current.substringBeforeLast("/", "")
            loadFiles(parentPath)
        }
    }

    fun refresh() {
        loadFiles(_currentPath.value)
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val currentDir = if (_currentPath.value.isEmpty()) "" else _currentPath.value + "/"
            systemRepository.createDirectory(name, currentDir + name).fold(
                onSuccess = {
                    refresh()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "创建失败")
                }
            )
        }
    }

    fun renameFile(file: FileItem, newName: String) {
        viewModelScope.launch {
            systemRepository.renameFile(file.path ?: "", file.name ?: "", newName).fold(
                onSuccess = {
                    refresh()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "重命名失败")
                }
            )
        }
    }

    fun deleteFile(file: FileItem) {
        viewModelScope.launch {
            systemRepository.deleteFile(file.path ?: "", file.name ?: "").fold(
                onSuccess = {
                    refresh()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "删除失败")
                }
            )
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val files: List<FileItem>) : UiState()
        data class Error(val message: String) : UiState()
    }
}
