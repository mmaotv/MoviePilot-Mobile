package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.Plugin
import com.moviepilot.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _installedPlugins = MutableStateFlow<List<Plugin>>(emptyList())
    val installedPlugins: StateFlow<List<Plugin>> = _installedPlugins

    private val _marketPlugins = MutableStateFlow<List<Plugin>>(emptyList())
    val marketPlugins: StateFlow<List<Plugin>> = _marketPlugins

    fun loadPlugins() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // Load installed plugins
            systemRepository.getPlugins().fold(
                onSuccess = { plugins ->
                    _installedPlugins.value = plugins
                },
                onFailure = { /* ignore */ }
            )

            // Load market plugins
            systemRepository.getPluginMarket().fold(
                onSuccess = { plugins ->
                    _marketPlugins.value = plugins
                },
                onFailure = { /* ignore */ }
            )

            if (_installedPlugins.value.isNotEmpty() || _marketPlugins.value.isNotEmpty()) {
                _uiState.value = UiState.Success
            } else {
                _uiState.value = UiState.Error("加载插件列表失败")
            }
        }
    }

    fun installPlugin(plugin: Plugin) {
        viewModelScope.launch {
            systemRepository.installPlugin(plugin.id).fold(
                onSuccess = {
                    loadPlugins()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "安装失败")
                }
            )
        }
    }

    fun uninstallPlugin(plugin: Plugin) {
        viewModelScope.launch {
            systemRepository.uninstallPlugin(plugin.id).fold(
                onSuccess = {
                    loadPlugins()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "卸载失败")
                }
            )
        }
    }

    fun reloadPlugins() {
        viewModelScope.launch {
            systemRepository.reloadPlugins().fold(
                onSuccess = {
                    loadPlugins()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "重载失败")
                }
            )
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
