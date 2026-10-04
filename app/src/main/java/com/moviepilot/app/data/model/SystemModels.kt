package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * System related models
 */
data class VersionInfo(
    @SerializedName("version") val version: String?,
    @SerializedName("build_time") val buildTime: String?,
    @SerializedName("commit") val commit: String?
)

data class LogEntry(
    @SerializedName("time") val time: String?,
    @SerializedName("level") val level: String?,
    @SerializedName("message") val message: String?
)

data class ProcessInfo(
    @SerializedName("pid") val pid: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("cpu_percent") val cpuPercent: Float?,
    @SerializedName("memory_percent") val memoryPercent: Float?,
    @SerializedName("num_threads") val numThreads: Int?
)

data class DatabaseBackup(
    @SerializedName("filename") val filename: String?,
    @SerializedName("size") val size: Long?,
    @SerializedName("created_at") val createdAt: String?
)

data class UpdateCheckResult(
    @SerializedName("has_update") val hasUpdate: Boolean?,
    @SerializedName("latest_version") val latestVersion: String?,
    @SerializedName("release_notes") val releaseNotes: String?,
    @SerializedName("download_url") val downloadUrl: String?
)

data class AuditLog(
    @SerializedName("id") val id: Int?,
    @SerializedName("username") val username: String?,
    @SerializedName("action") val action: String?,
    @SerializedName("detail") val detail: String?,
    @SerializedName("ip") val ip: String?,
    @SerializedName("user_agent") val userAgent: String?,
    @SerializedName("created_at") val createdAt: String?
)
