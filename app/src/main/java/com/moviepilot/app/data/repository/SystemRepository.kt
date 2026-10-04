package com.moviepilot.app.data.repository

import com.moviepilot.app.data.model.*
import com.moviepilot.app.data.network.ApiClient
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemRepository @Inject constructor() {

    private val apiService get() = ApiClient.getApiService()

    suspend fun getGlobalConfig(token: String = "moviepilot"): Result<GlobalConfig> {
        return try {
            Result.success(apiService.getGlobalConfig(token))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSystemVersion(): Result<VersionInfo> {
        return try {
            Result.success(apiService.getSystemVersion())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSystemLogging(): Result<List<LogEntry>> {
        return try {
            Result.success(apiService.getSystemLogging())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearSystemLogging(): Result<Unit> {
        return try {
            val response = apiService.clearSystemLogging()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("清除日志失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSystemName(): Result<String> {
        return try {
            Result.success(apiService.getSystemName())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSystemProgress(): Result<Map<String, Float>> {
        return try {
            Result.success(apiService.getSystemProgress())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restartSystem(): Result<Unit> {
        return try {
            val response = apiService.restartSystem()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("重启系统失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkUpdate(): Result<UpdateCheckResult> {
        return try {
            Result.success(apiService.checkUpdate())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSystem(): Result<Unit> {
        return try {
            val response = apiService.updateSystem()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("更新系统失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDefaultConfig(): Result<Map<String, Any>> {
        return try {
            Result.success(apiService.getDefaultConfig())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConfig(key: String): Result<Unit> {
        return try {
            val response = apiService.getConfig(key)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("获取配置失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setConfig(key: String, value: Any): Result<Unit> {
        return try {
            val response = apiService.setConfig(key, value)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("设置配置失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSystemSetting(key: String): Result<Any?> {
        return try {
            Result.success(apiService.getSystemSetting(key))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSystemSetting(key: String, value: Any): Result<Unit> {
        return try {
            val response = apiService.saveSystemSetting(key, value)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("保存设置失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSystemEnv(): Result<Map<String, Any?>> {
        return try {
            Result.success(apiService.getSystemEnv())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSystemEnv(env: Map<String, Any?>): Result<Unit> {
        return try {
            val response = apiService.saveSystemEnv(env)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("保存环境变量失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserConfig(key: String): Result<Any?> {
        return try {
            Result.success(apiService.getUserConfig(key))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserConfig(key: String, value: Any): Result<Unit> {
        return try {
            val response = apiService.saveUserConfig(key, value)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("保存用户配置失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServiceStatus(): Result<Map<String, String>> {
        return try {
            Result.success(apiService.getServiceStatus())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun controlService(name: String, action: String): Result<Unit> {
        return try {
            val response = apiService.controlService(name, action)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("控制服务失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDatabaseInfo(): Result<Map<String, Any>> {
        return try {
            Result.success(apiService.getDatabaseInfo())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun backupDatabase(): Result<DatabaseBackup> {
        return try {
            Result.success(apiService.backupDatabase())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreDatabase(filename: String): Result<Unit> {
        return try {
            val response = apiService.restoreDatabase(filename)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("恢复数据库失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDependents(): Result<List<Map<String, Any>>> {
        return try {
            Result.success(apiService.getDependents())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installDependent(name: String): Result<Unit> {
        return try {
            val response = apiService.installDependent(name)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("安装依赖失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuthLevels(): Result<List<Map<String, Any>>> {
        return try {
            Result.success(apiService.getAuthLevels())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuditLogs(page: Int? = 1, count: Int? = 30): Result<List<AuditLog>> {
        return try {
            Result.success(apiService.getAuditLogs(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Media Detail ==========
    
    suspend fun getMediaDetail(
        mediaId: String,
        mediaType: String,
        title: String? = null,
        year: String? = null
    ): Result<MediaInfo> {
        return try {
            Result.success(apiService.getMediaDetail(mediaId, mediaType, title, year))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTMDbSeasons(tmdbId: Int): Result<List<TmdbSeasonDetail>> {
        return try {
            Result.success(apiService.getTMDbSeasons(tmdbId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTMDbEpisodes(
        tmdbId: Int,
        season: Int,
        episodeGroup: String? = null
    ): Result<List<TmdbEpisode>> {
        return try {
            Result.success(apiService.getTMDbEpisodes(tmdbId, season, episodeGroup))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTMDbCredits(tmdbId: Int, typeName: String): Result<List<TmdbPerson>> {
        return try {
            Result.success(apiService.getTMDbCredits(tmdbId, typeName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTMDbSimilar(tmdbId: Int, typeName: String): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.getTMDbSimilar(tmdbId, typeName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTMDbRecommendations(tmdbId: Int, typeName: String): Result<List<MediaInfo>> {
        return try {
            Result.success(apiService.getTMDbRecommendations(tmdbId, typeName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Workflow ==========

    suspend fun getWorkflows(): Result<List<Workflow>> {
        return try {
            Result.success(apiService.getWorkflows())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflowDetail(id: Int): Result<Workflow> {
        return try {
            Result.success(apiService.getWorkflowDetail(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun runWorkflow(id: Int, fromBegin: Boolean? = true): Result<WorkflowRunResult> {
        return try {
            Result.success(apiService.runWorkflow(id, fromBegin))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEventTypes(): Result<List<EventType>> {
        return try {
            Result.success(apiService.getEventTypes())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPluginActions(pluginId: String? = null): Result<List<Action>> {
        return try {
            Result.success(apiService.getPluginActions(pluginId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllActions(): Result<List<Action>> {
        return try {
            Result.success(apiService.getAllActions())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Plugin ==========

    suspend fun getPlugins(): Result<List<Plugin>> {
        return try {
            Result.success(apiService.getPlugins())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPluginMarket(): Result<List<Plugin>> {
        return try {
            Result.success(apiService.getPluginMarket())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installPlugin(pluginId: String): Result<Unit> {
        return try {
            val response = apiService.installPlugin(pluginId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("安装插件失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uninstallPlugin(pluginId: String): Result<Unit> {
        return try {
            val response = apiService.uninstallPlugin(pluginId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("卸载插件失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadPlugins(): Result<Unit> {
        return try {
            val response = apiService.reloadPlugins()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("重载插件失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== User Management ==========

    suspend fun getUsers(): Result<List<User>> {
        return try {
            Result.success(apiService.getUsers())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserInfo(): Result<UserConfigDetail> {
        return try {
            Result.success(apiService.getUserInfo())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUser(user: UserCreate): Result<Unit> {
        return try {
            val response = apiService.createUser(user)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("创建用户失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: UserUpdate): Result<Unit> {
        return try {
            val response = apiService.updateUser(user)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("更新用户失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(username: String): Result<Unit> {
        return try {
            val response = apiService.deleteUser(username)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("删除用户失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Calendar ==========

    suspend fun getBangumiCalendar(page: Int? = 1, count: Int? = 30): Result<List<BangumiCalendarItem>> {
        return try {
            Result.success(apiService.getBangumiCalendar(page, count))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Transfer ==========

    suspend fun getTransferQueue(): Result<List<RenamePreview>> {
        return try {
            Result.success(apiService.getTransferQueue())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategoryConfig(): Result<TransferConfig> {
        return try {
            Result.success(apiService.getCategoryConfig())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCategoryConfig(config: TransferConfig): Result<Unit> {
        return try {
            val response = apiService.saveCategoryConfig(config)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("保存配置失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Storage/FileManager ==========

    suspend fun listFiles(path: String, storageName: String, sort: String? = "updated_at"): Result<DirectoryContent> {
        return try {
            val fileItem = FileItem(type = "folder", path = path, name = storageName)
            Result.success(apiService.listFiles(fileItem, sort))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(name: String, path: String): Result<Unit> {
        return try {
            val response = apiService.createDirectory(FileItem(type = "folder", path = path, name = name), name)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("创建目录失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(path: String, name: String): Result<Unit> {
        return try {
            val response = apiService.deleteFile(FileItem(type = "file", path = path, name = name))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("删除文件失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(path: String, name: String, newName: String): Result<Unit> {
        return try {
            val response = apiService.renameFile(FileItem(type = "file", path = path, name = name), newName)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("重命名文件失败: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
