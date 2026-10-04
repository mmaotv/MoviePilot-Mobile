package com.moviepilot.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.moviepilot.app.data.model.ServerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * 使用 Android Keystore + AES-GCM 对敏感字段进行加密存储。
 * 密钥由系统硬件/TEE 管理，应用外无法导出。
 * 支持多服务器配置（ServerProfile JSON 数组）。
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USERNAME = stringPreferencesKey("username")
        /** 存储格式：Base64(IV) + ":" + Base64(密文) */
        val PASSWORD = stringPreferencesKey("password_enc")
        const val DEFAULT_SERVER_URL = "http://YOUR_SERVER_IP:PORT/"

        /** 全局记住密码开关 */
        val REMEMBER_PASSWORD = booleanPreferencesKey("remember_password")

        /** 多服务器列表：JSON 数组 */
        val SERVER_PROFILES = stringPreferencesKey("server_profiles")
        /** 当前激活的服务器 ID */
        val ACTIVE_SERVER_ID = stringPreferencesKey("active_server_id")
        
        /** 主题设置：0=系统默认, 1=浅色, 2=深色 */
        val THEME_MODE = stringPreferencesKey("theme_mode")
        
        /** 通知开关 */
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val DOWNLOAD_NOTIFICATION_ENABLED = booleanPreferencesKey("download_notification_enabled")
        val NEW_CONTENT_NOTIFICATION_ENABLED = booleanPreferencesKey("new_content_notification_enabled")
        val ALERT_NOTIFICATION_ENABLED = booleanPreferencesKey("alert_notification_enabled")

        /** 悬浮按钮位置 */
        val FAB_POS_X = floatPreferencesKey("fab_pos_x")
        val FAB_POS_Y = floatPreferencesKey("fab_pos_y")
        /** 悬浮按钮是否已手动调整过位置 */
        val FAB_CUSTOMIZED = booleanPreferencesKey("fab_customized")

        private const val KEYSTORE_ALIAS = "moviepilot_cred_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private val gson = Gson()
    }

    // ── Keystore 密钥管理 ──────────────────────────────────────────

    private fun getOrCreateSecretKey(): SecretKey? = runCatching {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        keyGen.generateKey()
    }.getOrNull()

    /**
     * 加密明文，返回 Base64(IV):Base64(密文)
     * 失败时返回空字符串（密码不保存，跳过记住密码功能）
     */
    private fun encrypt(plaintext: String): String = runCatching {
        val key = getOrCreateSecretKey() ?: return ""
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        "${Base64.encodeToString(iv, Base64.NO_WRAP)}:${Base64.encodeToString(cipherBytes, Base64.NO_WRAP)}"
    }.getOrDefault("")

    /** 解密 Base64(IV):Base64(密文)，失败时返回 null */
    private fun decrypt(encoded: String): String? = runCatching {
        val parts = encoded.split(":")
        if (parts.size != 2) return null
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }.getOrNull()

    // ── 服务器地址（兼容旧版单服务器）────────────────────────────────

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
        }
    }

    val serverUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SERVER_URL] ?: DEFAULT_SERVER_URL
    }

    suspend fun getServerUrl(): String {
        return context.dataStore.data.map { preferences ->
            preferences[SERVER_URL] ?: DEFAULT_SERVER_URL
        }.first()
    }

    // ── Token ─────────────────────────────────────────────────────

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }

    suspend fun getAuthToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN]
        }.first()
    }

    suspend fun clearAuthToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // ── 记住密码标志 ────────────────────────────────────────────────

    suspend fun saveRememberPassword(remember: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMEMBER_PASSWORD] = remember
        }
    }

    suspend fun getRememberPassword(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[REMEMBER_PASSWORD] ?: false
        }.first()
    }

    // ── 用户名 & 密码（密码 AES-GCM 加密）───────────────────────────

    suspend fun saveCredentials(username: String, password: String) {
        val encryptedPassword = encrypt(password)
        context.dataStore.edit { preferences ->
            preferences[USERNAME] = username
            preferences[PASSWORD] = encryptedPassword
        }
    }

    suspend fun getCredentials(): Pair<String, String>? {
        return context.dataStore.data.map { preferences ->
            val u = preferences[USERNAME]
            val enc = preferences[PASSWORD]
            if (u != null && enc != null) {
                val p = decrypt(enc) ?: return@map null
                Pair(u, p)
            } else null
        }.first()
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(USERNAME)
            preferences.remove(PASSWORD)
        }
    }

    // ── 多服务器配置（ServerProfile 列表）─────────────────────────────

    suspend fun getServerProfiles(): List<ServerProfile> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[SERVER_PROFILES]
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<ServerProfile>>() {}.type
                gson.fromJson<List<ServerProfile>>(json, type) ?: emptyList()
            }
        }.first()
    }

    suspend fun saveServerProfiles(profiles: List<ServerProfile>) {
        val json = gson.toJson(profiles)
        context.dataStore.edit { preferences ->
            preferences[SERVER_PROFILES] = json
        }
    }

    suspend fun addServerProfile(profile: ServerProfile) {
        val profiles = getServerProfiles().toMutableList()
        // 如果设为默认，清除其他默认标记
        if (profile.isDefault) {
            profiles.replaceAll { it.copy(isDefault = false) }
        }
        profiles.add(profile)
        saveServerProfiles(profiles)
    }

    suspend fun updateServerProfile(profile: ServerProfile) {
        val profiles = getServerProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profile.id }
        if (idx >= 0) {
            // 如果设为默认，清除其他默认标记
            if (profile.isDefault) {
                profiles.replaceAll { it.copy(isDefault = false) }
            }
            profiles[idx] = profile
            saveServerProfiles(profiles)
        }
    }

    suspend fun deleteServerProfile(profileId: String) {
        val profiles = getServerProfiles().filter { it.id != profileId }
        saveServerProfiles(profiles)
        // 如果删除的是当前激活的，清除 activeServerId
        val activeId = getActiveServerId()
        if (activeId == profileId) {
            setActiveServerId(null)
        }
    }

    suspend fun getActiveServerId(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[ACTIVE_SERVER_ID]
        }.first()
    }

    suspend fun setActiveServerId(id: String?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[ACTIVE_SERVER_ID] = id
            } else {
                preferences.remove(ACTIVE_SERVER_ID)
            }
        }
    }

    /**
     * 获取默认服务器，优先返回 isDefault 的，否则返回第一个
     */
    suspend fun getDefaultServerProfile(): ServerProfile? {
        val profiles = getServerProfiles()
        return profiles.find { it.isDefault } ?: profiles.firstOrNull()
    }

    /**
     * 迁移旧版单服务器配置到多服务器列表
     * 只在列表为空时执行一次
     * 失败时静默跳过，不影响应用启动
     */
    suspend fun migrateLegacyServer(): ServerProfile? = runCatching {
        val profiles = getServerProfiles()
        if (profiles.isNotEmpty()) return null

        val url = getServerUrl()
        val credentials = getCredentials()
        if (url == DEFAULT_SERVER_URL && credentials == null) return null

        val rememberPwd = getRememberPassword()
        val encPwd = if (rememberPwd && credentials != null) encrypt(credentials.second) else ""

        val profile = ServerProfile(
            name = "默认服务器",
            serverUrl = url,
            username = credentials?.first ?: "",
            rememberPassword = rememberPwd,
            encPassword = encPwd,
            isDefault = true
        )
        addServerProfile(profile)
        setActiveServerId(profile.id)
        profile
    }.getOrNull()

    /**
     * 根据 ServerProfile 获取解密后的密码
     * 仅当 rememberPassword=true 且 encPassword 非空时返回
     * 解密失败时返回 null，不影响应用运行
     */
    fun decryptServerPassword(profile: ServerProfile): String? {
        if (!profile.rememberPassword || profile.encPassword.isBlank()) return null
        return runCatching { decrypt(profile.encPassword) }.getOrNull()
    }

    /**
     * 加密密码，用于存入 ServerProfile
     */
    fun encryptPassword(password: String): String = encrypt(password)
    
    // ── 主题设置 ────────────────────────────────────────────────
    
    enum class ThemeMode(val value: String) {
        SYSTEM("0"),
        LIGHT("1"),
        DARK("2");
        
        companion object {
            fun fromValue(value: String): ThemeMode = entries.find { it.value == value } ?: LIGHT
        }
    }
    
    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.value
        }
    }
    
    suspend fun getThemeMode(): ThemeMode {
        return context.dataStore.data.map { preferences ->
            ThemeMode.fromValue(preferences[THEME_MODE] ?: ThemeMode.LIGHT.value)
        }.first()
    }
    
    // ── 通知设置 ────────────────────────────────────────────────
    
    suspend fun saveNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_ENABLED] = enabled
        }
    }
    
    suspend fun getNotificationEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[NOTIFICATION_ENABLED] ?: true
        }.first()
    }
    
    suspend fun saveDownloadNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_NOTIFICATION_ENABLED] = enabled
        }
    }
    
    suspend fun getDownloadNotificationEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[DOWNLOAD_NOTIFICATION_ENABLED] ?: true
        }.first()
    }
    
    suspend fun saveNewContentNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NEW_CONTENT_NOTIFICATION_ENABLED] = enabled
        }
    }
    
    suspend fun getNewContentNotificationEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[NEW_CONTENT_NOTIFICATION_ENABLED] ?: true
        }.first()
    }
    
    suspend fun saveAlertNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALERT_NOTIFICATION_ENABLED] = enabled
        }
    }
    
    suspend fun getAlertNotificationEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[ALERT_NOTIFICATION_ENABLED] ?: true
        }.first()
    }

    // ── 悬浮按钮位置 ────────────────────────────────────────────────

    /**
     * 保存悬浮按钮位置
     * @param x 相对屏幕宽度的比例 (0.0 - 1.0)
     * @param y 相对屏幕高度的比例 (0.0 - 1.0)
     */
    suspend fun saveFabPosition(x: Float, y: Float) {
        context.dataStore.edit { preferences ->
            preferences[FAB_POS_X] = x.coerceIn(0f, 1f)
            preferences[FAB_POS_Y] = y.coerceIn(0f, 1f)
            preferences[FAB_CUSTOMIZED] = true
        }
    }

    /**
     * 获取悬浮按钮位置
     * @return Pair(x比例, y比例)，如果未设置则返回 null
     */
    suspend fun getFabPosition(): Pair<Float, Float>? {
        return context.dataStore.data.map { preferences ->
            if (preferences[FAB_CUSTOMIZED] == true) {
                val x = preferences[FAB_POS_X] ?: 0.85f  // 默认靠右
                val y = preferences[FAB_POS_Y] ?: 0.85f  // 默认靠下
                Pair(x, y)
            } else {
                null
            }
        }.first()
    }

    /**
     * 检查悬浮按钮是否已手动调整过
     */
    suspend fun isFabCustomized(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[FAB_CUSTOMIZED] ?: false
        }.first()
    }
}
