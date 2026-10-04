package com.moviepilot.app.data.network

import com.google.gson.annotations.SerializedName
import com.moviepilot.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

// Type alias for common Response type
typealias ApiResult = Response<Unit>

interface ApiService {

    // ========== Authentication ==========
    @POST("api/v1/login/access-token")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("otp_password") otpPassword: String? = null
    ): LoginResponse

    @GET("api/v1/system/global/user")
    suspend fun getCurrentUser(): UserConfig

    @GET("api/v1/system/global")
    suspend fun getGlobalConfig(@Query("token") token: String = "moviepilot"): GlobalConfig

    // ========== Dashboard ==========
    @GET("api/v1/dashboard/storage")
    suspend fun getStorageInfo(): StorageInfo

    @GET("api/v1/dashboard/statistic")
    suspend fun getMediaStatistics(): MediaStatistics

    @GET("api/v1/dashboard/downloader")
    suspend fun getDownloaderInfo(): DownloaderInfo

    @GET("api/v1/dashboard/cpu")
    suspend fun getCpuUsage(): Float

    @GET("api/v1/dashboard/memory")
    suspend fun getMemoryUsage(): List<Int>

    @GET("api/v1/dashboard/transfer")
    suspend fun getTransferStats(): List<Int>

    @GET("api/v1/dashboard/schedule")
    suspend fun getSchedule(): List<ScheduleInfo>

    // ========== Resources & Discovery ==========
    @GET("api/v1/search/title")
    suspend fun searchTorrents(@Query("keyword") keyword: String): SearchResponse

    @GET("api/v1/search/last")
    suspend fun getLastSearchResults(): SearchResponse

    @GET("api/v1/recommend/trending")
    suspend fun getTrending(@Query("type") type: String = "MOV", @Query("page") page: Int = 1): List<MediaInfo>

    @GET("api/v1/recommend/now_playing")
    suspend fun getNowPlaying(@Query("type") type: String = "MOV", @Query("page") page: Int = 1): List<MediaInfo>

    @GET("api/v1/discover/filter")
    suspend fun discoverMedia(@QueryMap filters: Map<String, String>): List<MediaInfo>

    // ========== Subscriptions ==========
    @GET("api/v1/subscribe/")
    suspend fun getSubscriptions(@Query("type") type: String? = null): List<Subscribe>

    @GET("api/v1/subscribe/{id}")
    suspend fun getSubscription(@Path("id") id: Int): Subscribe

    @POST("api/v1/subscribe/")
    suspend fun addSubscription(@Body subscription: SubscribeRequest): Response<Unit>

    @PUT("api/v1/subscribe/")
    suspend fun updateSubscription(@Body subscription: SubscribeRequest): Response<Unit>

    @DELETE("api/v1/subscribe/{id}")
    suspend fun deleteSubscription(@Path("id") id: Int): Response<Unit>

    @PUT("api/v1/subscribe/status/{id}")
    suspend fun updateSubscriptionStatus(@Path("id") id: Int, @Query("state") state: String): Response<Unit>

    // ========== Downloads ==========
    @GET("api/v1/download/")
    suspend fun getDownloadTasks(@Query("name") downloaderName: String? = null): List<DownloadTask>

    @GET("api/v1/download/clients")
    suspend fun getDownloadClients(): List<Map<String, Any>>

    /**
     * 下载种子到服务器
     * 请求体为 TorrentSearchResult（含 torrent_info / meta_info / media_info）
     */
    @POST("api/v1/download/")
    suspend fun addDownloadTask(@Body torrent: Map<String, Any>): Response<Unit>

    // ========== History ==========
    @GET("api/v1/history/transfer")
    suspend fun getTransferHistory(@Query("page") page: Int = 1, @Query("count") count: Int = 30): TransferHistoryWrapper

    // ========== Plugins ==========
    @GET("api/v1/plugin/")
    suspend fun getPlugins(): List<Plugin>

    @POST("api/v1/plugin/install")
    suspend fun installPlugin(@Query("pid") pluginId: String): Response<Unit>

    @POST("api/v1/plugin/uninstall")
    suspend fun uninstallPlugin(@Query("id") pluginId: String): Response<Unit>

    // ========== Sites ==========
    @GET("api/v1/site/")
    suspend fun getSites(): List<SiteInfo>

    // ========== System Settings ==========
    @GET("api/v1/system/setting/{key}")
    suspend fun getSystemSetting(@Path("key") key: String): Any?

    @POST("api/v1/system/setting/{key}")
    suspend fun saveSystemSetting(@Path("key") key: String, @Body value: Any): Response<Unit>

    @GET("api/v1/system/env")
    suspend fun getSystemEnv(): Map<String, Any?>

    @POST("api/v1/system/env")
    suspend fun saveSystemEnv(@Body env: Map<String, Any?>): Response<Unit>

    // ========== User Config ==========
    @GET("api/v1/user/config/{key}")
    suspend fun getUserConfig(@Path("key") key: String): Any?

    @POST("api/v1/user/config/{key}")
    suspend fun saveUserConfig(@Path("key") key: String, @Body value: Any): Response<Unit>

    // ========== Media Servers ==========
    @GET("api/v1/mediaserver/library")
    suspend fun getMediaServerLibraries(): List<Map<String, Any>>

    // ========== MFA (Two-Factor Auth) ==========
    @GET("api/v1/mfa/status")
    suspend fun getMFAStatus(): MFAStatus

    @POST("api/v1/mfa/generate")
    suspend fun generateMFASecret(): MFASecret

    @POST("api/v1/mfa/enable")
    suspend fun enableMFA(@Body data: MFAEnableRequest): Response<Unit>

    @POST("api/v1/mfa/disable")
    suspend fun disableMFA(@Body data: MFAVerifyRequest): Response<Unit>

    @POST("api/v1/mfa/verify")
    suspend fun verifyMFACode(@Body data: MFAVerifyRequest): Response<Unit>

    // ========== Users ==========
    @GET("api/v1/user/")
    suspend fun getUsers(): List<User>

    @POST("api/v1/user/")
    suspend fun createUser(@Body user: UserCreate): Response<Unit>

    @PUT("api/v1/user/")
    suspend fun updateUser(@Body user: UserUpdate): Response<Unit>

    @DELETE("api/v1/user/{username}")
    suspend fun deleteUser(@Path("username") username: String): Response<Unit>

    @GET("api/v1/user/info")
    suspend fun getUserInfo(): UserConfigDetail

    // ========== Dashboard Extended ==========
    @GET("api/v1/dashboard/statistics")
    suspend fun getDashboardStatistics(): MediaStatistics

    @GET("api/v1/dashboard/storages")
    suspend fun getDashboardStorages(): List<StorageInfo>

    @GET("api/v1/dashboard/scheduler")
    suspend fun getSchedulerJobs(): List<ScheduleInfo>

    @GET("api/v1/dashboard/processes")
    suspend fun getProcesses(): List<ProcessInfo>

    // ========== Search Extended ==========
    @GET("api/v1/search/media/{mediaid}")
    suspend fun searchByMediaId(
        @Path("mediaid") mediaid: String,
        @Query("mtype") mtype: String?,
        @Query("area") area: String? = "title",
        @Query("title") title: String?,
        @Query("year") year: String?,
        @Query("season") season: String?,
        @Query("sites") sites: String?
    ): Response<Unit>

    @POST("api/v1/search/recommend")
    suspend fun getAIRecommendations(@Body request: Map<String, Any>): Response<Unit>

    // ========== Subscriptions Extended ==========
    @GET("api/v1/subscribe/list")
    suspend fun getSubscribesWithToken(@Query("token") token: String): List<Subscribe>

    @GET("api/v1/subscribe/media/{mediaid}")
    suspend fun getSubscribeByMediaId(
        @Path("mediaid") mediaid: String,
        @Query("season") season: Int?
    ): Subscribe?

    @GET("api/v1/subscribe/refresh")
    suspend fun refreshSubscribes(): Response<Unit>

    @GET("api/v1/subscribe/reset/{subid}")
    suspend fun resetSubscribe(@Path("subid") subid: Int): Response<Unit>

    @GET("api/v1/subscribe/check")
    suspend fun checkSubscribeTMDB(): Response<Unit>

    @GET("api/v1/subscribe/search")
    suspend fun searchAllSubscribes(): Response<Unit>

    @GET("api/v1/subscribe/search/{subscribe_id}")
    suspend fun searchSubscribe(@Path("subscribe_id") id: Int): Response<Unit>

    @DELETE("api/v1/subscribe/media/{mediaid}")
    suspend fun deleteSubscribeByMediaId(
        @Path("mediaid") mediaid: String,
        @Query("season") season: Int?
    ): Response<Unit>

    @GET("api/v1/subscribe/history/{mtype}")
    suspend fun getSubscribeHistory(@Path("mtype") mtype: String): List<Subscribe>

    @DELETE("api/v1/subscribe/history/{history_id}")
    suspend fun deleteSubscribeHistory(@Path("history_id") id: Int): Response<Unit>

    @GET("api/v1/subscribe/popular")
    suspend fun getPopularSubscribes(): List<MediaInfo>

    @GET("api/v1/subscribe/user/{username}")
    suspend fun getUserSubscribes(@Path("username") username: String): List<Subscribe>

    @GET("api/v1/subscribe/files/{subscribe_id}")
    suspend fun getSubscribeFiles(@Path("subscribe_id") id: Int): Response<Unit>

    @POST("api/v1/subscribe/share")
    suspend fun shareSubscribe(@Body share: Map<String, Any>): Response<Unit>

    @DELETE("api/v1/subscribe/share/{share_id}")
    suspend fun deleteSubscribeShare(@Path("share_id") id: Int): Response<Unit>

    @POST("api/v1/subscribe/fork")
    suspend fun forkSubscribe(@Body fork: Map<String, Any>): Response<Unit>

    @GET("api/v1/subscribe/follow")
    suspend fun getFollowedUsers(): List<String>

    @POST("api/v1/subscribe/follow")
    suspend fun followUser(@Body data: Map<String, String>): Response<Unit>

    @DELETE("api/v1/subscribe/follow")
    suspend fun unfollowUser(@Body data: Map<String, String>): Response<Unit>

    @GET("api/v1/subscribe/shares")
    suspend fun getSharedSubscribes(
        @Query("name") name: String?,
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<Subscribe>

    @GET("api/v1/subscribe/share/statistics")
    suspend fun getSubscribeShareStatistics(): MediaStatistics

    // ========== Downloads Extended ==========
    @GET("api/v1/download/current")
    suspend fun getCurrentDownloads(): List<DownloadTask>

    @GET("api/v1/download/history")
    suspend fun getDownloadHistory(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DownloadTask>

    @GET("api/v1/download/stats")
    suspend fun getDownloadStats(): DownloaderInfo

    @POST("api/v1/download/start")
    suspend fun startDownload(@Body download: Map<String, Any>): Response<Unit>

    @POST("api/v1/download/stop")
    suspend fun stopDownload(@Body download: Map<String, Any>): Response<Unit>

    @DELETE("api/v1/download/{download_id}")
    suspend fun deleteDownload(@Path("download_id") id: String): Response<Unit>

    @POST("api/v1/download/test")
    suspend fun testDownloadClient(@Body client: Map<String, Any>): Response<Unit>

    @GET("api/v1/download/categories")
    suspend fun getDownloadCategories(): List<String>

    // ========== History Extended ==========
    @DELETE("api/v1/history/{history_id}")
    suspend fun deleteTransferHistory(@Path("history_id") id: Int): Response<Unit>

    @POST("api/v1/history/batch-delete")
    suspend fun batchDeleteHistory(@Body ids: List<Int>): Response<Unit>

    @GET("api/v1/history/statistics")
    suspend fun getHistoryStatistics(): MediaStatistics

    // ========== Sites Extended ==========
    @POST("api/v1/site/")
    suspend fun addSite(@Body site: SiteInfo): Response<Unit>

    @PUT("api/v1/site/")
    suspend fun updateSite(@Body site: SiteInfo): Response<Unit>

    @DELETE("api/v1/site/{site_id}")
    suspend fun deleteSite(@Path("site_id") id: Int): Response<Unit>

    @GET("api/v1/site/statistics")
    suspend fun getSiteStatistics(): List<SiteStatistic>

    @GET("api/v1/site/rss")
    suspend fun getSiteRSS(@Query("site_id") siteId: Int): List<TorrentSearchResult>

    @POST("api/v1/site/test")
    suspend fun testSite(@Body site: SiteTestRequest): SiteTestResult

    @GET("api/v1/site/brushtask")
    suspend fun getBrushTasks(): List<BrushTask>

    @POST("api/v1/site/brushtask")
    suspend fun addBrushTask(@Body task: BrushTask): Response<Unit>

    @PUT("api/v1/site/brushtask")
    suspend fun updateBrushTask(@Body task: BrushTask): Response<Unit>

    @DELETE("api/v1/site/brushtask/{task_id}")
    suspend fun deleteBrushTask(@Path("task_id") id: Int): Response<Unit>

    @GET("api/v1/site/cookiecloud")
    suspend fun getCookieCloudSites(): List<SiteInfo>

    @POST("api/v1/site/cookiecloud/update")
    suspend fun updateCookieCloud(): Response<Unit>

    // ========== Plugins Extended ==========
    @GET("api/v1/plugin/apps")
    suspend fun getInstalledApps(): List<Plugin>

    @GET("api/v1/plugin/statistic")
    suspend fun getPluginStatistics(): Map<String, Int>

    @GET("api/v1/plugin/{plugin_id}/info")
    suspend fun getPluginInfo(@Path("plugin_id") pluginId: String): Plugin

    @GET("api/v1/plugin/{plugin_id}/page")
    suspend fun getPluginPage(@Path("plugin_id") pluginId: String): Response<Unit>

    @POST("api/v1/plugin/{plugin_id}/{method}")
    suspend fun callPluginMethod(
        @Path("plugin_id") pluginId: String,
        @Path("method") method: String,
        @Body params: Map<String, Any>? = null
    ): Response<Unit>

    @GET("api/v1/plugin/market")
    suspend fun getPluginMarket(): List<Plugin>

    @POST("api/v1/plugin/reload")
    suspend fun reloadPlugins(): Response<Unit>

    // ========== System Extended ==========
    @GET("api/v1/system/version")
    suspend fun getSystemVersion(): VersionInfo

    @GET("api/v1/system/logging")
    suspend fun getSystemLogging(): List<LogEntry>

    @POST("api/v1/system/logging")
    suspend fun clearSystemLogging(): Response<Unit>

    @GET("api/v1/system/name")
    suspend fun getSystemName(): String

    @GET("api/v1/system/progress")
    suspend fun getSystemProgress(): Map<String, Float>

    @GET("api/v1/system/restart")
    suspend fun restartSystem(): Response<Unit>

    @GET("api/v1/system/update")
    suspend fun checkUpdate(): UpdateCheckResult

    @POST("api/v1/system/update")
    suspend fun updateSystem(): Response<Unit>

    @GET("api/v1/system/config/defaults")
    suspend fun getDefaultConfig(): Map<String, Any>

    @GET("api/v1/system/config/{key}")
    suspend fun getConfig(@Path("key") key: String): Response<Unit>

    @POST("api/v1/system/config/{key}")
    suspend fun setConfig(@Path("key") key: String, @Body value: Any): Response<Unit>

    @GET("api/v1/system/service")
    suspend fun getServiceStatus(): Map<String, String>

    @POST("api/v1/system/service/{name}")
    suspend fun controlService(
        @Path("name") name: String,
        @Query("action") action: String
    ): Response<Unit>

    @GET("api/v1/system/database")
    suspend fun getDatabaseInfo(): Map<String, Any>

    @POST("api/v1/system/database/backup")
    suspend fun backupDatabase(): DatabaseBackup

    @POST("api/v1/system/database/restore")
    suspend fun restoreDatabase(@Query("filename") filename: String): Response<Unit>

    @GET("api/v1/system/dependents")
    suspend fun getDependents(): List<Map<String, Any>>

    @POST("api/v1/system/dependents/install")
    suspend fun installDependent(@Query("name") name: String): Response<Unit>

    @GET("api/v1/system/authlevels")
    suspend fun getAuthLevels(): List<Map<String, Any>>

    @GET("api/v1/system/audit")
    suspend fun getAuditLogs(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<AuditLog>

    // ========== Media Extended ==========
    @GET("api/v1/media/recognize")
    suspend fun recognizeMedia(
        @Query("title") title: String,
        @Query("subtitle") subtitle: String?
    ): MediaInfo

    @GET("api/v1/media/recognize_file")
    suspend fun recognizeFile(@Query("path") path: String): MediaInfo

    @GET("api/v1/media/search")
    suspend fun searchMedia(
        @Query("title") title: String,
        @Query("type") type: String? = "media",
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 8
    ): List<MediaInfo>

    @POST("api/v1/media/scrape/{storage}")
    suspend fun scrapeMedia(
        @Path("storage") storage: String? = "local",
        @Body fileitem: FileItem
    ): Response<Unit>

    @GET("api/v1/media/category/config")
    suspend fun getCategoryConfig(): TransferConfig

    @POST("api/v1/media/category/config")
    suspend fun saveCategoryConfig(@Body config: TransferConfig): Response<Unit>

    @GET("api/v1/media/category")
    suspend fun getMediaCategories(): List<String>

    @GET("api/v1/media/group/seasons/{episode_group}")
    suspend fun getGroupSeasons(@Path("episode_group") group: String): List<TmdbSeasonDetail>

    @GET("api/v1/media/groups/{tmdbid}")
    suspend fun getEpisodeGroups(@Path("tmdbid") tmdbid: Int): List<Map<String, Any>>

    @GET("api/v1/media/seasons")
    suspend fun getSeasons(
        @Query("mediaid") mediaid: String?,
        @Query("title") title: String?,
        @Query("year") year: String?,
        @Query("season") season: Int?
    ): List<TmdbSeasonDetail>

    @GET("api/v1/media/{mediaid}")
    suspend fun getMediaDetail(
        @Path("mediaid") mediaid: String,
        @Query("type_name") typeName: String,
        @Query("title") title: String?,
        @Query("year") year: String?
    ): MediaInfo

    // ========== Transfer ==========
    @GET("api/v1/transfer/name")
    suspend fun queryTransferName(
        @Query("path") path: String,
        @Query("filetype") filetype: String
    ): RenamePreview

    @GET("api/v1/transfer/queue")
    suspend fun getTransferQueue(): List<RenamePreview>

    @DELETE("api/v1/transfer/queue")
    suspend fun removeFromQueue(@Body fileitem: FileItem): Response<Unit>

    @POST("api/v1/transfer/manual")
    suspend fun manualTransfer(
        @Body transferItem: ManualTransferRequest,
        @Query("background") background: Boolean? = false
    ): TransferResult

    @GET("api/v1/transfer/now")
    suspend fun triggerTransferNow(@Query("token") token: String): Response<Unit>

    // ========== Torrent Cache ==========
    @GET("api/v1/torrent/cache")
    suspend fun getTorrentCache(): List<TorrentCacheItem>

    @DELETE("api/v1/torrent/cache/{domain}/{torrent_hash}")
    suspend fun deleteTorrentCache(
        @Path("domain") domain: String,
        @Path("torrent_hash") hash: String
    ): Response<Unit>

    @DELETE("api/v1/torrent/cache")
    suspend fun clearTorrentCache(): Response<Unit>

    @POST("api/v1/torrent/cache/refresh")
    suspend fun refreshTorrentCache(): TorrentCacheRefreshResult

    @POST("api/v1/torrent/cache/reidentify/{domain}/{torrent_hash}")
    suspend fun reidentifyTorrent(
        @Path("domain") domain: String,
        @Path("torrent_hash") hash: String,
        @Query("tmdbid") tmdbid: Int?,
        @Query("doubanid") doubanid: String?
    ): Response<Unit>

    // ========== Messages ==========
    @POST("api/v1/message/web")
    suspend fun sendWebMessage(@Body message: Message): Response<Unit>

    @GET("api/v1/message/web")
    suspend fun getWebMessages(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 20
    ): List<Message>

    @POST("api/v1/message/webpush/subscribe")
    suspend fun subscribeWebPush(@Body subscription: Map<String, Any>): Response<Unit>

    @POST("api/v1/message/webpush/send")
    suspend fun sendWebPushNotification(@Body payload: Map<String, Any>): Response<Unit>

    // ========== Workflows ==========
    @GET("api/v1/workflow/")
    suspend fun getWorkflows(): List<Workflow>

    @POST("api/v1/workflow/")
    suspend fun createWorkflow(@Body workflow: Workflow): Response<Unit>

    @GET("api/v1/workflow/plugin/actions")
    suspend fun getPluginActions(@Query("plugin_id") pluginId: String?): List<Action>

    @GET("api/v1/workflow/actions")
    suspend fun getAllActions(): List<Action>

    @GET("api/v1/workflow/event_types")
    suspend fun getEventTypes(): List<EventType>

    @POST("api/v1/workflow/share")
    suspend fun shareWorkflow(@Body share: Map<String, Any>): Response<Unit>

    @DELETE("api/v1/workflow/share/{share_id}")
    suspend fun deleteWorkflowShare(@Path("share_id") id: Int): Response<Unit>

    @POST("api/v1/workflow/fork")
    suspend fun forkWorkflow(@Body fork: Map<String, Any>): Response<Unit>

    @GET("api/v1/workflow/shares")
    suspend fun getSharedWorkflows(
        @Query("name") name: String?,
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<Workflow>

    @POST("api/v1/workflow/{workflow_id}/run")
    suspend fun runWorkflow(
        @Path("workflow_id") id: Int,
        @Query("from_begin") fromBegin: Boolean? = true
    ): WorkflowRunResult

    @POST("api/v1/workflow/{workflow_id}/start")
    suspend fun startWorkflow(@Path("workflow_id") id: Int): Response<Unit>

    @POST("api/v1/workflow/{workflow_id}/pause")
    suspend fun pauseWorkflow(@Path("workflow_id") id: Int): Response<Unit>

    @POST("api/v1/workflow/{workflow_id}/reset")
    suspend fun resetWorkflow(@Path("workflow_id") id: Int): Response<Unit>

    @GET("api/v1/workflow/{workflow_id}")
    suspend fun getWorkflowDetail(@Path("workflow_id") id: Int): Workflow

    @PUT("api/v1/workflow/{workflow_id}")
    suspend fun updateWorkflow(
        @Path("workflow_id") id: Int,
        @Body workflow: Workflow
    ): Response<Unit>

    @DELETE("api/v1/workflow/{workflow_id}")
    suspend fun deleteWorkflow(@Path("workflow_id") id: Int): Response<Unit>

    // ========== Media Servers Extended ==========
    @GET("api/v1/mediaserver/play/{itemid}")
    suspend fun getPlayUrl(@Path("itemid") itemId: String): PlayUrl

    @GET("api/v1/mediaserver/exists")
    suspend fun checkLocalExists(
        @Query("title") title: String?,
        @Query("year") year: String?,
        @Query("mtype") mtype: String?,
        @Query("tmdbid") tmdbid: Int?,
        @Query("season") season: Int?
    ): ExistsResult

    @POST("api/v1/mediaserver/exists_remote")
    suspend fun checkRemoteExists(@Body mediaInfo: MediaInfo): ExistsResult

    @POST("api/v1/mediaserver/notexists")
    suspend fun getNotExistsInfo(@Body mediaInfo: MediaInfo): List<MediaInfo>

    @GET("api/v1/mediaserver/latest")
    suspend fun getLatestItems(
        @Query("server") server: String,
        @Query("count") count: Int? = 20
    ): List<MediaServerItem>

    @GET("api/v1/mediaserver/playing")
    suspend fun getPlayingItems(
        @Query("server") server: String,
        @Query("count") count: Int? = 12
    ): List<MediaServerItem>

    @GET("api/v1/mediaserver/clients")
    suspend fun getAvailableServers(): List<Map<String, Any>>

    // ========== Storage ==========
    @GET("api/v1/storage/qrcode/{name}")
    suspend fun generateQRCode(@Path("name") name: String): Response<Unit>

    @GET("api/v1/storage/auth_url/{name}")
    suspend fun getAuthUrl(@Path("name") name: String): Response<Unit>

    @GET("api/v1/storage/check/{name}")
    suspend fun checkLogin(
        @Path("name") name: String,
        @Query("ck") ck: String?,
        @Query("t") t: String?
    ): Response<Unit>

    @POST("api/v1/storage/save/{name}")
    suspend fun saveStorageConfig(
        @Path("name") name: String,
        @Body config: Map<String, Any>
    ): Response<Unit>

    @GET("api/v1/storage/reset/{name}")
    suspend fun resetStorageConfig(@Path("name") name: String): Response<Unit>

    @POST("api/v1/storage/list")
    suspend fun listFiles(
        @Body fileitem: FileItem,
        @Query("sort") sort: String? = "updated_at"
    ): DirectoryContent

    @POST("api/v1/storage/mkdir")
    suspend fun createDirectory(
        @Body fileitem: FileItem,
        @Query("name") name: String
    ): Response<Unit>

    @POST("api/v1/storage/delete")
    suspend fun deleteFile(@Body fileitem: FileItem): Response<Unit>

    @POST("api/v1/storage/download")
    suspend fun downloadFile(@Body fileitem: FileItem): Response<Unit>

    @POST("api/v1/storage/image")
    suspend fun previewImage(@Body fileitem: FileItem): Response<Unit>

    @POST("api/v1/storage/rename")
    suspend fun renameFile(
        @Body fileitem: FileItem,
        @Query("new_name") newName: String,
        @Query("recursive") recursive: Boolean? = false
    ): Response<Unit>

    @GET("api/v1/storage/usage/{name}")
    suspend fun getStorageUsage(@Path("name") name: String): StorageUsage

    @GET("api/v1/storage/transtype/{name}")
    suspend fun getSupportedTransferTypes(@Path("name") name: String): List<StorageTransType>

    // ========== Discovery ==========
    @GET("api/v1/discover/source")
    suspend fun getDiscoverSources(): List<Map<String, Any>>

    @GET("api/v1/discover/bangumi")
    suspend fun discoverBangumi(
        @Query("type") type: Int? = 2,
        @Query("cat") cat: Int?,
        @Query("sort") sort: String? = "rank",
        @Query("year") year: String?,
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<BangumiCalendarItem>

    @GET("api/v1/discover/douban_movies")
    suspend fun discoverDoubanMovies(
        @Query("sort") sort: String? = "R",
        @Query("tags") tags: String? = "",
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/discover/douban_tvs")
    suspend fun discoverDoubanTVs(
        @Query("sort") sort: String? = "R",
        @Query("tags") tags: String? = "",
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/discover/tmdb_movies")
    suspend fun discoverTMDbMovies(
        @Query("sort_by") sortBy: String? = "popularity.desc",
        @Query("with_genres") withGenres: String? = "",
        @Query("with_original_language") withLanguage: String? = "",
        @Query("with_keywords") withKeywords: String? = "",
        @Query("with_watch_providers") withProviders: String? = "",
        @Query("vote_average") voteAverage: Float? = 0.0f,
        @Query("vote_count") voteCount: Int? = 0,
        @Query("release_date") releaseDate: String?,
        @Query("page") page: Int? = 1
    ): List<MediaInfo>

    @GET("api/v1/discover/tmdb_tvs")
    suspend fun discoverTMDbTVs(
        @Query("sort_by") sortBy: String? = "popularity.desc",
        @Query("with_genres") withGenres: String? = "",
        @Query("with_original_language") withLanguage: String? = "",
        @Query("with_keywords") withKeywords: String? = "",
        @Query("with_watch_providers") withProviders: String? = "",
        @Query("vote_average") voteAverage: Float? = 0.0f,
        @Query("vote_count") voteCount: Int? = 0,
        @Query("release_date") releaseDate: String?,
        @Query("page") page: Int? = 1
    ): List<MediaInfo>

    // ========== Recommendations ==========
    @GET("api/v1/recommend/source")
    suspend fun getRecommendSources(): List<Map<String, Any>>

    @GET("api/v1/recommend/bangumi_calendar")
    suspend fun getBangumiCalendar(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<BangumiCalendarItem>

    @GET("api/v1/recommend/douban_showing")
    suspend fun getDoubanShowing(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/recommend/douban_movies")
    suspend fun getDoubanMovies(
        @Query("sort") sort: String? = "R",
        @Query("tags") tags: String? = "",
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/recommend/douban_tvs")
    suspend fun getDoubanTVs(
        @Query("sort") sort: String? = "R",
        @Query("tags") tags: String? = "",
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/recommend/douban_movie_top250")
    suspend fun getDoubanTop250(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/recommend/douban_tv_weekly_chinese")
    suspend fun getDoubanWeeklyChinese(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/recommend/douban_tv_weekly_global")
    suspend fun getDoubanWeeklyGlobal(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/recommend/douban_tv_animation")
    suspend fun getDoubanAnimation(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/recommend/douban_movie_hot")
    suspend fun getDoubanHotMovies(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/recommend/douban_tv_hot")
    suspend fun getDoubanHotTVs(
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 30
    ): List<DoubanHotItem>

    @GET("api/v1/recommend/tmdb_movies")
    suspend fun getTMDbMovies(
        @Query("sort_by") sortBy: String? = "popularity.desc",
        @Query("with_genres") withGenres: String? = "",
        @Query("with_original_language") withLanguage: String? = "",
        @Query("with_keywords") withKeywords: String? = "",
        @Query("with_watch_providers") withProviders: String? = "",
        @Query("vote_average") voteAverage: Float? = 0.0f,
        @Query("vote_count") voteCount: Int? = 0,
        @Query("release_date") releaseDate: String?,
        @Query("page") page: Int? = 1
    ): List<MediaInfo>

    @GET("api/v1/recommend/tmdb_tvs")
    suspend fun getTMDbTVs(
        @Query("sort_by") sortBy: String? = "popularity.desc",
        @Query("with_genres") withGenres: String? = "",
        @Query("with_original_language") withLanguage: String? = "",
        @Query("with_keywords") withKeywords: String? = "",
        @Query("with_watch_providers") withProviders: String? = "",
        @Query("vote_average") voteAverage: Float? = 0.0f,
        @Query("vote_count") voteCount: Int? = 0,
        @Query("release_date") releaseDate: String?,
        @Query("page") page: Int? = 1
    ): List<MediaInfo>

    @GET("api/v1/recommend/tmdb_trending")
    suspend fun getTMDbTrending(@Query("page") page: Int? = 1): List<TrendingItem>

    // ========== TMDB ==========
    @GET("api/v1/tmdb/seasons/{tmdbid}")
    suspend fun getTMDbSeasons(@Path("tmdbid") tmdbid: Int): List<TmdbSeasonDetail>

    @GET("api/v1/tmdb/similar/{tmdbid}/{type_name}")
    suspend fun getTMDbSimilar(
        @Path("tmdbid") tmdbid: Int,
        @Path("type_name") typeName: String
    ): List<MediaInfo>

    @GET("api/v1/tmdb/recommend/{tmdbid}/{type_name}")
    suspend fun getTMDbRecommendations(
        @Path("tmdbid") tmdbid: Int,
        @Path("type_name") typeName: String
    ): List<MediaInfo>

    @GET("api/v1/tmdb/collection/{collection_id}")
    suspend fun getTMDbCollection(
        @Path("collection_id") collectionId: Int,
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 20
    ): List<MediaInfo>

    @GET("api/v1/tmdb/credits/{tmdbid}/{type_name}")
    suspend fun getTMDbCredits(
        @Path("tmdbid") tmdbid: Int,
        @Path("type_name") typeName: String,
        @Query("page") page: Int? = 1
    ): List<TmdbPerson>

    @GET("api/v1/tmdb/person/{person_id}")
    suspend fun getTMDbPerson(@Path("person_id") personId: Int): TmdbPerson

    @GET("api/v1/tmdb/person/credits/{person_id}")
    suspend fun getTMDbPersonCredits(
        @Path("person_id") personId: Int,
        @Query("page") page: Int? = 1
    ): List<MediaInfo>

    @GET("api/v1/tmdb/{tmdbid}/{season}")
    suspend fun getTMDbEpisodes(
        @Path("tmdbid") tmdbid: Int,
        @Path("season") season: Int,
        @Query("episode_group") episodeGroup: String?
    ): List<TmdbEpisode>

    // ========== Douban ==========
    @GET("api/v1/douban/person/{person_id}")
    suspend fun getDoubanPerson(@Path("person_id") personId: Int): Map<String, Any>

    @GET("api/v1/douban/person/credits/{person_id}")
    suspend fun getDoubanPersonCredits(
        @Path("person_id") personId: Int,
        @Query("page") page: Int? = 1
    ): List<MediaInfo>

    @GET("api/v1/douban/credits/{doubanid}/{type_name}")
    suspend fun getDoubanCredits(
        @Path("doubanid") doubanid: String,
        @Path("type_name") typeName: String
    ): List<Map<String, Any>>

    @GET("api/v1/douban/recommend/{doubanid}/{type_name}")
    suspend fun getDoubanRecommendations(
        @Path("doubanid") doubanid: String,
        @Path("type_name") typeName: String
    ): List<MediaInfo>

    @GET("api/v1/douban/{doubanid}")
    suspend fun getDoubanInfo(@Path("doubanid") doubanid: String): MediaInfo

    // ========== Bangumi ==========
    @GET("api/v1/bangumi/credits/{bangumiid}")
    suspend fun getBangumiCredits(
        @Path("bangumiid") bangumiid: Int,
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 20
    ): List<Map<String, Any>>

    @GET("api/v1/bangumi/recommend/{bangumiid}")
    suspend fun getBangumiRecommendations(
        @Path("bangumiid") bangumiid: Int,
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 20
    ): List<MediaInfo>

    @GET("api/v1/bangumi/person/{person_id}")
    suspend fun getBangumiPerson(@Path("person_id") personId: Int): Map<String, Any>

    @GET("api/v1/bangumi/person/credits/{person_id}")
    suspend fun getBangumiPersonCredits(
        @Path("person_id") personId: Int,
        @Query("page") page: Int? = 1,
        @Query("count") count: Int? = 20
    ): List<MediaInfo>

    @GET("api/v1/bangumi/{bangumiid}")
    suspend fun getBangumiInfo(@Path("bangumiid") bangumiid: Int): MediaInfo
}

/**
 * Generic API response wrapper (avoid conflict with retrofit2.Response)
 */
data class ApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: Any?
)

/**
 * Search API response wrapper with typed data
 */
data class SearchResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<TorrentSearchResult>?
)
