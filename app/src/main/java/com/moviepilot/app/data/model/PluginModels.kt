package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

data class Plugin(
    @SerializedName("id") val id: String,
    @SerializedName("plugin_name") val pluginName: String,
    @SerializedName("plugin_desc") val pluginDesc: String,
    @SerializedName("plugin_version") val pluginVersion: String,
    @SerializedName("plugin_icon") val pluginIcon: String,
    @SerializedName("plugin_author") val pluginAuthor: String,
    @SerializedName("author_url") val authorUrl: String?,
    @SerializedName("plugin_level") val pluginLevel: Int,
    @SerializedName("history") val history: Map<String, String>?,
    @SerializedName("state") val state: Boolean,
    @SerializedName("has_page") val hasPage: Boolean,
    @SerializedName("config") val config: Any?
)

data class SiteInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("pri") val priority: Int,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("limit_interval") val limitInterval: Int?,
    @SerializedName("limit_count") val limitCount: Int?,
    @SerializedName("rss") val rss: Boolean?,
    @SerializedName("cookie") val cookie: String?,
    @SerializedName("token") val token: String?,
    @SerializedName("ua") val ua: String?
)
