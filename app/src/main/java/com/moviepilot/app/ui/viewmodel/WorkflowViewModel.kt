package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.Workflow
import com.moviepilot.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkflowViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _workflows = MutableStateFlow<List<Workflow>>(emptyList())
    val workflows: StateFlow<List<Workflow>> = _workflows

    fun loadWorkflows() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            systemRepository.getWorkflows().fold(
                onSuccess = { workflowList ->
                    _workflows.value = workflowList
                    _uiState.value = UiState.Success
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    fun runWorkflow(workflow: Workflow) {
        viewModelScope.launch {
            workflow.id.toIntOrNull()?.let { id ->
                systemRepository.runWorkflow(id).fold(
                    onSuccess = { result ->
                        // Refresh the list after running
                        loadWorkflows()
                    },
                    onFailure = { error ->
                        _uiState.value = UiState.Error(error.message ?: "运行失败")
                    }
                )
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
