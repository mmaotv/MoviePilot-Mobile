package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor() {

    private val apiService get() = ApiClient.getApiService()

    suspend fun sendWebMessage(message: Message): Result<Unit> {
        return try {
            val response = apiService.sendWebMessage(message)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("发送消息失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWebMessages(page: Int? = 1, count: Int? = 20): Result<List<Message>> {
        return try {
            Result.success(apiService.getWebMessages(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun subscribeWebPush(subscription: Map<String, Any>): Result<Unit> {
        return try {
            val response = apiService.subscribeWebPush(subscription)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("订阅推送失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendWebPushNotification(payload: Map<String, Any>): Result<Unit> {
        return try {
            val response = apiService.sendWebPushNotification(payload)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("发送推送失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
