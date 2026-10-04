package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Media Server models (Emby/Plex/Jellyfin)
 */
data class PlayUrl(
    @SerializedName("url") val url: String,
    @SerializedName("type") val type: String?
)

data class ExistsResult(
    @SerializedName("exists") val exists: Boolean,
    @SerializedName("seasons") val seasons: List<Int>?,
    @SerializedName("episodes") val episodes: List<Int>?
)

data class MediaServerItem(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("tmdb_id") val tmdbId: Int?,
    @SerializedName("imdb_id") val imdbId: String?,
    @SerializedName("tvdb_id") val tvdbId: Int?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("backdrop") val backdrop: String?,
    @SerializedName("rating") val rating: Float?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("play_count") val playCount: Int?,
    @SerializedName("last_played") val lastPlayed: String?
)

data class MediaServerLibrary(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("count") val count: Int
)

data class MediaServerUser(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("admin") val admin: Boolean?
)
