package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import com.moviepilot.app.data.network.ApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscribeRepository @Inject constructor() {
    
    private val apiService: ApiService
        get() = ApiClient.getApiService()

    suspend fun getSubscriptions(type: String? = null): Result<List<Subscribe>> {
        return try {
            Result.success(apiService.getSubscriptions(type))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubscription(id: Int): Result<Subscribe> {
        return try {
            Result.success(apiService.getSubscription(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubscribesWithToken(token: String): Result<List<Subscribe>> {
        return try {
            Result.success(apiService.getSubscribesWithToken(token))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubscribeByMediaId(mediaId: String, season: Int? = null): Result<Subscribe?> {
        return try {
            Result.success(apiService.getSubscribeByMediaId(mediaId, season))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSubscribes(username: String): Result<List<Subscribe>> {
        return try {
            Result.success(apiService.getUserSubscribes(username))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubscribeHistory(mtype: String): Result<List<Subscribe>> {
        return try {
            Result.success(apiService.getSubscribeHistory(mtype))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularSubscribes(): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.getPopularSubscribes())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSharedSubscribes(
        name: String? = null,
        page: Int? = 1,
        count: Int? = 30
    ): Result<List<Subscribe>> {
        return try {
            Result.success(apiService.getSharedSubscribes(name, page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubscribeShareStatistics(): Result<MediaStatistics> {
        return try {
            Result.success(apiService.getSubscribeShareStatistics())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSubscription(subscription: SubscribeRequest): Result<Unit> {
        return try {
            val response = apiService.addSubscription(subscription)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("添加订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubscription(subscription: SubscribeRequest): Result<Unit> {
        return try {
            val response = apiService.updateSubscription(subscription)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("更新订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubscription(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteSubscription(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("删除订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubscribeByMediaId(mediaId: String, season: Int? = null): Result<Unit> {
        return try {
            val response = apiService.deleteSubscribeByMediaId(mediaId, season)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("删除订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubscribeHistory(historyId: Int): Result<Unit> {
        return try {
            val response = apiService.deleteSubscribeHistory(historyId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("删除历史失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubscriptionStatus(id: Int, state: String): Result<Unit> {
        return try {
            val response = apiService.updateSubscriptionStatus(id, state)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("更新状态失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSubscribes(): Result<Unit> {
        return try {
            val response = apiService.refreshSubscribes()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("刷新订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetSubscribe(subId: Int): Result<Unit> {
        return try {
            val response = apiService.resetSubscribe(subId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("重置订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkSubscribeTMDB(): Result<Unit> {
        return try {
            val response = apiService.checkSubscribeTMDB()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("检查TMDB失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchAllSubscribes(): Result<Unit> {
        return try {
            val response = apiService.searchAllSubscribes()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("搜索订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchSubscribe(id: Int): Result<Unit> {
        return try {
            val response = apiService.searchSubscribe(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("搜索订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareSubscribe(share: Map<String, Any>): Result<Unit> {
        return try {
            val response = apiService.shareSubscribe(share)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("分享订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubscribeShare(shareId: Int): Result<Unit> {
        return try {
            val response = apiService.deleteSubscribeShare(shareId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("删除分享失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forkSubscribe(fork: Map<String, Any>): Result<Unit> {
        return try {
            val response = apiService.forkSubscribe(fork)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Fork订阅失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowedUsers(): Result<List<String>> {
        return try {
            Result.success(apiService.getFollowedUsers())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun followUser(data: Map<String, String>): Result<Unit> {
        return try {
            val response = apiService.followUser(data)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("关注用户失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(data: Map<String, String>): Result<Unit> {
        return try {
            val response = apiService.unfollowUser(data)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("取消关注失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
