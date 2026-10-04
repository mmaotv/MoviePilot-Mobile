package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentCacheRepository @Inject constructor() {

    private val apiService get() = ApiClient.getApiService()

    suspend fun getTorrentCache(): Result<List<TorrentCacheItem>> {
        return try {
            Result.success(apiService.getTorrentCache())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTorrentCache(domain: String, hash: String): Result<Response<Unit>> {
        return try {
            Result.success(apiService.deleteTorrentCache(domain, hash))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearTorrentCache(): Result<Response<Unit>> {
        return try {
            Result.success(apiService.clearTorrentCache())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshTorrentCache(): Result<TorrentCacheRefreshResult> {
        return try {
            Result.success(apiService.refreshTorrentCache())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reidentifyTorrent(
        domain: String,
        hash: String,
        tmdbId: Int?,
        doubanId: String?
    ): Result<Response<Unit>> {
        return try {
            Result.success(apiService.reidentifyTorrent(domain, hash, tmdbId, doubanId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
