package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import com.moviepilot.app.data.network.ApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor() {
    
    private val apiService: ApiService
        get() = ApiClient.getApiService()

    suspend fun getDownloadTasks(): List<DownloadTask> {
        return apiService.getDownloadTasks()
    }

    suspend fun getTransferHistory(page: Int = 1, count: Int = 30): TransferHistoryWrapper {
        return apiService.getTransferHistory(page, count)
    }

    suspend fun getCurrentDownloads(): List<DownloadTask> {
        return apiService.getCurrentDownloads()
    }

    suspend fun getDownloadHistory(page: Int, count: Int): List<DownloadTask> {
        return apiService.getDownloadHistory(page, count)
    }

    suspend fun getDownloadStats(): DownloaderInfo {
        return apiService.getDownloadStats()
    }

    suspend fun startDownload(download: Map<String, Any>): Response<Unit> {
        return apiService.startDownload(download)
    }

    suspend fun stopDownload(download: Map<String, Any>): Response<Unit> {
        return apiService.stopDownload(download)
    }

    suspend fun deleteDownload(downloadId: String): Response<Unit> {
        return apiService.deleteDownload(downloadId)
    }

    suspend fun testDownloadClient(client: Map<String, Any>): Response<Unit> {
        return apiService.testDownloadClient(client)
    }
}
