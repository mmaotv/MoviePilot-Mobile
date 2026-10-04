package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.model.User
import com.moviepilot.app.data.model.UserCreate
import com.moviepilot.app.data.model.UserUpdate
import com.moviepilot.app.data.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            systemRepository.getUsers().fold(
                onSuccess = { userList ->
                    _users.value = userList
                    _uiState.value = UiState.Success
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载用户失败")
                }
            )
        }
    }

    fun createUser(
        name: String,
        password: String,
        email: String?,
        isAdmin: Boolean
    ) {
        viewModelScope.launch {
            val user = UserCreate(
                name = name,
                password = password,
                email = email,
                isAdmin = isAdmin,
                permission = null
            )
            systemRepository.createUser(user).fold(
                onSuccess = {
                    loadUsers()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "创建用户失败")
                }
            )
        }
    }

    fun updateUser(
        name: String,
        password: String?,
        email: String?,
        isAdmin: Boolean?,
        isActive: Boolean?
    ) {
        viewModelScope.launch {
            val user = UserUpdate(
                name = name,
                password = password,
                email = email,
                isAdmin = isAdmin,
                isActive = isActive,
                permission = null
            )
            systemRepository.updateUser(user).fold(
                onSuccess = {
                    loadUsers()
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "更新用户失败")
                }
            )
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            user.name?.let { username ->
                systemRepository.deleteUser(username).fold(
                    onSuccess = {
                        loadUsers()
                    },
                    onFailure = { error ->
                        _uiState.value = UiState.Error(error.message ?: "删除用户失败")
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
