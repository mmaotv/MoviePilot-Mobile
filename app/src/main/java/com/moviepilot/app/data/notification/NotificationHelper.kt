package com.moviepilot.app.data.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.moviepilot.app.MainActivity
import com.moviepilot.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MoviePilot 通知推送管理器
 * 支持下载完成、新内容提醒、系统告警等通知
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_DOWNLOAD = "download_channel"
        const val CHANNEL_NEW_CONTENT = "new_content_channel"
        const val CHANNEL_ALERT = "alert_channel"
        
        const val NOTIFICATION_ID_DOWNLOAD = 1001
        const val NOTIFICATION_ID_NEW_CONTENT = 1002
        const val NOTIFICATION_ID_ALERT = 1003
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * 创建通知渠道（Android 8.0+ 必须）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            
            // 下载完成渠道
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "下载完成",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "媒体下载完成通知"
                enableVibration(true)
            }
            
            // 新内容提醒渠道
            val newContentChannel = NotificationChannel(
                CHANNEL_NEW_CONTENT,
                "新内容提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "订阅内容更新提醒"
                enableVibration(true)
            }
            
            // 系统告警渠道
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "系统告警",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "服务器异常、磁盘空间不足等告警通知"
                enableVibration(true)
                enableLights(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(downloadChannel, newContentChannel, alertChannel)
            )
        }
    }
    
    /**
     * 显示下载完成通知
     */
    fun showDownloadCompleteNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_DOWNLOAD,
            notificationId = NOTIFICATION_ID_DOWNLOAD,
            title = title,
            message = message,
            icon = android.R.drawable.stat_sys_download_done
        )
    }
    
    /**
     * 显示新内容提醒通知
     */
    fun showNewContentNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_NEW_CONTENT,
            notificationId = NOTIFICATION_ID_NEW_CONTENT,
            title = title,
            message = message,
            icon = android.R.drawable.ic_popup_reminder
        )
    }
    
    /**
     * 显示系统告警通知
     */
    fun showAlertNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_ALERT,
            notificationId = NOTIFICATION_ID_ALERT,
            title = title,
            message = message,
            icon = android.R.drawable.ic_dialog_alert,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    /**
     * 显示一般通知
     */
    private fun showNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
    
    /**
     * 取消指定通知
     */
    fun cancelNotification(notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
