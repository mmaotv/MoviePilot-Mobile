package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Torrent Cache models
 */
data class TorrentCacheItem(
    @SerializedName("domain") val domain: String,
    @SerializedName("torrent_hash") val torrentHash: String,
    @SerializedName("title") val title: String,
    @SerializedName("enclosure") val enclosure: String?,
    @SerializedName("size") val size: Long?,
    @SerializedName("seeders") val seeders: Int?,
    @SerializedName("peers") val peers: Int?,
    @SerializedName("pubdate") val pubDate: String?,
    @SerializedName("volume_factor") val volumeFactor: String?,
    @SerializedName("hit_and_run") val hitAndRun: Boolean?,
    @SerializedName("labels") val labels: List<String>?,
    @SerializedName("cache_time") val cacheTime: String?
)

data class TorrentCacheRefreshResult(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("count") val count: Int?
)
