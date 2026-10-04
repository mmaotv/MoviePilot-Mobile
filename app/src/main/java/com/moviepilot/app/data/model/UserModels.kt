package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * User related models
 */
data class User(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("is_active") val isActive: Boolean?,
    @SerializedName("is_admin") val isAdmin: Boolean?,
    @SerializedName("permission") val permission: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class UserCreate(
    @SerializedName("name") val name: String,
    @SerializedName("password") val password: String,
    @SerializedName("email") val email: String?,
    @SerializedName("is_admin") val isAdmin: Boolean?,
    @SerializedName("permission") val permission: String?
)

data class UserUpdate(
    @SerializedName("name") val name: String,
    @SerializedName("password") val password: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("is_admin") val isAdmin: Boolean?,
    @SerializedName("is_active") val isActive: Boolean?,
    @SerializedName("permission") val permission: String?
)

data class UserConfigDetail(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("is_admin") val isAdmin: Boolean?,
    @SerializedName("permission") val permission: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("user_group") val userGroup: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("last_login") val lastLogin: String?
)
