package com.moviepilot.app.ui.viewmodel

import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviepilot.app.data.local.PreferencesManager
import com.moviepilot.app.data.model.AccountProfile
import com.moviepilot.app.data.model.LoginResponse
import com.moviepilot.app.data.model.ServerProfile
import com.moviepilot.app.data.model.WebUserSeed
import com.moviepilot.app.data.model.displayServerUrl
import com.moviepilot.app.data.model.normalizeServerUrl
import com.moviepilot.app.data.model.parseServerUrl
import com.moviepilot.app.data.model.serverCandidates
import com.moviepilot.app.data.network.ApiClient
import com.moviepilot.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * 登录 / 服务器 / 账号状态。
 *
 * 设计前提：除了登录与切换，所有功能都由服务器网页版提供，
 * 所以这里只维护「当前连哪台服务器、用哪个账号」。
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    data class UiState(
        val initializing: Boolean = true,
        val loggedIn: Boolean = false,
        /** 用户当前输入的原始地址（可能还没写完，不做规范化） */
        val serverInput: String = "",
        /** 规范化后的地址，用于连接与展示；输入非法时为空 */
        val serverUrl: String = "",
        val servers: List<ServerProfile> = emptyList(),
        val accounts: List<AccountProfile> = emptyList(),
        val currentUsername: String = "",
        val token: String = "",
        /**
         * 写进网页版 localStorage 的 `user` store 快照。
         * 网页版根路由靠里面的 superUser 决定进主页还是应用中心。
         */
        val webUserSeed: String = "",
        val loginState: LoginState = LoginState.Idle,
        val panelVisible: Boolean = false,
        val sessionEpoch: Int = 0,
        /** 每次切换服务器/账号 +1，登录页据此重置输入框 */
        val revision: Int = 0,
        val initialUsername: String = "",
        val initialPassword: String = "",
        val rememberPassword: Boolean = false
    )

    sealed interface LoginState {
        data object Idle : LoginState
        data object Loading : LoginState
        data class Error(val message: String) : LoginState
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /** 服务器地址输入时的账号查询防抖任务 */
    private var accountsJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { prefs.migrateIfNeeded() }

            val servers = prefs.getServers()
            val storedActive = normalizeServerUrl(prefs.getActiveServerUrl().orEmpty())
            // 没有任何内置地址：没存过就是空，由用户自己输入
            val activeUrl = storedActive.ifBlank { servers.firstOrNull()?.serverUrl.orEmpty() }
            if (activeUrl.isNotBlank()) ApiClient.setBaseUrl(activeUrl)

            val accounts = if (activeUrl.isBlank()) emptyList() else accountsOf(activeUrl)
            val last = accounts.firstOrNull()

            var loggedIn = false
            var token = ""
            var userSeed = ""
            val savedToken = prefs.getAuthToken()
            if (activeUrl.isNotBlank() && !savedToken.isNullOrBlank()) {
                ApiClient.setAuthToken(savedToken)
                val session = withTimeoutOrNull(5_000) { authRepository.loadSession() }
                when (session?.state) {
                    AuthRepository.TokenState.Valid -> {
                        loggedIn = true
                        token = savedToken
                        userSeed = session.userSeed.orEmpty()
                    }
                    AuthRepository.TokenState.Invalid -> {
                        // token 确实失效了，清掉；网络异常的情况保留
                        ApiClient.setAuthToken("")
                        prefs.clearAuthToken()
                    }
                    else -> Unit
                }
            }

            _ui.value = UiState(
                initializing = false,
                loggedIn = loggedIn,
                serverInput = displayServerUrl(activeUrl),
                serverUrl = activeUrl,
                servers = servers,
                accounts = accounts,
                currentUsername = if (loggedIn) {
                    prefs.getCurrentUsername() ?: last?.username.orEmpty()
                } else "",
                token = token,
                webUserSeed = userSeed,
                initialUsername = last?.username.orEmpty(),
                initialPassword = last?.let { prefs.decryptPassword(it) }.orEmpty(),
                rememberPassword = last?.rememberPassword == true
            )
        }
    }

    // ── 登录页输入 ───────────────────────────────────────────────

    /** 用户手动输入服务器地址：原始文本照存，另外维护一份规范化的地址 */
    fun setServerInput(raw: String) {
        val normalized = normalizeServerUrl(raw)
        val changed = normalized != _ui.value.serverUrl
        _ui.update { it.copy(serverInput = raw, serverUrl = normalized) }
        if (!changed || normalized.isBlank()) return
        // 输入的是以前连过的服务器，把它的账号带出来。
        // 打字过程中地址会不断变化，这里做 300ms 防抖，避免每敲一个字符都读一次 DataStore。
        accountsJob?.cancel()
        accountsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            val accounts = accountsOf(normalized)
            _ui.update { current ->
                if (current.serverUrl == normalized) current.copy(accounts = accounts) else current
            }
        }
    }

    /** 在登录页的服务器列表里选中一台 */
    fun selectServer(server: ServerProfile) {
        viewModelScope.launch {
            ApiClient.setBaseUrl(server.serverUrl)
            prefs.setActiveServerUrl(server.serverUrl)
            prefs.touchServer(server.id)
            val accounts = accountsOf(server.serverUrl)
            val servers = prefs.getServers()
            val last = accounts.firstOrNull()
            val lastPassword = last?.let { acc -> prefs.decryptPassword(acc) }.orEmpty()
            _ui.update {
                it.copy(
                    serverInput = displayServerUrl(server.serverUrl),
                    serverUrl = server.serverUrl,
                    servers = servers,
                    accounts = accounts,
                    initialUsername = last?.username.orEmpty(),
                    initialPassword = lastPassword,
                    rememberPassword = last?.rememberPassword == true,
                    loginState = LoginState.Idle,
                    revision = it.revision + 1
                )
            }
        }
    }

    // ── 登录 / 退出 ─────────────────────────────────────────────

    /**
     * 登录。服务器地址以界面上当前输入的内容为准，可以是任意地址。
     *
     * 用户没写协议时会自动试 http / https 两个候选，
     * 内网优先 http、公网域名优先 https。
     */
    fun login(serverInput: String, username: String, password: String, rememberPassword: Boolean) {
        val accountName = username.trim()
        val candidates = serverCandidates(serverInput)
        val error = when {
            serverInput.isBlank() -> "请填写服务器地址"
            candidates.isEmpty() -> "服务器地址格式不正确，例如 192.168.1.10:3000 或 https://mp.example.com"
            accountName.isBlank() -> "请填写用户名"
            password.isEmpty() -> "请填写密码"
            else -> null
        }
        if (error != null) {
            _ui.update { it.copy(loginState = LoginState.Error(error)) }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(loginState = LoginState.Loading) }

            var usedUrl: String? = null
            var response: LoginResponse? = null
            var lastError = "登录失败"

            for (candidate in candidates) {
                ApiClient.setBaseUrl(candidate)
                val outcome = runCatching {
                    withTimeoutOrNull(12_000) { authRepository.login(accountName, password) }
                }.getOrNull()

                if (outcome == null) {
                    lastError = "连接超时，请检查网络或服务器地址"
                    continue
                }
                val ok = outcome.getOrNull()
                if (ok != null) {
                    usedUrl = candidate
                    response = ok
                    break
                }
                val cause = outcome.exceptionOrNull()
                lastError = cause?.let { describe(it) } ?: "登录失败"
                // 服务器已经应答（401/403/404/5xx）说明地址是对的，不用再换协议
                if (cause is retrofit2.HttpException) break
            }

            val url = usedUrl
            val loginResult = response
            if (url == null || loginResult == null) {
                _ui.update { it.copy(loginState = LoginState.Error(lastError)) }
                return@launch
            }

            val token = loginResult.accessToken
            ApiClient.setBaseUrl(url)
            ApiClient.setAuthToken(token)
            prefs.saveAuthToken(token)
            prefs.saveCurrentUsername(accountName)

            // 登录响应里通常已经带齐 super_user / permissions；
            // 老版本没带的话再补一次 /user/current，否则网页版会落到「应用中心」
            var userSeed = WebUserSeed.fromLogin(loginResult).orEmpty()
            if (userSeed.isBlank()) {
                userSeed = withTimeoutOrNull(5_000) { authRepository.loadSession() }
                    ?.userSeed
                    .orEmpty()
            }

            val encPassword = if (rememberPassword) prefs.encryptPassword(password) else ""
            prefs.upsertAccount(url, accountName, rememberPassword, encPassword)
            val server = prefs.upsertServer(url)
            prefs.setActiveServerUrl(server.serverUrl)

            // 换账号必须清掉 WebView 里的旧会话，否则网页版会沿用上一个账号
            clearWebSession()

            val accounts = accountsOf(url)
            val servers = prefs.getServers()
            _ui.update {
                it.copy(
                    loggedIn = true,
                    serverInput = displayServerUrl(url),
                    serverUrl = url,
                    servers = servers,
                    accounts = accounts,
                    currentUsername = accountName,
                    token = token,
                    webUserSeed = userSeed,
                    loginState = LoginState.Idle,
                    panelVisible = false,
                    sessionEpoch = it.sessionEpoch + 1,
                    initialUsername = accountName,
                    initialPassword = if (rememberPassword) password else "",
                    rememberPassword = rememberPassword,
                    revision = it.revision + 1
                )
            }
        }
    }

    /** 退出登录：清空会话回到登录页，保留当前服务器与账号用于预填 */
    fun logout() {
        viewModelScope.launch {
            val account = _ui.value.accounts.find {
                it.username.equals(_ui.value.currentUsername, ignoreCase = true)
            }
            val rememberedPassword = account?.let { acc -> prefs.decryptPassword(acc) }.orEmpty()
            clearSession()
            _ui.update {
                it.copy(
                    loggedIn = false,
                    token = "",
                    webUserSeed = "",
                    currentUsername = "",
                    panelVisible = false,
                    loginState = LoginState.Idle,
                    initialUsername = account?.username.orEmpty(),
                    initialPassword = rememberedPassword,
                    rememberPassword = account?.rememberPassword == true,
                    revision = it.revision + 1
                )
            }
        }
    }

    fun switchServer(server: ServerProfile) {
        viewModelScope.launch {
            clearSession()
            ApiClient.setBaseUrl(server.serverUrl)
            prefs.setActiveServerUrl(server.serverUrl)
            prefs.touchServer(server.id)
            val accounts = accountsOf(server.serverUrl)
            val servers = prefs.getServers()
            val last = accounts.firstOrNull()
            val lastPassword = last?.let { acc -> prefs.decryptPassword(acc) }.orEmpty()
            _ui.update {
                it.copy(
                    loggedIn = false,
                    serverInput = displayServerUrl(server.serverUrl),
                    serverUrl = server.serverUrl,
                    servers = servers,
                    accounts = accounts,
                    currentUsername = "",
                    token = "",
                    webUserSeed = "",
                    panelVisible = false,
                    loginState = LoginState.Idle,
                    initialUsername = last?.username.orEmpty(),
                    initialPassword = lastPassword,
                    rememberPassword = last?.rememberPassword == true,
                    revision = it.revision + 1
                )
            }
        }
    }

    fun switchAccount(account: AccountProfile) {
        viewModelScope.launch {
            clearSession()
            ApiClient.setBaseUrl(account.serverUrl)
            prefs.setActiveServerUrl(account.serverUrl)
            prefs.touchAccount(account.id)
            val accounts = accountsOf(account.serverUrl)
            val servers = prefs.getServers()
            val savedPassword = prefs.decryptPassword(account).orEmpty()
            _ui.update {
                it.copy(
                    loggedIn = false,
                    serverInput = displayServerUrl(account.serverUrl),
                    serverUrl = account.serverUrl,
                    servers = servers,
                    accounts = accounts,
                    currentUsername = "",
                    token = "",
                    webUserSeed = "",
                    panelVisible = false,
                    loginState = LoginState.Idle,
                    initialUsername = account.username,
                    initialPassword = savedPassword,
                    rememberPassword = account.rememberPassword,
                    revision = it.revision + 1
                )
            }
        }
    }

    /** 新增账号：回到登录页，服务器保持不变，账号密码清空 */
    fun prepareNewAccount() {
        viewModelScope.launch {
            clearSession()
            _ui.update {
                it.copy(
                    loggedIn = false,
                    token = "",
                    webUserSeed = "",
                    currentUsername = "",
                    panelVisible = false,
                    loginState = LoginState.Idle,
                    initialUsername = "",
                    initialPassword = "",
                    rememberPassword = false,
                    revision = it.revision + 1
                )
            }
        }
    }

    // ── 服务器 / 账号管理 ────────────────────────────────────────

    /**
     * 切换到用户输入的任意地址：存进历史列表并回到登录页。
     * 地址解析不出来返回 false（此时不改任何状态）。
     */
    fun connectToServer(rawUrl: String, name: String = ""): Boolean {
        val url = parseServerUrl(rawUrl) ?: return false
        viewModelScope.launch {
            val server = prefs.upsertServer(url, name.trim())
            clearSession()
            prefs.setActiveServerUrl(server.serverUrl)
            ApiClient.setBaseUrl(server.serverUrl)
            val accounts = accountsOf(server.serverUrl)
            val servers = prefs.getServers()
            val last = accounts.firstOrNull()
            val lastPassword = last?.let { acc -> prefs.decryptPassword(acc) }.orEmpty()
            _ui.update {
                it.copy(
                    loggedIn = false,
                    serverInput = displayServerUrl(server.serverUrl),
                    serverUrl = server.serverUrl,
                    servers = servers,
                    accounts = accounts,
                    currentUsername = "",
                    token = "",
                    webUserSeed = "",
                    panelVisible = false,
                    loginState = LoginState.Idle,
                    initialUsername = last?.username.orEmpty(),
                    initialPassword = lastPassword,
                    rememberPassword = last?.rememberPassword == true,
                    revision = it.revision + 1
                )
            }
        }
        return true
    }

    fun deleteServer(id: String) {
        viewModelScope.launch {
            val deleted = _ui.value.servers.find { it.id == id } ?: return@launch
            val wasActive = deleted.serverUrl == _ui.value.serverUrl
            prefs.deleteServer(id)
            val servers = prefs.getServers()
            val next = servers.firstOrNull()
            val nextAccounts = next?.let { accountsOf(it.serverUrl) }.orEmpty()
            if (wasActive) {
                clearSession()
                ApiClient.setBaseUrl(next?.serverUrl.orEmpty())
                _ui.update {
                    it.copy(
                        loggedIn = false,
                        servers = servers,
                        // 删光了就是空，回到「自己输地址」的状态
                        serverInput = displayServerUrl(next?.serverUrl.orEmpty()),
                        serverUrl = next?.serverUrl.orEmpty(),
                        accounts = nextAccounts,
                        currentUsername = "",
                        token = "",
                        webUserSeed = "",
                        panelVisible = false,
                        initialUsername = "",
                        initialPassword = "",
                        rememberPassword = false,
                        revision = it.revision + 1
                    )
                }
            } else {
                _ui.update { it.copy(servers = servers) }
            }
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            val account = _ui.value.accounts.find { it.id == id } ?: return@launch
            val isCurrent = account.username.equals(_ui.value.currentUsername, ignoreCase = true)
            prefs.deleteAccount(id)
            val remaining = accountsOf(account.serverUrl)
            if (isCurrent && _ui.value.loggedIn) {
                clearSession()
                _ui.update {
                    it.copy(
                        loggedIn = false,
                        currentUsername = "",
                        token = "",
                        webUserSeed = "",
                        panelVisible = false,
                        accounts = remaining,
                        revision = it.revision + 1
                    )
                }
            } else {
                _ui.update { it.copy(accounts = remaining) }
            }
        }
    }

    // ── 面板 ─────────────────────────────────────────────────────

    fun setPanelVisible(visible: Boolean) {
        if (visible == _ui.value.panelVisible) return
        if (visible) viewModelScope.launch {
            // 打开面板时刷新一下服务器列表，防止在其他地方新增后没有同步
            val servers = prefs.getServers()
            _ui.update { it.copy(servers = servers, panelVisible = true) }
        } else {
            _ui.update { it.copy(panelVisible = false) }
        }
    }

    fun passwordOf(account: AccountProfile): String = prefs.decryptPassword(account).orEmpty()

    suspend fun fabPosition(): Pair<Float, Float>? = prefs.getFabPosition()

    fun saveFabPosition(x: Float, y: Float) {
        viewModelScope.launch { prefs.saveFabPosition(x, y) }
    }

    // ── 内部 ─────────────────────────────────────────────────────

    private suspend fun accountsOf(serverUrl: String): List<AccountProfile> {
        val normalized = normalizeServerUrl(serverUrl)
        return prefs.getAccounts()
            .filter { it.serverUrl == normalized }
            .sortedByDescending { it.lastUsedAt }
    }

    private suspend fun clearSession() {
        runCatching { prefs.clearAuthToken() }
        runCatching { prefs.clearCurrentUsername() }
        ApiClient.setAuthToken("")
        ApiClient.clearCredentials()
        clearWebSession()
    }

    private fun clearWebSession() {
        runCatching {
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
            }
        }
        runCatching { WebStorage.getInstance().deleteAllData() }
    }

    private fun describe(error: Throwable): String {
        var cause: Throwable? = error
        while (cause != null) {
            when (cause) {
                is UnknownHostException -> return "无法解析服务器地址"
                is ConnectException -> return "无法连接服务器，请检查地址与端口"
                is SocketTimeoutException -> return "连接超时，请检查网络"
                is retrofit2.HttpException -> return when (cause.code()) {
                    401 -> "账号或密码错误"
                    403 -> "账号被禁用或没有权限"
                    404 -> "服务器地址不是 MoviePilot 服务"
                    in 500..599 -> "服务器内部错误（${cause.code()}）"
                    else -> "登录失败（HTTP ${cause.code()}）"
                }
            }
            cause = cause.cause
        }
        return error.message ?: "登录失败"
    }
}
