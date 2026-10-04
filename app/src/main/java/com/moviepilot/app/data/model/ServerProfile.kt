package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * 服务器配置档案：支持多服务器保存与快速切换
 *
 * 每个档案包含服务器地址 + 可选的登录凭证，
 * 切换时自动恢复该服务器的 baseUrl / token / credentials。
 *
 * rememberPassword: 是否记住密码（用户勾选）
 * encPassword: AES-GCM 加密后的密码，仅当 rememberPassword=true 时存储
 */
data class ServerProfile(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("server_url") val serverUrl: String,
    @SerializedName("username") val username: String = "",
    @SerializedName("remember_password") val rememberPassword: Boolean = false,
    @SerializedName("enc_password") val encPassword: String = "",
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("last_used_at") val lastUsedAt: Long = System.currentTimeMillis()
)
