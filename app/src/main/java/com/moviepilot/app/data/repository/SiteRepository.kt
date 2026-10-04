package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteRepository @Inject constructor() {

    private val apiService get() = ApiClient.getApiService()

    suspend fun getSites(): Result<List<SiteInfo>> {
        return try {
            Result.success(apiService.getSites())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSite(site: SiteInfo): Result<Unit> {
        return try {
            val response = apiService.addSite(site)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("添加站点失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSite(site: SiteInfo): Result<Unit> {
        return try {
            val response = apiService.updateSite(site)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("更新站点失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSite(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteSite(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("删除站点失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSiteStatistics(): Result<List<SiteStatistic>> {
        return try {
            Result.success(apiService.getSiteStatistics())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSiteRSS(siteId: Int): Result<List<TorrentSearchResult>> {
        return try {
            Result.success(apiService.getSiteRSS(siteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testSite(site: SiteTestRequest): Result<SiteTestResult> {
        return try {
            Result.success(apiService.testSite(site))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBrushTasks(): Result<List<BrushTask>> {
        return try {
            Result.success(apiService.getBrushTasks())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addBrushTask(task: BrushTask): Result<Unit> {
        return try {
            val response = apiService.addBrushTask(task)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("添加刷流任务失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBrushTask(task: BrushTask): Result<Unit> {
        return try {
            val response = apiService.updateBrushTask(task)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("更新刷流任务失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBrushTask(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteBrushTask(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("删除刷流任务失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCookieCloudSites(): Result<List<SiteInfo>> {
        return try {
            Result.success(apiService.getCookieCloudSites())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCookieCloud(): Result<Unit> {
        return try {
            val response = apiService.updateCookieCloud()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("更新CookieCloud失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
