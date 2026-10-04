package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.BangumiCalendarItem
import com.moviepilot.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _calendarItems = MutableStateFlow<List<BangumiCalendarItem>>(emptyList())
    val calendarItems: StateFlow<List<BangumiCalendarItem>> = _calendarItems

    private val _selectedWeekday = MutableStateFlow(0)
    val selectedWeekday: StateFlow<Int> = _selectedWeekday

    fun loadCalendar() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            systemRepository.getBangumiCalendar().fold(
                onSuccess = { items ->
                    _calendarItems.value = items
                    _uiState.value = UiState.Success
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    fun selectWeekday(weekday: Int) {
        _selectedWeekday.value = weekday
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
