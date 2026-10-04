package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * TMDB related models
 */
data class TmdbSeasonDetail(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("season_number") val seasonNumber: Int?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("episode_count") val episodeCount: Int?
)

data class TmdbEpisode(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("episode_number") val episodeNumber: Int?,
    @SerializedName("season_number") val seasonNumber: Int?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("vote_average") val voteAverage: Float?,
    @SerializedName("vote_count") val voteCount: Int?
)

data class TmdbPerson(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("character") val character: String?,
    @SerializedName("job") val job: String?,
    @SerializedName("department") val department: String?,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("order") val order: Int?
)

data class TrendingItem(
    @SerializedName("id") val id: Int?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Float?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?
)

data class BangumiCalendarItem(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("name_cn") val nameCn: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("air_weekday") val airWeekday: Int?,
    @SerializedName("weekday") val weekday: String?,
    @SerializedName("images") val images: BangumiImages?,
    @SerializedName("image") val image: String?,
    @SerializedName("thumb") val thumb: String?,
    @SerializedName("duration") val duration: String?,
    @SerializedName("eps") val eps: Int?
)

data class BangumiImages(
    @SerializedName("large") val large: String?,
    @SerializedName("medium") val medium: String?,
    @SerializedName("small") val small: String?
)

data class DoubanHotItem(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("rate") val rate: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("types") val types: List<String>?,
    @SerializedName("regions") val regions: List<String>?,
    @SerializedName("actors") val actors: List<String>?,
    @SerializedName("directors") val directors: List<String>?,
    @SerializedName("is_tv") val isTv: Boolean?
)
