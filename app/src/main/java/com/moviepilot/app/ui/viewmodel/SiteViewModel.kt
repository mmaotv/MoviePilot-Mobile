package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.SiteInfo
import com.moviepilot.app.data.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SiteViewModel @Inject constructor(
    private val siteRepository: SiteRepository
) : ViewModel() {

    sealed class SitesState {
        object Loading : SitesState()
        data class Success(val sites: List<SiteInfo>) : SitesState()
        data class Error(val message: String) : SitesState()
    }

    private val _sitesState = MutableStateFlow<SitesState>(SitesState.Loading)
    val sitesState: StateFlow<SitesState> = _sitesState

    fun loadSites() {
        viewModelScope.launch {
            _sitesState.value = SitesState.Loading
            siteRepository.getSites()
                .onSuccess { sites ->
                    _sitesState.value = SitesState.Success(sites)
                }
                .onFailure { error ->
                    _sitesState.value = SitesState.Error(error.message ?: "加载失败")
                }
        }
    }

    fun addSite(site: SiteInfo) {
        viewModelScope.launch {
            siteRepository.addSite(site)
                .onSuccess {
                    loadSites() // Reload after adding
                }
                .onFailure { error ->
                    _sitesState.value = SitesState.Error("添加失败: ${error.message}")
                }
        }
    }

    fun updateSite(site: SiteInfo) {
        viewModelScope.launch {
            siteRepository.updateSite(site)
                .onSuccess {
                    loadSites() // Reload after updating
                }
                .onFailure { error ->
                    _sitesState.value = SitesState.Error("更新失败: ${error.message}")
                }
        }
    }

    fun deleteSite(id: Int) {
        viewModelScope.launch {
            siteRepository.deleteSite(id)
                .onSuccess {
                    loadSites() // Reload after deleting
                }
                .onFailure { error ->
                    _sitesState.value = SitesState.Error("删除失败: ${error.message}")
                }
        }
    }
}
