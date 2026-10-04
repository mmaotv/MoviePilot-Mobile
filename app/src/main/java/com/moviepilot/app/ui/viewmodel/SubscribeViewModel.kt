package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.Subscribe
import com.moviepilot.app.data.model.SubscribeRequest
import com.moviepilot.app.data.repository.SubscribeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscribeViewModel @Inject constructor(
    private val subscribeRepository: SubscribeRepository
) : ViewModel() {

    private val _subscribeState = MutableStateFlow<SubscribeState>(SubscribeState.Loading)
    val subscribeState: StateFlow<SubscribeState> = _subscribeState

    init {
        loadSubscriptions()
    }

    fun loadSubscriptions(type: String? = null) {
        viewModelScope.launch {
            _subscribeState.value = SubscribeState.Loading
            subscribeRepository.getSubscriptions(type).fold(
                onSuccess = { subscriptions ->
                    _subscribeState.value = SubscribeState.Success(subscriptions)
                },
                onFailure = { error ->
                    _subscribeState.value = SubscribeState.Error(error.message ?: "加载失败")
                }
            )
        }
    }

    fun addSubscription(subscription: SubscribeRequest) {
        viewModelScope.launch {
            subscribeRepository.addSubscription(subscription).fold(
                onSuccess = {
                    loadSubscriptions()
                },
                onFailure = { error ->
                    // Handle error
                }
            )
        }
    }

    fun deleteSubscription(id: Int) {
        viewModelScope.launch {
            subscribeRepository.deleteSubscription(id).fold(
                onSuccess = {
                    loadSubscriptions()
                },
                onFailure = { error ->
                    // Handle error
                }
            )
        }
    }

    fun refreshSubscribes() {
        loadSubscriptions()
    }

    fun updateSubscription(subscription: SubscribeRequest) {
        viewModelScope.launch {
            subscribeRepository.updateSubscription(subscription).fold(
                onSuccess = {
                    loadSubscriptions()
                },
                onFailure = { error ->
                    // Handle error
                }
            )
        }
    }

    sealed class SubscribeState {
        object Loading : SubscribeState()
        data class Success(val subscriptions: List<Subscribe>) : SubscribeState()
        data class Error(val message: String) : SubscribeState()
    }
}
