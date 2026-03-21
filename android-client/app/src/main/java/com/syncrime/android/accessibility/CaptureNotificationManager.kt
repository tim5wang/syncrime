package com.syncrime.android.accessibility

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.syncrime.android.R
import com.syncrime.app.presentation.MainActivity

/**
 * 采集通知管理器
 * 
 * 负责管理无障碍服务的通知状态
 */
object CaptureNotificationManager {
    
    private const val CHANNEL_ID = "syncrime_capture_service"
    private const val NOTIFICATION_ID = 1001
    private const val CHANNEL_NAME = "输入采集服务"
    
    /**
     * 创建通知渠道
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示输入采集服务状态"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 显示服务启动通知
     */
    fun showServiceActiveNotification(service: Service) {
        createNotificationChannel(service)
        
        val notification = buildBaseNotification(service)
            .setContentTitle("SyncRime 输入采集")
            .setContentText("正在监听输入，提供智能推荐")
            .setSmallIcon(android.R.drawable.ic_input_get)
            .setOngoing(true)
            .build()
        
        service.startForeground(NOTIFICATION_ID, notification)
    }
    
    /**
     * 更新输入计数通知
     */
    fun updateInputCountNotification(context: Context, inputCount: Int) {
        createNotificationChannel(context)
        
        val notification = buildBaseNotification(context)
            .setContentTitle("SyncRime 输入采集")
            .setContentText("已采集 $inputCount 次输入")
            .setSmallIcon(android.R.drawable.ic_input_get)
            .setOngoing(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 显示服务暂停通知
     */
    fun showServicePausedNotification(context: Context) {
        createNotificationChannel(context)
        
        val notification = buildBaseNotification(context)
            .setContentTitle("SyncRime 输入采集已暂停")
            .setContentText("点击恢复采集")
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setOngoing(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 构建基础通知
     */
    private fun buildBaseNotification(context: Context): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }
    
    /**
     * 停止前台服务
     */
    fun stopForegroundService(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        
        if (context is Service) {
            context.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        }
    }
}
