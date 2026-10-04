package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import com.moviepilot.app.data.network.ApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceRepository @Inject constructor() {
    
    private val apiService: ApiService
        get() = ApiClient.getApiService()


    suspend fun downloadTorrent(torrent: TorrentSearchResult): Result<Unit> {
        return try {
            // 构建请求体，把 TorrentSearchResult 转换为 Map
            val body = mutableMapOf<String, Any>()
            body["torrent_info"] = mapOf(
                "site"       to torrent.torrentInfo.site,
                "site_name"  to torrent.torrentInfo.siteName,
                "title"      to torrent.torrentInfo.title,
                "enclosure"  to (torrent.torrentInfo.enclosure ?: ""),
                "page_url"   to (torrent.torrentInfo.pageUrl ?: ""),
                "size"       to torrent.torrentInfo.size,
                "seeders"    to torrent.torrentInfo.seeders,
                "peers"      to torrent.torrentInfo.peers,
                "description" to (torrent.torrentInfo.description ?: ""),
                "pubdate"    to (torrent.torrentInfo.pubDate ?: ""),
                "volume_factor" to (torrent.torrentInfo.volumeFactor ?: ""),
                "hit_and_run" to (torrent.torrentInfo.hitAndRun ?: false),
                "labels"     to (torrent.torrentInfo.labels ?: emptyList<String>()),
                "uploadvolumefactor"   to (torrent.torrentInfo.uploadVolumeFactor ?: 1.0f),
                "downloadvolumefactor" to (torrent.torrentInfo.downloadVolumeFactor ?: 1.0f)
            )
            torrent.metaInfo?.let { meta ->
                body["meta_info"] = mapOf(
                    "title"          to (meta.title ?: ""),
                    "subtitle"       to (meta.subtitle ?: ""),
                    "type"           to (meta.type ?: ""),
                    "name"           to (meta.name ?: ""),
                    "year"           to (meta.year ?: ""),
                    "resource_pix"   to (meta.resourcePix ?: ""),
                    "resource_type"  to (meta.resourceType ?: ""),
                    "video_encode"   to (meta.videoEncode ?: ""),
                    "begin_season"   to (meta.beginSeason ?: 0),
                    "begin_episode"  to (meta.beginEpisode ?: 0)
                )
            }
            torrent.mediaInfo?.let { media ->
                body["media_info"] = mapOf(
                    "tmdb_id"      to (media.id ?: 0),
                    "title"        to (media.title ?: ""),
                    "year"         to (media.year ?: ""),
                    "type"         to (media.type ?: ""),
                    "poster_path"  to (media.posterPath ?: ""),
                    "vote_average" to (media.voteAverage ?: 0.0f)
                )
            }

            val response = apiService.addDownloadTask(body)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("下载失败: HTTP ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchTorrents(keyword: String): Result<List<TorrentSearchResult>> {
        return try {
            val response = apiService.searchTorrents(keyword)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "搜索失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLastSearchResults(): Result<List<TorrentSearchResult>> {
        return try {
            val response = apiService.getLastSearchResults()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByMediaId(
        mediaId: String,
        mtype: String?,
        title: String?,
        year: String?,
        season: String?,
        sites: String? = null
    ): Result<Response<Unit>> {
        return try {
            Result.success(apiService.searchByMediaId(mediaId, mtype, "title", title, year, season, sites))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrending(type: String = "MOV", page: Int = 1): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.getTrending(type, page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNowPlaying(type: String = "MOV", page: Int = 1): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.getNowPlaying(type, page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverMedia(filters: Map<String, String>): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.discoverMedia(filters))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Discovery methods
    suspend fun discoverBangumi(
        type: Int? = 2,
        cat: Int?,
        sort: String? = "rank",
        year: String?,
        page: Int? = 1,
        count: Int? = 30
    ): Result<List<BangumiCalendarItem>> {
        return try {
            Result.success(apiService.discoverBangumi(type, cat, sort, year, page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverDoubanMovies(
        sort: String? = "R",
        tags: String? = "",
        page: Int? = 1,
        count: Int? = 30
    ): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.discoverDoubanMovies(sort, tags, page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverDoubanTVs(
        sort: String? = "R",
        tags: String? = "",
        page: Int? = 1,
        count: Int? = 30
    ): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.discoverDoubanTVs(sort, tags, page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverTMDbMovies(
        sortBy: String? = "popularity.desc",
        withGenres: String? = "",
        withLanguage: String? = "",
        withKeywords: String? = "",
        withProviders: String? = "",
        voteAverage: Float? = 0.0f,
        voteCount: Int? = 0,
        releaseDate: String? = null,
        page: Int? = 1
    ): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.discoverTMDbMovies(
                sortBy, withGenres, withLanguage, withKeywords, withProviders,
                voteAverage, voteCount, releaseDate, page
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverTMDbTVs(
        sortBy: String? = "popularity.desc",
        withGenres: String? = "",
        withLanguage: String? = "",
        withKeywords: String? = "",
        withProviders: String? = "",
        voteAverage: Float? = 0.0f,
        voteCount: Int? = 0,
        releaseDate: String? = null,
        page: Int? = 1
    ): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.discoverTMDbTVs(
                sortBy, withGenres, withLanguage, withKeywords, withProviders,
                voteAverage, voteCount, releaseDate, page
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Recommendation methods
    suspend fun getBangumiCalendar(page: Int? = 1, count: Int? = 30): Result<List<BangumiCalendarItem>> {
        return try {
            Result.success(apiService.getBangumiCalendar(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoubanShowing(page: Int? = 1, count: Int? = 30): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.getDoubanShowing(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoubanMovies(
        sort: String? = "R",
        tags: String? = "",
        page: Int? = 1,
        count: Int? = 30
    ): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.getDoubanMovies(sort, tags, page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoubanTVs(
        sort: String? = "R",
        tags: String? = "",
        page: Int? = 1,
        count: Int? = 30
    ): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.getDoubanTVs(sort, tags, page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoubanTop250(page: Int? = 1, count: Int? = 30): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.getDoubanTop250(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoubanWeeklyChinese(page: Int? = 1, count: Int? = 30): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.getDoubanWeeklyChinese(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoubanWeeklyGlobal(page: Int? = 1, count: Int? = 30): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.getDoubanWeeklyGlobal(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoubanAnimation(page: Int? = 1, count: Int? = 30): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.getDoubanAnimation(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoubanHotMovies(page: Int? = 1, count: Int? = 30): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.getDoubanHotMovies(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoubanHotTVs(page: Int? = 1, count: Int? = 30): Result<List<DoubanHotItem>> {
        return try {
            Result.success(apiService.getDoubanHotTVs(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTMDbTrending(page: Int? = 1): Result<List<TrendingItem>> {
        return try {
            Result.success(apiService.getTMDbTrending(page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTMDbMovies(
        sortBy: String? = "popularity.desc",
        withGenres: String? = "",
        withLanguage: String? = "",
        withKeywords: String? = "",
        withProviders: String? = "",
        voteAverage: Float? = 0.0f,
        voteCount: Int? = 0,
        releaseDate: String? = null,
        page: Int? = 1
    ): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.getTMDbMovies(
                sortBy, withGenres, withLanguage, withKeywords, withProviders,
                voteAverage, voteCount, releaseDate, page
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTMDbTVs(
        sortBy: String? = "popularity.desc",
        withGenres: String? = "",
        withLanguage: String? = "",
        withKeywords: String? = "",
        withProviders: String? = "",
        voteAverage: Float? = 0.0f,
        voteCount: Int? = 0,
        releaseDate: String? = null,
        page: Int? = 1
    ): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.getTMDbTVs(
                sortBy, withGenres, withLanguage, withKeywords, withProviders,
                voteAverage, voteCount, releaseDate, page
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
