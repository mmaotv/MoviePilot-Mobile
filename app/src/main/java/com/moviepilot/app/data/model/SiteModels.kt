package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Site management extended models
 */
data class BrushTask(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("site") val site: Int,
    @SerializedName("downloader") val downloader: String?,
    @SerializedName("interval") val interval: Int,
    @SerializedName("state") val state: Boolean,
    @SerializedName("rss_rule") val rssRule: Map<String, Any>?,
    @SerializedName("remove_rule") val removeRule: Map<String, Any>?,
    @SerializedName("savepath_rule") val savepathRule: Map<String, Any>?,
    @SerializedName("label_rule") val labelRule: Map<String, Any>?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class CookieCloudConfig(
    @SerializedName("server") val server: String?,
    @SerializedName("key") val key: String?,
    @SerializedName("password") val password: String?
)

data class SiteTestResult(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("response_time") val responseTime: Float?
)

data class SiteTestRequest(
    @SerializedName("id") val id: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("cookie") val cookie: String?
)

data class SiteStatistic(
    @SerializedName("site") val site: String,
    @SerializedName("upload") val upload: Long?,
    @SerializedName("download") val download: Long?,
    @SerializedName("ratio") val ratio: Float?,
    @SerializedName("seeding") val seeding: Int?,
    @SerializedName("leeching") val leeching: Int?,
    @SerializedName("bonus") val bonus: Float?,
    @SerializedName("update_time") val updateTime: String?
)
