package com.moviepilot.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * 安装下载好的 APK。
 *
 * Android 8 起需要「允许安装未知应用」这个逐个 App 开关，
 * Android 7 起必须用 content:// 而不是 file://（FileProvider 提供）。
 * 两件事都在这里统一处理，界面只关心「能不能直接装 / 要不要先去开权限」。
 */
object ApkInstaller {

    fun canInstall(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /** 跳到本应用的「安装未知应用」开关页 */
    fun openInstallPermissionSettings(context: Context) {
        val appUri = Uri.parse("package:${context.packageName}")
        val direct = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, appUri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(direct) }.isSuccess) return

        val fallback = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(fallback) }.isSuccess) return

        // 极少数 ROM 没有这个页面，退到本应用详情页
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun install(context: Context, apk: File): Result<Unit> = runCatching {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val base = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 直接点名系统包安装器：测试机上装了 MT管理器 / App Manager 等一堆能处理 APK 的应用，
        // 不指定就会先弹一个「打开方式」选择器，多一步很别扭。
        // AOSP / 大多数国产 ROM 是 com.android.packageinstaller，Google 版是 com.google.android.packageinstaller。
        val installers = listOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
        )

        for (pkg in installers) {
            val explicit = Intent(base).setPackage(pkg)
            if (runCatching { context.startActivity(explicit) }.isSuccess) return@runCatching
        }

        // 都不认的时候退回通用 Intent（可能弹选择器，但至少能装上）
        context.startActivity(base)
    }
}
