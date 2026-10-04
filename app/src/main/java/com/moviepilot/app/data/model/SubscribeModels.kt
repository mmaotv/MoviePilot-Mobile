package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

data class Subscribe(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("year") val year: JsonElement?,
    @SerializedName("type") val type: String,
    @SerializedName("tmdbid") val tmdbId: Int?,
    @SerializedName("doubanid") val doubanId: String?,
    @SerializedName("bangumiid") val bangumiId: Int?,
    @SerializedName("mediaid") val mediaId: Int?,
    @SerializedName("season") val season: Int?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("backdrop") val backdrop: String?,
    @SerializedName("vote") val vote: Float?,
    @SerializedName("description") val description: String?,
    @SerializedName("filter") val filter: String?,
    @SerializedName("include") val include: String?,
    @SerializedName("exclude") val exclude: String?,
    @SerializedName("quality") val quality: String?,
    @SerializedName("resolution") val resolution: String?,
    @SerializedName("effect") val effect: String?,
    @SerializedName("total_episode") val totalEpisode: Int?,
    @SerializedName("start_episode") val startEpisode: Int?,
    @SerializedName("lack_episode") val lackEpisode: Int?,
    @SerializedName("note") val note: JsonElement?,
    @SerializedName("state") val state: String?,
    @SerializedName("last_update") val lastUpdate: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("sites") val sites: List<Int>?,
    @SerializedName("downloader") val downloader: String?,
    @SerializedName("best_version") val bestVersion: Int?,
    @SerializedName("current_priority") val currentPriority: Int?,
    @SerializedName("save_path") val savePath: String?,
    @SerializedName("search_imdbid") val searchImdbId: Int?,
    @SerializedName("date") val date: String?,
    @SerializedName("custom_words") val customWords: String?,
    @SerializedName("media_category") val mediaCategory: String?,
    @SerializedName("filter_groups") val filterGroups: List<String>?,
    @SerializedName("episode_group") val episodeGroup: String?
) {
    // Helper to get year as string
    val yearValue: String?
        get() = when (year) {
            is JsonPrimitive -> year.asString
            else -> null
        }

    // Helper to get note as list of episodes
    val noteEpisodes: List<Int>?
        get() = when (note) {
            is com.google.gson.JsonArray -> note?.asJsonArray?.map { it.asInt }
            else -> null
        }
}

data class SubscribeRequest(
    @SerializedName("name") val name: String,
    @SerializedName("year") val year: Int?,
    @SerializedName("type") val type: String,
    @SerializedName("tmdbid") val tmdbId: Int?,
    @SerializedName("doubanid") val doubanId: String?,
    @SerializedName("season") val season: Int?,
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("resolution") val resolution: String? = null,
    @SerializedName("effect") val effect: String? = null,
    @SerializedName("include") val include: String? = null,
    @SerializedName("exclude") val exclude: String? = null,
    @SerializedName("sites") val sites: List<Int>? = null,
    @SerializedName("total_episode") val totalEpisode: Int? = null,
    @SerializedName("start_episode") val startEpisode: Int? = null,
    @SerializedName("save_path") val savePath: String? = null
)
