package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.MediaInfo
import com.moviepilot.app.data.model.TmdbEpisode
import com.moviepilot.app.data.model.TmdbPerson
import com.moviepilot.app.data.model.TmdbSeasonDetail
import com.moviepilot.app.data.repository.MediaServerRepository
import com.moviepilot.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _mediaDetail = MutableStateFlow<MediaInfo?>(null)
    val mediaDetail: StateFlow<MediaInfo?> = _mediaDetail

    private val _seasons = MutableStateFlow<List<TmdbSeasonDetail>>(emptyList())
    val seasons: StateFlow<List<TmdbSeasonDetail>> = _seasons

    private val _episodes = MutableStateFlow<List<TmdbEpisode>>(emptyList())
    val episodes: StateFlow<List<TmdbEpisode>> = _episodes

    private val _credits = MutableStateFlow<List<TmdbPerson>>(emptyList())
    val credits: StateFlow<List<TmdbPerson>> = _credits

    fun loadMediaDetail(mediaId: String, mediaType: String, title: String?, year: String?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // Load media detail from API
            systemRepository.getMediaDetail(mediaId, mediaType, title, year).fold(
                onSuccess = { detail ->
                    _mediaDetail.value = detail
                    _uiState.value = UiState.Success(
                        mediaDetail = detail,
                        seasons = _seasons.value,
                        episodes = _episodes.value,
                        credits = _credits.value
                    )
                    // Load additional info
                    loadSeasonsAndCredits(mediaId, mediaType, detail)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    private fun loadSeasonsAndCredits(mediaId: String, mediaType: String, detail: MediaInfo) {
        viewModelScope.launch {
            // Load TMDB seasons for TV shows
            if (mediaType == "tv" && detail.id != null) {
                systemRepository.getTMDbSeasons(detail.id).fold(
                    onSuccess = { seasonList ->
                        _seasons.value = seasonList
                        updateSuccessState()
                    },
                    onFailure = { /* ignore */ }
                )

                // Load episodes for first season
                systemRepository.getTMDbEpisodes(detail.id, 1, null).fold(
                    onSuccess = { episodeList ->
                        _episodes.value = episodeList
                        updateSuccessState()
                    },
                    onFailure = { /* ignore */ }
                )

                // Load credits
                systemRepository.getTMDbCredits(detail.id, mediaType).fold(
                    onSuccess = { creditsList ->
                        _credits.value = creditsList
                        updateSuccessState()
                    },
                    onFailure = { /* ignore */ }
                )
            } else if (mediaType == "movie" && detail.id != null) {
                // Load credits for movies
                systemRepository.getTMDbCredits(detail.id, mediaType).fold(
                    onSuccess = { creditsList ->
                        _credits.value = creditsList
                        updateSuccessState()
                    },
                    onFailure = { /* ignore */ }
                )
            }
        }
    }

    fun loadEpisodes(tmdbId: Int, seasonNumber: Int, episodeGroup: String? = null) {
        viewModelScope.launch {
            systemRepository.getTMDbEpisodes(tmdbId, seasonNumber, episodeGroup).fold(
                onSuccess = { episodeList ->
                    _episodes.value = episodeList
                    updateSuccessState()
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    private fun updateSuccessState() {
        _mediaDetail.value?.let { detail ->
            _uiState.value = UiState.Success(
                mediaDetail = detail,
                seasons = _seasons.value,
                episodes = _episodes.value,
                credits = _credits.value
            )
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val mediaDetail: MediaInfo,
            val seasons: List<TmdbSeasonDetail>,
            val episodes: List<TmdbEpisode>,
            val credits: List<TmdbPerson>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }
}
