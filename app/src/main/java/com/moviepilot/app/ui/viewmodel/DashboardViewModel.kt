package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.repository.DashboardData
import com.moviepilot.app.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    init {
        loadDashboardData()
        // Start polling for real-time updates every 5 seconds
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Poll every 5 seconds
                refreshDashboardData()
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _dashboardState.value = DashboardState.Loading
            dashboardRepository.getDashboardData().fold(
                onSuccess = { data ->
                    _dashboardState.value = DashboardState.Success(data)
                },
                onFailure = { error ->
                    _dashboardState.value = DashboardState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    fun refreshDashboardData() {
        viewModelScope.launch {
            dashboardRepository.getDashboardData().fold(
                onSuccess = { data ->
                    _dashboardState.value = DashboardState.Success(data)
                },
                onFailure = { /* Silent fail for background refresh */ }
            )
        }
    }

    sealed class DashboardState {
        object Loading : DashboardState()
        data class Success(val data: DashboardData) : DashboardState()
        data class Error(val message: String) : DashboardState()
    }
}
