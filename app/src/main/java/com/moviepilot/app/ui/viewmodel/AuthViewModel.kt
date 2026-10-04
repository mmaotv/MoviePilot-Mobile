package com.moviepilot.app.ui.viewmodel

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.local.PreferencesManager
import com.moviepilot.app.data.model.ServerProfile
import com.moviepilot.app.data.model.UserConfig
import com.moviepilot.app.data.network.ApiClient
import com.moviepilot.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _currentUser = MutableStateFlow<UserConfig?>(null)
    val currentUser: StateFlow<UserConfig?> = _currentUser

    private val _serverUrl = MutableStateFlow("http://YOUR_SERVER_IP:PORT/")
    val serverUrl: StateFlow<String> = _serverUrl

    // 是否正在初始化（恢复持久化状态中），为 true 时不显示登录页
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing

    // 记住密码开关
    private val _rememberPassword = MutableStateFlow(false)
    val rememberPassword: StateFlow<Boolean> = _rememberPassword

    // ── 多服务器管理 ──────────────────────────────────────────

    private val _serverProfiles = MutableStateFlow<List<ServerProfile>>(emptyList())
    val serverProfiles: StateFlow<List<ServerProfile>> = _serverProfiles

    private val _activeServerId = MutableStateFlow<String?>(null)
    val activeServerId: StateFlow<String?> = _activeServerId

    // 连接超时/断开事件，用于通知 UI 层跳转登录页
    private val _connectionLost = MutableStateFlow<String?>(null)
    val connectionLost: StateFlow<String?> = _connectionLost

    init {
        viewModelScope.launch {
            try {
                // 1. 迁移旧版单服务器配置
                preferencesManager.migrateLegacyServer()

                // 2. 加载服务器列表
                loadServerProfiles()

                // 3. 恢复记住密码标志
                _rememberPassword.value = preferencesManager.getRememberPassword()

                // 4. 恢复激活的服务器
                val activeId = preferencesManager.getActiveServerId()
                val profiles = _serverProfiles.value

                val activeProfile = if (activeId != null) {
                    profiles.find { it.id == activeId }
                } else {
                    profiles.find { it.isDefault } ?: profiles.firstOrNull()
                }

                if (activeProfile != null) {
                    _activeServerId.value = activeProfile.id
                    val normalizedUrl = normalizeUrl(activeProfile.serverUrl)
                    _serverUrl.value = normalizedUrl
                    ApiClient.setBaseUrl(normalizedUrl)

                    // 如果该服务器有保存的用户名，预填
                    if (activeProfile.username.isNotEmpty()) {
                        ApiClient.setCredentials(activeProfile.username, "")
                    }
                } else {
                    // 无服务器配置，使用旧版恢复
                    val savedUrl = preferencesManager.getServerUrl()
                    val normalizedUrl = normalizeUrl(savedUrl)
                    _serverUrl.value = normalizedUrl
                    ApiClient.setBaseUrl(normalizedUrl)
                }

                // 5. 恢复凭证
                val credentials = preferencesManager.getCredentials()
                if (credentials != null) {
                    ApiClient.setCredentials(credentials.first, credentials.second)
                }

                // 6. 恢复 token，成功后立即拉取当前用户（保持登录态）
                val savedToken = preferencesManager.getAuthToken()
                if (savedToken != null) {
                    ApiClient.setAuthToken(savedToken)
                    authRepository.getCurrentUser().fold(
                        onSuccess = { user ->
                            _currentUser.value = user
                            _loginState.value = LoginState.Success(savedToken)
                        },
                        onFailure = {
                            // token 失效，清除本地存储，回到登录页
                            preferencesManager.clearAuthToken()
                            preferencesManager.clearCredentials()
                            ApiClient.setAuthToken("")
                            ApiClient.clearCredentials()
                        }
                    )
                }
            } catch (e: Exception) {
                // 初始化失败，仍允许显示登录页
                _loginState.value = LoginState.Idle
            } finally {
                // 初始化完成，允许 UI 渲染
                _isInitializing.value = false
            }
        }
    }

    // ── 登录 ─────────────────────────────────────────────────

    fun setServerUrl(url: String) {
        viewModelScope.launch {
            val normalizedUrl = normalizeUrl(url)
            ApiClient.setBaseUrl(normalizedUrl)
            preferencesManager.saveServerUrl(normalizedUrl)
            _serverUrl.value = normalizedUrl
        }
    }

    fun setRememberPassword(remember: Boolean) {
        _rememberPassword.value = remember
        viewModelScope.launch {
            preferencesManager.saveRememberPassword(remember)
            if (!remember) {
                // 取消记住密码时，清除所有服务器配置中保存的密码
                val profiles = _serverProfiles.value.map {
                    it.copy(rememberPassword = false, encPassword = "")
                }
                preferencesManager.saveServerProfiles(profiles)
                _serverProfiles.value = profiles
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // 5 秒超时
                val result = withTimeoutOrNull(5000L) {
                    authRepository.login(username, password)
                }
                if (result == null) {
                    // 超时
                    _loginState.value = LoginState.Error("连接超时，请检查网络或服务器地址")
                    return@launch
                }
                result.fold(
                    onSuccess = { token ->
                        // 持久化保存 token
                        preferencesManager.saveAuthToken(token)
                        ApiClient.setAuthToken(token)

                        // 根据记住密码标志决定是否保存凭证
                        if (_rememberPassword.value) {
                            preferencesManager.saveCredentials(username, password)
                            ApiClient.setCredentials(username, password)
                        } else {
                            // 不记住密码时仍然设置内存凭证（WebView 自动登录需要）
                            ApiClient.setCredentials(username, password)
                            preferencesManager.clearCredentials()
                        }

                        _loginState.value = LoginState.Success(token)
                        loadCurrentUser()

                        // 更新或创建服务器配置（含密码保存）
                        upsertCurrentServerProfile(username, password)
                    },
                    onFailure = { error ->
                        _loginState.value = LoginState.Error(error.message ?: "登录失败")
                    }
                )
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "登录失败")
            }
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    _currentUser.value = user
                },
                onFailure = {
                    // Handle error silently
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferencesManager.clearAuthToken()
            preferencesManager.clearCredentials()
            ApiClient.setAuthToken("")
            ApiClient.clearCredentials()
            CookieManager.getInstance().removeAllCookies(null)
            authRepository.logout()
            _currentUser.value = null
            _loginState.value = LoginState.Idle
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    // ── 连接丢失 ──────────────────────────────────────────────

    fun notifyConnectionLost(message: String) {
        _connectionLost.value = message
    }

    fun clearConnectionLost() {
        _connectionLost.value = null
    }

    /**
     * 连接丢失时，清除登录态并回到登录页
     */
    fun handleConnectionLost(message: String) {
        viewModelScope.launch {
            try {
                preferencesManager.clearAuthToken()
                ApiClient.setAuthToken("")
                _currentUser.value = null
                _loginState.value = LoginState.Error(message)
                _connectionLost.value = null
            } catch (_: Exception) {
                // 即使出错也要确保 UI 知道连接丢失
                _connectionLost.value = null
            }
        }
    }

    // ── 多服务器管理 ──────────────────────────────────────────

    private suspend fun loadServerProfiles() {
        _serverProfiles.value = preferencesManager.getServerProfiles()
        _activeServerId.value = preferencesManager.getActiveServerId()
    }

    /**
     * 登录成功后，更新或创建当前服务器的 ServerProfile
     * 如果 rememberPassword=true，加密保存密码到 encPassword
     */
    private suspend fun upsertCurrentServerProfile(username: String, password: String) {
        val currentUrl = ApiClient.getBaseUrl()
        val profiles = _serverProfiles.value.toMutableList()
        val remember = _rememberPassword.value

        // 加密失败时返回空字符串，不影响登录流程
        val encPwd = if (remember) runCatching {
            preferencesManager.encryptPassword(password)
        }.getOrDefault("") else ""

        // 查找是否已有相同 URL 的配置
        val existing = profiles.find { normalizeUrl(it.serverUrl) == normalizeUrl(currentUrl) }
        if (existing != null) {
            // 更新用户名、密码和最后使用时间
            val updated = existing.copy(
                username = username,
                rememberPassword = remember,
                encPassword = encPwd,
                lastUsedAt = System.currentTimeMillis()
            )
            profiles[profiles.indexOf(existing)] = updated
            preferencesManager.saveServerProfiles(profiles)
            preferencesManager.setActiveServerId(updated.id)
            _activeServerId.value = updated.id
        } else {
            // 新建配置
            val name = runCatching {
                java.net.URL(currentUrl).host
            }.getOrDefault(currentUrl)
            val newProfile = ServerProfile(
                name = name,
                serverUrl = currentUrl,
                username = username,
                rememberPassword = remember,
                encPassword = encPwd,
                isDefault = profiles.isEmpty(),
                lastUsedAt = System.currentTimeMillis()
            )
            profiles.add(newProfile)
            preferencesManager.saveServerProfiles(profiles)
            preferencesManager.setActiveServerId(newProfile.id)
            _activeServerId.value = newProfile.id
        }

        _serverProfiles.value = profiles
    }

    /**
     * 切换到指定服务器（用户快速切换）
     * - 清除当前登录态
     * - 加载目标服务器的 baseUrl
     * - 自动填充用户名，如果 rememberPassword=true 则自动填充密码
     * - 如果有保存的 token，尝试自动登录
     */
    fun switchToServer(profile: ServerProfile) {
        viewModelScope.launch {
            // 1. 清除当前 WebView Cookie
            CookieManager.getInstance().removeAllCookies(null)

            // 2. 清除当前 token 和凭证
            ApiClient.setAuthToken("")
            ApiClient.clearCredentials()
            preferencesManager.clearAuthToken()
            preferencesManager.clearCredentials()

            // 3. 切换到新服务器
            val normalizedUrl = normalizeUrl(profile.serverUrl)
            ApiClient.setBaseUrl(normalizedUrl)
            _serverUrl.value = normalizedUrl

            // 4. 更新激活服务器
            preferencesManager.setActiveServerId(profile.id)
            preferencesManager.saveServerUrl(normalizedUrl)
            _activeServerId.value = profile.id

            // 5. 更新最后使用时间
            val profiles = _serverProfiles.value.toMutableList()
            val idx = profiles.indexOfFirst { it.id == profile.id }
            if (idx >= 0) {
                profiles[idx] = profiles[idx].copy(lastUsedAt = System.currentTimeMillis())
                preferencesManager.saveServerProfiles(profiles)
                _serverProfiles.value = profiles
            }

            // 6. 恢复记住密码标志
            _rememberPassword.value = profile.rememberPassword

            // 7. 清除当前用户，回到登录页（UI 层会读取 profile 自动填充）
            _currentUser.value = null
            _loginState.value = LoginState.Idle
        }
    }

    /**
     * 获取指定服务器的解密密码，用于自动填充
     */
    fun getDecryptedPassword(profile: ServerProfile): String? {
        return preferencesManager.decryptServerPassword(profile)
    }

    fun addServerProfile(profile: ServerProfile) {
        viewModelScope.launch {
            preferencesManager.addServerProfile(profile)
            loadServerProfiles()
        }
    }

    fun updateServerProfile(profile: ServerProfile) {
        viewModelScope.launch {
            preferencesManager.updateServerProfile(profile)
            loadServerProfiles()
        }
    }

    fun deleteServerProfile(profileId: String) {
        viewModelScope.launch {
            preferencesManager.deleteServerProfile(profileId)
            loadServerProfiles()
        }
    }

    fun refreshServerProfiles() {
        viewModelScope.launch {
            loadServerProfiles()
        }
    }

    // ── 工具方法 ─────────────────────────────────────────────

    private fun normalizeUrl(url: String): String {
        val withScheme = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> "http://$url"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val token: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}
