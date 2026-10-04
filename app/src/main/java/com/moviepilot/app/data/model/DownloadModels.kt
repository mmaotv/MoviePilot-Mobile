package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Download task model matching actual MoviePilot API response
 * GET /api/v1/download/ returns List<DownloadTask>
 */
data class DownloadTask(
    @SerializedName("downloader") val downloader: String?,
    @SerializedName("hash") val hash: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("season_episode") val seasonEpisode: String?,
    @SerializedName("size") val size: Long?,
    @SerializedName("progress") val progress: Float?,
    @SerializedName("state") val state: String?,
    @SerializedName("upspeed") val upspeed: String?,
    @SerializedName("dlspeed") val dlspeed: String?,
    @SerializedName("tags") val tags: String?,
    @SerializedName("media") val media: DownloadMediaInfo?,
    @SerializedName("userid") val userId: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("left_time") val leftTime: String?
)

data class DownloadMediaInfo(
    @SerializedName("tmdbid") val tmdbId: Int?,
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("season") val season: String?,
    @SerializedName("episode") val episode: String?,
    @SerializedName("image") val image: String?
)

/**
 * Transfer history model matching actual API response
 * The API wraps it in {success: true, data: {list: [...], total: N}}
 */
data class TransferHistoryWrapper(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: TransferHistoryData?
)

data class TransferHistoryData(
    @SerializedName("list") val list: List<TransferHistory>?,
    @SerializedName("total") val total: Int?
)

data class TransferHistory(
    @SerializedName("id") val id: Int?,
    @SerializedName("src") val src: String?,
    @SerializedName("dest") val dest: String?,
    @SerializedName("mode") val mode: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("tmdbid") val tmdbId: Int?,
    @SerializedName("imdbid") val imdbId: String?,
    @SerializedName("tvdbid") val tvdbId: Int?,
    @SerializedName("doubanid") val doubanId: String?,
    @SerializedName("seasons") val seasons: String?,
    @SerializedName("episodes") val episodes: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("download_hash") val downloadHash: String?,
    @SerializedName("episode_group") val episodeGroup: String?,
    @SerializedName("status") val status: Boolean?,
    @SerializedName("errmsg") val errorMsg: String?,
    @SerializedName("date") val date: String?
)

/**
 * Transfer and storage related models
 */
data class RenamePreview(
    @SerializedName("file") val file: FileItem?,
    @SerializedName("src_file") val srcFile: FileItem?,
    @SerializedName("dest_file") val destFile: String?,
    @SerializedName("media_info") val mediaInfo: MediaInfo?,
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?
)

data class FileItem(
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("path") val path: String,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("modified") val modified: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("mimetype") val mimetype: String? = null,
    @SerializedName("storage") val storage: String? = null
)

data class ManualTransferRequest(
    @SerializedName("fileitem") val fileitem: FileItem,
    @SerializedName("target") val target: String?,
    @SerializedName("settings") val settings: Map<String, Any>?
)

data class TransferResult(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("file") val file: String?,
    @SerializedName("target") val target: String?
)

data class TransferConfig(
    @SerializedName("movie_path") val moviePath: String?,
    @SerializedName("tv_path") val tvPath: String?,
    @SerializedName("anime_path") val animePath: String?,
    @SerializedName("category") val category: List<String>?
)

data class DirectoryContent(
    @SerializedName("files") val files: List<FileItem>,
    @SerializedName("total") val total: Int
)

data class StorageUsage(
    @SerializedName("total") val total: Long,
    @SerializedName("used") val used: Long,
    @SerializedName("free") val free: Long,
    @SerializedName("percentage") val percentage: Float
) {
    val usedStr: String get() = formatSize(used)
    val totalStr: String get() = formatSize(total)
    val percent: Float get() = percentage

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
}

enum class StorageTransType {
    @SerializedName("copy") COPY,
    @SerializedName("move") MOVE,
    @SerializedName("link") LINK,
    @SerializedName("softlink") SOFTLINK,
    @SerializedName("rclone_copy") RCLONE_COPY,
    @SerializedName("rclone_move") RCLONE_MOVE
}
