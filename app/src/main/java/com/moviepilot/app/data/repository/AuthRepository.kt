package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import com.moviepilot.app.data.network.ApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {
    
    private val apiService: ApiService
        get() = ApiClient.getApiService()

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = apiService.login(username, password)
            Result.success(response.accessToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<UserConfig> {
        return try {
            val user = apiService.getCurrentUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        // Clear local auth data
    }

    suspend fun getUsers(): List<User> {
        return apiService.getUsers()
    }

    suspend fun getUserInfo(): UserConfigDetail {
        return apiService.getUserInfo()
    }

    suspend fun createUser(userCreate: UserCreate): Response<Unit> {
        return apiService.createUser(userCreate)
    }

    suspend fun updateUser(userUpdate: UserUpdate): Response<Unit> {
        return apiService.updateUser(userUpdate)
    }

    suspend fun deleteUser(username: String): Response<Unit> {
        return apiService.deleteUser(username)
    }
}
