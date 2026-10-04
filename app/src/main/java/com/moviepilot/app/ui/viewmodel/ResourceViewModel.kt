package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.MediaInfo
import com.moviepilot.app.data.model.TorrentSearchResult
import com.moviepilot.app.data.repository.ResourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResourceViewModel @Inject constructor(
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    private val _trendingState = MutableStateFlow<TrendingState>(TrendingState.Loading)
    val trendingState: StateFlow<TrendingState> = _trendingState

    /** 下载状态：torrent hash -> DownloadState */
    private val _downloadState = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadState: StateFlow<Map<String, DownloadState>> = _downloadState

    init {
        loadTrending()
    }

    fun searchTorrents(keyword: String) {
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            resourceRepository.searchTorrents(keyword).fold(
                onSuccess = { torrents ->
                    _searchState.value = SearchState.Success(torrents)
                },
                onFailure = { error ->
                    _searchState.value = SearchState.Error(error.message ?: "搜索失败")
                }
            )
        }
    }

    fun loadTrending(type: String = "MOV") {
        viewModelScope.launch {
            _trendingState.value = TrendingState.Loading
            resourceRepository.getTrending(type).fold(
                onSuccess = { mediaList ->
                    _trendingState.value = TrendingState.Success(mediaList)
                },
                onFailure = { error ->
                    _trendingState.value = TrendingState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    /**
     * 下载种子 - 发送请求到服务器让服务器下载
     * @param torrent 要下载的种子
     */
    fun downloadTorrent(torrent: TorrentSearchResult) {
        // 使用 enclosure（磁力/torrent URL）作为唯一标识
        val key = torrent.torrentInfo.enclosure ?: torrent.torrentInfo.title
        viewModelScope.launch {
            // 设置正在下载
            _downloadState.value = _downloadState.value.toMutableMap().apply {
                put(key, DownloadState.Loading)
            }
            resourceRepository.downloadTorrent(torrent).fold(
                onSuccess = {
                    _downloadState.value = _downloadState.value.toMutableMap().apply {
                        put(key, DownloadState.Success)
                    }
                },
                onFailure = { error ->
                    _downloadState.value = _downloadState.value.toMutableMap().apply {
                        put(key, DownloadState.Error(error.message ?: "下载失败"))
                    }
                }
            )
        }
    }

    /** 清除某个种子的下载状态（关闭提示后调用） */
    fun clearDownloadState(torrent: TorrentSearchResult) {
        val key = torrent.torrentInfo.enclosure ?: torrent.torrentInfo.title
        _downloadState.value = _downloadState.value.toMutableMap().apply { remove(key) }
    }

    sealed class SearchState {
        object Idle : SearchState()
        object Loading : SearchState()
        data class Success(val torrents: List<TorrentSearchResult>) : SearchState()
        data class Error(val message: String) : SearchState()
    }

    sealed class TrendingState {
        object Loading : TrendingState()
        data class Success(val mediaList: List<MediaInfo>) : TrendingState()
        data class Error(val message: String) : TrendingState()
    }

    sealed class DownloadState {
        object Loading : DownloadState()
        object Success : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}
