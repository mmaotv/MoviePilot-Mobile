package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.DoubanHotItem
import com.moviepilot.app.data.model.MediaInfo
import com.moviepilot.app.data.model.TrendingItem
import com.moviepilot.app.data.repository.ResourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _doubanHotMoviesState = MutableStateFlow<DoubanState>(DoubanState.Loading)
    val doubanHotMoviesState: StateFlow<DoubanState> = _doubanHotMoviesState

    private val _doubanHotTVsState = MutableStateFlow<DoubanState>(DoubanState.Loading)
    val doubanHotTVsState: StateFlow<DoubanState> = _doubanHotTVsState

    private val _tmdbTrendingState = MutableStateFlow<TrendingState>(TrendingState.Loading)
    val tmdbTrendingState: StateFlow<TrendingState> = _tmdbTrendingState

    private val _doubanTop250State = MutableStateFlow<DoubanState>(DoubanState.Loading)
    val doubanTop250State: StateFlow<DoubanState> = _doubanTop250State

    private val _doubanWeeklyChineseState = MutableStateFlow<DoubanState>(DoubanState.Loading)
    val doubanWeeklyChineseState: StateFlow<DoubanState> = _doubanWeeklyChineseState

    private val _doubanWeeklyGlobalState = MutableStateFlow<DoubanState>(DoubanState.Loading)
    val doubanWeeklyGlobalState: StateFlow<DoubanState> = _doubanWeeklyGlobalState

    private val _doubanAnimationState = MutableStateFlow<DoubanState>(DoubanState.Loading)
    val doubanAnimationState: StateFlow<DoubanState> = _doubanAnimationState

    private val _nowPlayingState = MutableStateFlow<MediaInfoState>(MediaInfoState.Loading)
    val nowPlayingState: StateFlow<MediaInfoState> = _nowPlayingState

    init {
        loadAllRecommendations()
    }

    fun loadAllRecommendations() {
        loadDoubanHotMovies()
        loadDoubanHotTVs()
        loadTmdbTrending()
        loadDoubanTop250()
        loadDoubanWeeklyChinese()
        loadDoubanWeeklyGlobal()
        loadDoubanAnimation()
        loadNowPlaying()
    }

    fun loadDoubanHotMovies() {
        viewModelScope.launch {
            _doubanHotMoviesState.value = DoubanState.Loading
            resourceRepository.getDoubanHotMovies().fold(
                onSuccess = { _doubanHotMoviesState.value = DoubanState.Success(it) },
                onFailure = { _doubanHotMoviesState.value = DoubanState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadDoubanHotTVs() {
        viewModelScope.launch {
            _doubanHotTVsState.value = DoubanState.Loading
            resourceRepository.getDoubanHotTVs().fold(
                onSuccess = { _doubanHotTVsState.value = DoubanState.Success(it) },
                onFailure = { _doubanHotTVsState.value = DoubanState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadTmdbTrending() {
        viewModelScope.launch {
            _tmdbTrendingState.value = TrendingState.Loading
            resourceRepository.getTMDbTrending().fold(
                onSuccess = { _tmdbTrendingState.value = TrendingState.Success(it) },
                onFailure = { _tmdbTrendingState.value = TrendingState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadDoubanTop250() {
        viewModelScope.launch {
            _doubanTop250State.value = DoubanState.Loading
            resourceRepository.getDoubanTop250().fold(
                onSuccess = { _doubanTop250State.value = DoubanState.Success(it) },
                onFailure = { _doubanTop250State.value = DoubanState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadDoubanWeeklyChinese() {
        viewModelScope.launch {
            _doubanWeeklyChineseState.value = DoubanState.Loading
            resourceRepository.getDoubanWeeklyChinese().fold(
                onSuccess = { _doubanWeeklyChineseState.value = DoubanState.Success(it) },
                onFailure = { _doubanWeeklyChineseState.value = DoubanState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadDoubanWeeklyGlobal() {
        viewModelScope.launch {
            _doubanWeeklyGlobalState.value = DoubanState.Loading
            resourceRepository.getDoubanWeeklyGlobal().fold(
                onSuccess = { _doubanWeeklyGlobalState.value = DoubanState.Success(it) },
                onFailure = { _doubanWeeklyGlobalState.value = DoubanState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadDoubanAnimation() {
        viewModelScope.launch {
            _doubanAnimationState.value = DoubanState.Loading
            resourceRepository.getDoubanAnimation().fold(
                onSuccess = { _doubanAnimationState.value = DoubanState.Success(it) },
                onFailure = { _doubanAnimationState.value = DoubanState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadNowPlaying() {
        viewModelScope.launch {
            _nowPlayingState.value = MediaInfoState.Loading
            resourceRepository.getNowPlaying().fold(
                onSuccess = { _nowPlayingState.value = MediaInfoState.Success(it) },
                onFailure = { _nowPlayingState.value = MediaInfoState.Error(it.message ?: "加载失败") }
            )
        }
    }

    sealed class DoubanState {
        object Loading : DoubanState()
        data class Success(val data: List<DoubanHotItem>) : DoubanState()
        data class Error(val message: String) : DoubanState()
    }

    sealed class TrendingState {
        object Loading : TrendingState()
        data class Success(val data: List<TrendingItem>) : TrendingState()
        data class Error(val message: String) : TrendingState()
    }

    sealed class MediaInfoState {
        object Loading : MediaInfoState()
        data class Success(val data: List<MediaInfo>) : MediaInfoState()
        data class Error(val message: String) : MediaInfoState()
    }
}
