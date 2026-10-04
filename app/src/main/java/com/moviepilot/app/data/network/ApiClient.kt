package com.moviepilot.app.data.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 运行时可重建的 API 客户端单例。
 *
 * - baseUrl / authToken 变更时调用对应 setter，内部懒重建 ApiService。
 * - Hilt NetworkModule 直接委托此对象，不再维护独立的 Retrofit 实例。
 * - credentials 只保存在内存中（磁盘侧由 PreferencesManager + AES 加密存储）。
 */
object ApiClient {
    private const val DEFAULT_URL = "http://YOUR_SERVER_IP:PORT/"

    @Volatile private var _baseUrl: String = DEFAULT_URL
    @Volatile private var _authToken: String? = null
    @Volatile private var _username: String? = null
    @Volatile private var _password: String? = null
    @Volatile private var _apiService: ApiService? = null

    // ── URL ──────────────────────────────────────────────────────

    fun setBaseUrl(url: String) {
        val normalized = url.trimEnd('/') + "/"
        if (_baseUrl != normalized) {
            _baseUrl = normalized
            invalidate()
        }
    }

    fun getBaseUrl(): String = _baseUrl

    // ── Token ────────────────────────────────────────────────────

    fun setAuthToken(token: String) {
        if (_authToken != token) {
            _authToken = token.ifBlank { null }
            invalidate()
        }
    }

    fun getAuthToken(): String? = _authToken

    // ── Credentials（内存缓存，不写磁盘）─────────────────────────

    fun setCredentials(username: String, password: String) {
        _username = username
        _password = password
    }

    fun getCredentials(): Pair<String, String>? =
        if (_username != null && _password != null) Pair(_username!!, _password!!) else null

    fun clearCredentials() {
        _username = null
        _password = null
    }

    // ── ApiService ───────────────────────────────────────────────

    /** 强制下次调用 getApiService() 时重建实例 */
    private fun invalidate() {
        _apiService = null
    }

    fun getApiService(): ApiService = _apiService ?: buildApiService().also { _apiService = it }

    private fun buildApiService(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val auth = Interceptor { chain ->
            val req = chain.request().newBuilder().apply {
                _authToken?.let { addHeader("Authorization", "Bearer $it") }
                addHeader("Content-Type", "application/json")
            }.build()
            chain.proceed(req)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(_baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// ── Hilt 模块：委托 ApiClient，使用 Provider 确保始终获取最新实例 ──────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    fun provideApiService(): ApiService = ApiClient.getApiService()
}
