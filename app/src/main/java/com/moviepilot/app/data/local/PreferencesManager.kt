package com.moviepilot.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.moviepilot.app.data.model.AccountProfile
import com.moviepilot.app.data.model.ServerProfile
import com.moviepilot.app.data.model.displayServerUrl
import com.moviepilot.app.data.model.normalizeServerUrl
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
 * 本地配置存储。
 *
 * - 服务器列表、账号列表分开存（同一服务器可多账号）
 * - token 只保存当前会话
 * - 密码用 Android Keystore + AES-GCM 加密
 * - 首次启动会把 v2 的 server_profiles 迁移到新结构
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val SERVERS = stringPreferencesKey("mp_servers")
        private val ACCOUNTS = stringPreferencesKey("mp_accounts")
        private val ACTIVE_SERVER = stringPreferencesKey("mp_active_server")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val CURRENT_USER = stringPreferencesKey("mp_current_user")
        private val MIGRATED = booleanPreferencesKey("mp_migrated_v3")
        private val NAME_FIXED = booleanPreferencesKey("mp_names_fixed")

        private val FAB_POS_X = floatPreferencesKey("fab_pos_x")
        private val FAB_POS_Y = floatPreferencesKey("fab_pos_y")
        private val FAB_CUSTOMIZED = booleanPreferencesKey("fab_customized")

        // ── v2 旧键，仅用于迁移 ──────────────────────────────────
        private val LEGACY_PROFILES = stringPreferencesKey("server_profiles")
        private val LEGACY_SERVER_URL = stringPreferencesKey("server_url")
        private val LEGACY_USERNAME = stringPreferencesKey("username")
        private val LEGACY_PASSWORD = stringPreferencesKey("password_enc")
        private val LEGACY_REMEMBER = booleanPreferencesKey("remember_password")

        private const val KEYSTORE_ALIAS = "moviepilot_cred_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private val gson = Gson()

        private data class LegacyProfile(
            val id: String? = null,
            val name: String? = null,
            @SerializedName("server_url") val serverUrl: String? = null,
            val username: String? = null,
            @SerializedName("remember_password") val rememberPassword: Boolean = false,
            @SerializedName("enc_password") val encPassword: String? = null,
            @SerializedName("is_default") val isDefault: Boolean = false,
            @SerializedName("last_used_at") val lastUsedAt: Long = 0L
        )
    }

    // ── Keystore ─────────────────────────────────────────────────

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

    private fun encrypt(plaintext: String): String = runCatching {
        val key = getOrCreateSecretKey() ?: return ""
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        "${Base64.encodeToString(iv, Base64.NO_WRAP)}:${Base64.encodeToString(cipherBytes, Base64.NO_WRAP)}"
    }.getOrDefault("")

    private fun decrypt(encoded: String): String? = runCatching {
        val parts = encoded.split(":")
        if (parts.size != 2) return null
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }.getOrNull()

    fun encryptPassword(password: String): String = encrypt(password)

    fun decryptPassword(account: AccountProfile): String? {
        if (!account.rememberPassword || account.encPassword.isBlank()) return null
        return runCatching { decrypt(account.encPassword) }.getOrNull()
    }

    // ── 服务器 ───────────────────────────────────────────────────

    suspend fun getServers(): List<ServerProfile> {
        val json = runCatching {
            context.dataStore.data.map { prefs -> prefs[SERVERS] }.first()
        }.getOrNull()
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<ServerProfile>>() {}.type
        return runCatching { gson.fromJson<List<ServerProfile>>(json, type) }
            .getOrNull()
            ?.sortedByDescending { it.lastUsedAt }
            ?: emptyList()
    }

    suspend fun saveServers(servers: List<ServerProfile>) {
        context.dataStore.edit { it[SERVERS] = gson.toJson(servers) }
    }

    suspend fun upsertServer(url: String, name: String = ""): ServerProfile {
        val normalized = normalizeServerUrl(url)
        val servers = getServers().toMutableList()
        val existing = servers.find { it.serverUrl == normalized }
        if (existing != null) {
            val updated = existing.copy(
                name = name.ifBlank { existing.name },
                lastUsedAt = System.currentTimeMillis()
            )
            servers[servers.indexOf(existing)] = updated
            saveServers(servers)
            return updated
        }
        val created = ServerProfile(
            name = name.ifBlank { displayServerUrl(normalized) },
            serverUrl = normalized
        )
        servers.add(created)
        saveServers(servers)
        return created
    }

    suspend fun deleteServer(id: String) {
        val server = getServers().find { it.id == id } ?: return
        saveServers(getServers().filterNot { it.id == id })
        saveAccounts(getAccounts().filterNot { it.serverUrl == server.serverUrl })
        if (getActiveServerUrl() == server.serverUrl) {
            setActiveServerUrl(getServers().firstOrNull()?.serverUrl)
        }
    }

    suspend fun touchServer(id: String) {
        val servers = getServers().toMutableList()
        val idx = servers.indexOfFirst { it.id == id }
        if (idx >= 0) {
            servers[idx] = servers[idx].copy(lastUsedAt = System.currentTimeMillis())
            saveServers(servers)
        }
    }

    suspend fun getActiveServerUrl(): String? = runCatching {
        context.dataStore.data.map { it[ACTIVE_SERVER] }.first()
    }.getOrNull()

    suspend fun setActiveServerUrl(url: String?) {
        context.dataStore.edit {
            if (url.isNullOrBlank()) it.remove(ACTIVE_SERVER) else it[ACTIVE_SERVER] = url
        }
    }

    // ── 账号 ─────────────────────────────────────────────────────

    suspend fun getAccounts(): List<AccountProfile> {
        val json = runCatching {
            context.dataStore.data.map { prefs -> prefs[ACCOUNTS] }.first()
        }.getOrNull()
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<AccountProfile>>() {}.type
        return runCatching { gson.fromJson<List<AccountProfile>>(json, type) }
            .getOrNull()
            ?: emptyList()
    }

    suspend fun saveAccounts(accounts: List<AccountProfile>) {
        context.dataStore.edit { it[ACCOUNTS] = gson.toJson(accounts) }
    }

    /** 同一服务器 + 同一用户名视为同一个账号，只更新密码与使用时间 */
    suspend fun upsertAccount(
        serverUrl: String,
        username: String,
        rememberPassword: Boolean,
        encPassword: String
    ) {
        val normalized = normalizeServerUrl(serverUrl)
        val accounts = getAccounts().toMutableList()
        val existing = accounts.find {
            it.serverUrl == normalized && it.username.equals(username, ignoreCase = true)
        }
        val now = System.currentTimeMillis()
        if (existing != null) {
            accounts[accounts.indexOf(existing)] = existing.copy(
                username = username,
                rememberPassword = rememberPassword,
                encPassword = encPassword,
                lastUsedAt = now
            )
        } else {
            accounts.add(
                AccountProfile(
                    serverUrl = normalized,
                    username = username,
                    rememberPassword = rememberPassword,
                    encPassword = encPassword,
                    lastUsedAt = now
                )
            )
        }
        saveAccounts(accounts)
    }

    suspend fun touchAccount(id: String) {
        val accounts = getAccounts().toMutableList()
        val idx = accounts.indexOfFirst { it.id == id }
        if (idx >= 0) {
            accounts[idx] = accounts[idx].copy(lastUsedAt = System.currentTimeMillis())
            saveAccounts(accounts)
        }
    }

    suspend fun deleteAccount(id: String) {
        saveAccounts(getAccounts().filterNot { it.id == id })
    }

    // ── 当前会话 ──────────────────────────────────────────────────

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { it[AUTH_TOKEN] = token }
    }

    suspend fun getAuthToken(): String? = runCatching {
        context.dataStore.data.map { it[AUTH_TOKEN] }.first()
    }.getOrNull()

    suspend fun clearAuthToken() {
        context.dataStore.edit { it.remove(AUTH_TOKEN) }
    }

    suspend fun saveCurrentUsername(username: String) {
        context.dataStore.edit { it[CURRENT_USER] = username }
    }

    suspend fun getCurrentUsername(): String? = runCatching {
        context.dataStore.data.map { it[CURRENT_USER] }.first()
    }.getOrNull()

    suspend fun clearCurrentUsername() {
        context.dataStore.edit { it.remove(CURRENT_USER) }
    }

    // ── 悬浮球位置 ────────────────────────────────────────────────

    suspend fun saveFabPosition(x: Float, y: Float) {
        context.dataStore.edit {
            it[FAB_POS_X] = x.coerceIn(0f, 1f)
            it[FAB_POS_Y] = y.coerceIn(0f, 1f)
            it[FAB_CUSTOMIZED] = true
        }
    }

    suspend fun getFabPosition(): Pair<Float, Float>? = runCatching {
        context.dataStore.data.map { prefs ->
            if (prefs[FAB_CUSTOMIZED] == true) {
                Pair(prefs[FAB_POS_X] ?: 0.86f, prefs[FAB_POS_Y] ?: 0.80f)
            } else null
        }.first()
    }.getOrNull()

    // ── v2 → v3 迁移 ─────────────────────────────────────────────

    /**
     * 只在首次启动执行一次：
     * 旧的 server_profiles 里，每条记录带一个账号，拆成「服务器 + 账号」两条数据。
     *
     * 注意：不再预置任何服务器地址——用户输入什么就连什么。
     */
    suspend fun migrateIfNeeded() {
        val done = runCatching {
            context.dataStore.data.map { it[MIGRATED] ?: false }.first()
        }.getOrDefault(false)
        if (done) return

        if (getServers().isEmpty()) {
            val legacyJson = runCatching {
                context.dataStore.data.map { it[LEGACY_PROFILES] }.first()
            }.getOrNull()

            val legacy: List<LegacyProfile> = if (legacyJson.isNullOrBlank()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<LegacyProfile>>() {}.type
                runCatching { gson.fromJson<List<LegacyProfile>>(legacyJson, type) }
                    .getOrNull()
                    ?: emptyList()
            }

            if (legacy.isNotEmpty()) {
                legacy.forEach { old ->
                    val url = normalizeServerUrl(old.serverUrl ?: return@forEach)
                    if (url.isEmpty()) return@forEach
                    upsertServer(url, old.name ?: "")
                    val user = old.username.orEmpty()
                    if (user.isNotBlank()) {
                        upsertAccount(
                            serverUrl = url,
                            username = user,
                            rememberPassword = old.rememberPassword,
                            encPassword = old.encPassword.orEmpty()
                        )
                    }
                }
            } else {
                // 更早的单服务器配置
                val oldUrl = runCatching {
                    context.dataStore.data.map { it[LEGACY_SERVER_URL] }.first()
                }.getOrNull()
                val oldUser = runCatching {
                    context.dataStore.data.map { it[LEGACY_USERNAME] }.first()
                }.getOrNull()
                val oldPwd = runCatching {
                    context.dataStore.data.map { it[LEGACY_PASSWORD] }.first()
                }.getOrNull()
                val oldRemember = runCatching {
                    context.dataStore.data.map { it[LEGACY_REMEMBER] ?: false }.first()
                }.getOrDefault(false)

                if (!oldUrl.isNullOrBlank()) {
                    val url = normalizeServerUrl(oldUrl)
                    upsertServer(url, "")
                    if (!oldUser.isNullOrBlank() && !oldPwd.isNullOrBlank()) {
                        upsertAccount(url, oldUser, oldRemember, oldPwd)
                    }
                }
            }
        }

        if (getActiveServerUrl() == null) {
            getServers().firstOrNull()?.let { setActiveServerUrl(it.serverUrl) }
        }

        context.dataStore.edit { it[MIGRATED] = true }
        normalizeServerNames()
    }

    /**
     * 3.0.0 早期版本给首次安装塞过一条名字为「V3 服务器 / V2 服务器」的内置项，
     * 看起来像写死的地址。这里把这些名字换成实际地址，列表就是纯粹的用户记录。
     */
    suspend fun normalizeServerNames() {
        val done = runCatching {
            context.dataStore.data.map { it[NAME_FIXED] ?: false }.first()
        }.getOrDefault(false)
        if (done) return

        val servers = getServersRaw()
        val renamed = servers.map { server ->
            if (server.name == "V3 服务器" || server.name == "V2 服务器") {
                server.copy(name = displayServerUrl(server.serverUrl))
            } else {
                server
            }
        }
        if (renamed != servers) saveServers(renamed)
        context.dataStore.edit { it[NAME_FIXED] = true }
    }

    /** 不排序的原始列表，仅内部写回时用，避免排序结果影响 index 语义 */
    private suspend fun getServersRaw(): List<ServerProfile> {
        val json = runCatching {
            context.dataStore.data.map { prefs -> prefs[SERVERS] }.first()
        }.getOrNull()
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<ServerProfile>>() {}.type
        return runCatching { gson.fromJson<List<ServerProfile>>(json, type) }
            .getOrNull()
            ?: emptyList()
    }
}
