package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Storage: {total_storage: float, used_storage: float}
 */
data class StorageInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("total_storage") val totalStorage: Double?,
    @SerializedName("used_storage") val usedStorage: Double?
)

/**
 * Statistic: {movie_count: int, tv_count: int, episode_count: int, user_count: int}
 */
data class MediaStatistics(
    @SerializedName("movie_count") val movieCount: Int?,
    @SerializedName("tv_count") val tvCount: Int?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("user_count") val userCount: Int?
)

/**
 * Downloader info: single object with speeds/sizes/free_space
 */
data class DownloaderInfo(
    @SerializedName("download_speed") val downloadSpeed: Double?,
    @SerializedName("upload_speed") val uploadSpeed: Double?,
    @SerializedName("download_size") val downloadSize: Double?,
    @SerializedName("upload_size") val uploadSize: Double?,
    @SerializedName("free_space") val freeSpace: Double?
)

/**
 * Schedule info item
 */
data class ScheduleInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("provider") val provider: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("next_run") val nextRun: String?
)

/**
 * CPU: returns raw float (e.g. 19.9)
 * Memory: returns List<int> (e.g. [546340864, 6])
 * Transfer: returns List<int> (e.g. [3, 6, 3, 4, 4, 2, 2, 6])
 */
data class DashboardData(
    val storage: StorageInfo? = null,
    val statistics: MediaStatistics? = null,
    val downloaderInfo: DownloaderInfo? = null,
    val cpuUsage: Float? = null,
    val memoryUsage: List<Int>? = null,
    val transferStats: List<Int>? = null,
    val schedule: List<ScheduleInfo>? = null
)
