package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

// New search result model matching actual API response
data class TorrentSearchResult(
    @SerializedName("meta_info") val metaInfo: MetaInfo?,
    @SerializedName("media_info") val mediaInfo: SearchMediaInfo?,
    @SerializedName("torrent_info") val torrentInfo: TorrentInfoDetail
)

data class MetaInfo(
    @SerializedName("isfile") val isFile: Boolean?,
    @SerializedName("org_string") val orgString: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("cn_name") val cnName: String?,
    @SerializedName("en_name") val enName: String?,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("total_season") val totalSeason: Int?,
    @SerializedName("begin_season") val beginSeason: Int?,
    @SerializedName("end_season") val endSeason: Int?,
    @SerializedName("total_episode") val totalEpisode: Int?,
    @SerializedName("begin_episode") val beginEpisode: Int?,
    @SerializedName("end_episode") val endEpisode: Int?,
    @SerializedName("season_episode") val seasonEpisode: String?,
    @SerializedName("resource_type") val resourceType: String?,
    @SerializedName("resource_pix") val resourcePix: String?,
    @SerializedName("resource_team") val resourceTeam: String?,
    @SerializedName("resource_effect") val resourceEffect: String?,
    @SerializedName("video_encode") val videoEncode: String?,
    @SerializedName("video_bit") val videoBit: String?,
    @SerializedName("audio_encode") val audioEncode: String?,
    @SerializedName("edition") val edition: String?,
    @SerializedName("customization") val customization: String?,
    @SerializedName("part") val part: String?,
    @SerializedName("fps") val fps: String?,
    @SerializedName("web_source") val webSource: String?,
    @SerializedName("apply_words") val applyWords: List<String>?,
    @SerializedName("episode_list") val episodeList: List<String>?,
    @SerializedName("tmdbid") val tmdbId: Any?,
    @SerializedName("doubanid") val doubanId: Any?
)

data class SearchMediaInfo(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Float?
)

data class TorrentInfoDetail(
    @SerializedName("site") val site: Int,
    @SerializedName("site_name") val siteName: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("enclosure") val enclosure: String?,
    @SerializedName("page_url") val pageUrl: String?,
    @SerializedName("size") val size: Long,
    @SerializedName("seeders") val seeders: Int,
    @SerializedName("peers") val peers: Int,
    @SerializedName("grabs") val grabs: Int?,
    @SerializedName("pubdate") val pubDate: String?,
    @SerializedName("date_elapsed") val dateElapsed: String?,
    @SerializedName("freedate") val freeDate: String?,
    @SerializedName("uploadvolumefactor") val uploadVolumeFactor: Float?,
    @SerializedName("downloadvolumefactor") val downloadVolumeFactor: Float?,
    @SerializedName("hit_and_run") val hitAndRun: Boolean?,
    @SerializedName("labels") val labels: List<String>?,
    @SerializedName("category") val category: String?,
    @SerializedName("imdbid") val imdbId: String?,
    @SerializedName("volume_factor") val volumeFactor: String?,
    @SerializedName("freedate_diff") val freeDateDiff: String?
)

// Legacy model - keep for compatibility
data class TorrentInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int?,
    @SerializedName("season") val season: String?,
    @SerializedName("episode") val episode: String?,
    @SerializedName("site") val site: String,
    @SerializedName("seeders") val seeders: Int,
    @SerializedName("peers") val peers: Int,
    @SerializedName("leechers") val leechers: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("pubdate") val publishDate: String?,
    @SerializedName("video_codec") val videoCodec: String?,
    @SerializedName("resolution") val resolution: String?,
    @SerializedName("quality") val quality: String?,
    @SerializedName("release_group") val releaseGroup: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("size") val size: Long,
    @SerializedName("freedate") val freeDate: String?,
    @SerializedName("promotion_factor") val promotionFactor: String?,
    @SerializedName("upload_limit") val uploadLimit: Int?,
    @SerializedName("download_limit") val downloadLimit: Int?,
    @SerializedName("hit_and_run") val hitAndRun: Boolean?,
    @SerializedName("labels") val labels: List<String>?,
    @SerializedName("imdb_id") val imdbId: String?,
    @SerializedName("tmdb_id") val tmdbId: Int?,
    @SerializedName("douban_id") val doubanId: String?,
    @SerializedName("page_url") val pageUrl: String?,
    @SerializedName("enclosure") val enclosure: String?,
    @SerializedName("org_url") val orgUrl: String?
)

data class MediaInfo(
    @SerializedName("id") val id: Int?,
    @SerializedName("title") val title: String,
    @SerializedName("year") val year: Int?,
    @SerializedName("type") val type: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("season") val season: Int?,
    @SerializedName("episode") val episode: Int?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Float?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    @SerializedName("original_language") val originalLanguage: String?,
    @SerializedName("popularity") val popularity: Float?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?
)

data class DiscoverFilter(
    @SerializedName("type") val type: String = "MOV",
    @SerializedName("sort") val sort: String = "tmdb.popular",
    @SerializedName("tags") val tags: String = "",
    @SerializedName("area") val area: String = "",
    @SerializedName("year") val year: String = "",
    @SerializedName("season") val season: Int? = null,
    @SerializedName("begin_season") val beginSeason: Int? = null,
    @SerializedName("end_season") val endSeason: Int? = null
)
