package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import com.moviepilot.app.data.network.ApiService
import com.moviepilot.app.data.network.DownloadRequest
import com.moviepilot.app.data.network.MediaIn
import com.moviepilot.app.data.network.TorrentIn
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceRepository @Inject constructor() {
    
    private val apiService: ApiService
        get() = ApiClient.getApiService()


    /**
     * 下载种子 —— 发送到服务器下载器
     *
     * v3.1.0 接口变更：`POST /api/v1/download/` 的请求体字段为
     *   torrent_in : 种子信息（必填）
     *   media_in   : 媒体信息（必填）
     * 而非旧版的 torrent_info / media_info。
     * 另外必须使用强类型 data class —— `Map<String, Any>` 作为 @Body
     * 会让 Retrofit 生成含通配符的参数类型并抛
     * "Parameter type must not include a type variable or wildcard"。
     */
    suspend fun downloadTorrent(torrent: TorrentSearchResult): Result<Unit> {
        return try {
            val info = torrent.torrentInfo
            val meta = torrent.metaInfo

            // media_in 为必填，优先用媒体信息，否则从 meta_info 兜底构造
            val mediaIn = torrent.mediaInfo?.toMediaIn()
                ?: MediaIn(
                    title = meta?.title ?: meta?.name ?: info.title,
                    type = meta?.type ?: "未知",
                    year = meta?.year,
                    season = meta?.beginSeason ?: 0,
                    episode = meta?.beginEpisode ?: 0,
                    resourcePix = meta?.resourcePix,
                    resourceType = meta?.resourceType,
                    videoEncode = meta?.videoEncode
                )

            val body = DownloadRequest(
                torrentIn = TorrentIn(
                    site = info.site,
                    siteName = info.siteName,
                    title = info.title,
                    enclosure = info.enclosure,
                    pageUrl = info.pageUrl,
                    size = info.size,
                    seeders = info.seeders,
                    peers = info.peers,
                    description = info.description,
                    pubDate = info.pubDate,
                    volumeFactor = info.volumeFactor,
                    hitAndRun = info.hitAndRun ?: false,
                    labels = info.labels ?: emptyList(),
                    uploadVolumeFactor = info.uploadVolumeFactor ?: 1.0f,
                    downloadVolumeFactor = info.downloadVolumeFactor ?: 1.0f,
                    category = info.category
                ),
                mediaIn = mediaIn
            )

            val response = apiService.addDownloadTask(body)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception("HTTP ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SearchMediaInfo.toMediaIn() = MediaIn(
        id = id,
        title = title ?: "",
        year = year,
        type = type,
        posterPath = posterPath,
        voteAverage = voteAverage
    )

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
            // v3.1.0：该接口直接返回数组（非 {success,data} 包装）
            Result.success(apiService.getLastSearchResults())
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
