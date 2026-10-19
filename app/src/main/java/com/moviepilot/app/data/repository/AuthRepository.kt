package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.LoginResponse
import com.moviepilot.app.data.model.WebUserSeed
import com.moviepilot.app.data.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {

    enum class TokenState { Valid, Invalid, Unknown }

    /**
     * [userSeed] 是写进网页版 `user` store 的 JSON，token 有效时才有。
     */
    data class Session(
        val state: TokenState,
        val userSeed: String? = null
    )

    private val apiService get() = ApiClient.getApiService()

    suspend fun login(username: String, password: String): Result<LoginResponse> = runCatching {
        apiService.login(username, password)
    }

    /**
     * 区分「token 失效」与「服务器暂时不可达」：
     * 前者要清掉本地登录态，后者要保留，等网络恢复还能继续用。
     *
     * 顺带把用户信息取回来，供网页版登录态还原使用。
     */
    suspend fun loadSession(): Session = runCatching {
        val response = apiService.currentUser()
        when {
            response.isSuccessful -> Session(
                state = TokenState.Valid,
                userSeed = WebUserSeed.fromCurrentUser(response.body())
            )

            response.code() == 401 || response.code() == 403 -> Session(TokenState.Invalid)

            // 老版本没有 /user/current，退回旧的校验点，只能判断有效性
            response.code() == 404 -> {
                val legacy = apiService.checkToken()
                when {
                    legacy.isSuccessful -> Session(TokenState.Valid)
                    legacy.code() == 401 || legacy.code() == 403 -> Session(TokenState.Invalid)
                    else -> Session(TokenState.Unknown)
                }
            }

            else -> Session(TokenState.Unknown)
        }
    }.getOrDefault(Session(TokenState.Unknown))
}
