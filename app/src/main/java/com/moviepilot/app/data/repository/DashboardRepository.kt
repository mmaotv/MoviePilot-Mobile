package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import com.moviepilot.app.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor() {
    
    private val apiService: ApiService
        get() = ApiClient.getApiService()


    suspend fun getStorageInfo(): Result<StorageInfo> {
        return try {
            Result.success(apiService.getStorageInfo())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMediaStatistics(): Result<MediaStatistics> {
        return try {
            Result.success(apiService.getMediaStatistics())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDownloaderInfo(): Result<DownloaderInfo> {
        return try {
            Result.success(apiService.getDownloaderInfo())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCpuUsage(): Result<Float> {
        return try {
            Result.success(apiService.getCpuUsage())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMemoryUsage(): Result<List<Int>> {
        return try {
            Result.success(apiService.getMemoryUsage())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransferStats(): Result<List<Int>> {
        return try {
            Result.success(apiService.getTransferStats())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSchedule(): Result<List<ScheduleInfo>> {
        return try {
            Result.success(apiService.getSchedule())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extended dashboard methods
    suspend fun getDashboardStatistics(): Result<MediaStatistics> {
        return try {
            Result.success(apiService.getDashboardStatistics())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDashboardStorages(): Result<List<StorageInfo>> {
        return try {
            Result.success(apiService.getDashboardStorages())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSchedulerJobs(): Result<List<ScheduleInfo>> {
        return try {
            Result.success(apiService.getSchedulerJobs())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProcesses(): Result<List<ProcessInfo>> {
        return try {
            Result.success(apiService.getProcesses())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDashboardData(): Result<DashboardData> {
        return try {
            val storage = getStorageInfo().getOrNull()
            val statistics = getMediaStatistics().getOrNull()
            val downloaderInfo = getDownloaderInfo().getOrNull()
            val cpuUsage = getCpuUsage().getOrNull()
            val memoryUsage = getMemoryUsage().getOrNull()
            val transferStats = getTransferStats().getOrNull()
            val schedule = getSchedule().getOrNull()

            Result.success(
                DashboardData(
                    storage = storage,
                    statistics = statistics,
                    downloaderInfo = downloaderInfo,
                    cpuUsage = cpuUsage,
                    memoryUsage = memoryUsage,
                    transferStats = transferStats,
                    schedule = schedule
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class DashboardData(
    val storage: StorageInfo?,
    val statistics: MediaStatistics?,
    val downloaderInfo: DownloaderInfo?,
    val cpuUsage: Float?,
    val memoryUsage: List<Int>?,
    val transferStats: List<Int>?,
    val schedule: List<ScheduleInfo>?
)
