package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.DoubanHotItem
import com.moviepilot.app.data.model.MediaInfo
import com.moviepilot.app.data.repository.ResourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _tmdbMoviesState = MutableStateFlow<MediaExploreState>(MediaExploreState.Idle)
    val tmdbMoviesState: StateFlow<MediaExploreState> = _tmdbMoviesState

    private val _tmdbTVsState = MutableStateFlow<MediaExploreState>(MediaExploreState.Idle)
    val tmdbTVsState: StateFlow<MediaExploreState> = _tmdbTVsState

    private val _doubanMoviesState = MutableStateFlow<DoubanExploreState>(DoubanExploreState.Idle)
    val doubanMoviesState: StateFlow<DoubanExploreState> = _doubanMoviesState

    private val _doubanTVsState = MutableStateFlow<DoubanExploreState>(DoubanExploreState.Idle)
    val doubanTVsState: StateFlow<DoubanExploreState> = _doubanTVsState

    private val _currentSource = MutableStateFlow<ExploreSource>(ExploreSource.TMDB_MOVIES)
    val currentSource: StateFlow<ExploreSource> = _currentSource

    private var currentFilters = ExploreFilters()

    init {
        loadCurrentSource()
    }

    fun setSource(source: ExploreSource) {
        _currentSource.value = source
        loadCurrentSource()
    }

    private fun loadCurrentSource() {
        when (_currentSource.value) {
            ExploreSource.TMDB_MOVIES -> loadTmdbMovies()
            ExploreSource.TMDB_TVS -> loadTmdbTVs()
            ExploreSource.DOUBAN_MOVIES -> loadDoubanMovies()
            ExploreSource.DOUBAN_TVS -> loadDoubanTVs()
        }
    }

    fun applyFilters(filters: ExploreFilters) {
        currentFilters = filters
        loadCurrentSource()
    }

    fun loadTmdbMovies() {
        viewModelScope.launch {
            _tmdbMoviesState.value = MediaExploreState.Loading
            resourceRepository.discoverTMDbMovies(
                sortBy = currentFilters.tmdbSortBy,
                withGenres = currentFilters.tmdbGenres,
                withLanguage = currentFilters.tmdbLanguage,
                releaseDate = currentFilters.releaseDate
            ).fold(
                onSuccess = { _tmdbMoviesState.value = MediaExploreState.Success(it) },
                onFailure = { _tmdbMoviesState.value = MediaExploreState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadTmdbTVs() {
        viewModelScope.launch {
            _tmdbTVsState.value = MediaExploreState.Loading
            resourceRepository.discoverTMDbTVs(
                sortBy = currentFilters.tmdbSortBy,
                withGenres = currentFilters.tmdbGenres,
                withLanguage = currentFilters.tmdbLanguage,
                releaseDate = currentFilters.releaseDate
            ).fold(
                onSuccess = { _tmdbTVsState.value = MediaExploreState.Success(it) },
                onFailure = { _tmdbTVsState.value = MediaExploreState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadDoubanMovies() {
        viewModelScope.launch {
            _doubanMoviesState.value = DoubanExploreState.Loading
            resourceRepository.discoverDoubanMovies(
                sort = currentFilters.doubanSort,
                tags = currentFilters.doubanTags
            ).fold(
                onSuccess = { _doubanMoviesState.value = DoubanExploreState.Success(it) },
                onFailure = { _doubanMoviesState.value = DoubanExploreState.Error(it.message ?: "加载失败") }
            )
        }
    }

    fun loadDoubanTVs() {
        viewModelScope.launch {
            _doubanTVsState.value = DoubanExploreState.Loading
            resourceRepository.discoverDoubanTVs(
                sort = currentFilters.doubanSort,
                tags = currentFilters.doubanTags
            ).fold(
                onSuccess = { _doubanTVsState.value = DoubanExploreState.Success(it) },
                onFailure = { _doubanTVsState.value = DoubanExploreState.Error(it.message ?: "加载失败") }
            )
        }
    }

    sealed class MediaExploreState {
        object Idle : MediaExploreState()
        object Loading : MediaExploreState()
        data class Success(val data: List<MediaInfo>) : MediaExploreState()
        data class Error(val message: String) : MediaExploreState()
    }

    sealed class DoubanExploreState {
        object Idle : DoubanExploreState()
        object Loading : DoubanExploreState()
        data class Success(val data: List<DoubanHotItem>) : DoubanExploreState()
        data class Error(val message: String) : DoubanExploreState()
    }

    data class ExploreFilters(
        val tmdbSortBy: String? = "popularity.desc",
        val tmdbGenres: String? = "",
        val tmdbLanguage: String? = "",
        val releaseDate: String? = null,
        val doubanSort: String? = "R",
        val doubanTags: String? = ""
    )
}

enum class ExploreSource(val label: String) {
    TMDB_MOVIES("TMDB 电影"),
    TMDB_TVS("TMDB 电视剧"),
    DOUBAN_MOVIES("豆瓣电影"),
    DOUBAN_TVS("豆瓣电视剧")
}
