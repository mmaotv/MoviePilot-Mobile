package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("otp_password") val otpPassword: String? = null
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class UserConfig(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_superuser") val isSuperuser: Boolean,
    @SerializedName("avatar") val avatar: String?
)

data class GlobalConfig(
    @SerializedName("APP_DOMAIN") val appDomain: String? = null,
    @SerializedName("PROXY") val proxy: String? = null,
    @SerializedName("LOGIN_WALLPAPER") val loginWallpaper: String? = null,
    @SerializedName("DEFAULT_MEDIASERVER") val defaultMediaServer: String? = null,
    @SerializedName("MEDIA_SERVER_SYNC_INTERVAL") val mediaServerSyncInterval: Int? = null,
    @SerializedName("API_TOKEN") val apiToken: String? = null
)
