package com.syncrime.android.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * 同步调度器
 * 管理后台同步任务的调度
 */
object SyncScheduler {
    
    /**
     * 初始化同步调度
     * 应该在 Application  onCreate 中调用
     */
    fun initialize(context: Context) {
        val syncInterval = getSyncInterval(context)
        schedulePeriodicSync(context, syncInterval)
    }
    
    /**
     * 调度定期同步任务
     */
    fun schedulePeriodicSync(
        context: Context,
        intervalMinutes: Long = 15
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            WorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS
        )
        .addTag(SyncWorker.WORK_NAME)
        .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    /**
     * 触发立即同步
     */
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()
        
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
    
    /**
     * 取消所有同步任务
     */
    fun cancelAllSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }
    
    /**
     * 获取同步间隔（从设置中读取）
     */
    private fun getSyncInterval(context: Context): Long {
        val prefs = context.getSharedPreferences("syncrime_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("sync_interval", 15) // 默认 15 分钟
    }
    
    /**
     * 更新同步间隔
     */
    fun updateSyncInterval(context: Context, intervalMinutes: Long) {
        val prefs = context.getSharedPreferences("syncrime_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("sync_interval", intervalMinutes).apply()
        
        // 重新调度
        schedulePeriodicSync(context, intervalMinutes)
    }
}
