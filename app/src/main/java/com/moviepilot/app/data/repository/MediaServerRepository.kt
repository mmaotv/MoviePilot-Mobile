package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaServerRepository @Inject constructor() {

    private val apiService get() = ApiClient.getApiService()

    suspend fun getMediaServerLibraries(): Result<List<Map<String, Any>>> {
        return try {
            Result.success(apiService.getMediaServerLibraries())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayUrl(itemId: String): Result<PlayUrl> {
        return try {
            Result.success(apiService.getPlayUrl(itemId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkLocalExists(
        title: String?,
        year: String?,
        mtype: String?,
        tmdbId: Int?,
        season: Int?
    ): Result<ExistsResult> {
        return try {
            Result.success(apiService.checkLocalExists(title, year, mtype, tmdbId, season))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkRemoteExists(mediaInfo: MediaInfo): Result<ExistsResult> {
        return try {
            Result.success(apiService.checkRemoteExists(mediaInfo))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotExistsInfo(mediaInfo: MediaInfo): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.getNotExistsInfo(mediaInfo))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestItems(server: String, count: Int? = 20): Result<List<MediaServerItem>> {
        return try {
            Result.success(apiService.getLatestItems(server, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayingItems(server: String, count: Int? = 12): Result<List<MediaServerItem>> {
        return try {
            Result.success(apiService.getPlayingItems(server, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableServers(): Result<List<Map<String, Any>>> {
        return try {
            Result.success(apiService.getAvailableServers())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
