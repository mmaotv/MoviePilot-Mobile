package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Discovery and Recommendation models
 * Note: DoubanHotItem, BangumiCalendarItem, TrendingItem are defined in TmdbModels.kt
 */

data class RecommendItem(
    @SerializedName("douban_id") val doubanId: String?,
    @SerializedName("tmdb_id") val tmdbId: Int?,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: String?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("rating") val rating: Float?,
    @SerializedName("reason") val reason: String?
)
