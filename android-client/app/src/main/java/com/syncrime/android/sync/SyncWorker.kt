package com.syncrime.android.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.syncrime.android.data.local.database.SyncRimeDatabase
import com.syncrime.android.data.local.entity.SyncRecordEntity
import com.syncrime.android.data.repository.InputRepository
import com.syncrime.android.data.repository.SyncRepository
import com.syncrime.android.network.AuthService
import com.syncrime.android.network.SyncService
import com.syncrime.android.network.InputRecordData
import kotlinx.coroutines.flow.first

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    
    companion object {
        const val WORK_NAME = "syncrime_sync_worker"
        private const val TAG = "SyncWorker"
        private const val MAX_RETRY_COUNT = 3
    }
    
    private val database by lazy { SyncRimeDatabase.getDatabase(appContext) }
    private val inputRepository by lazy {
        InputRepository(database.inputSessionDao(), database.inputRecordDao())
    }
    private val syncRepository by lazy { SyncRepository(database.syncRecordDao()) }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "开始同步任务")
        
        // 检查是否已登录
        if (!AuthService.isLoggedIn()) {
            Log.w(TAG, "用户未登录，跳过同步")
            return Result.success()
        }
        
        return try {
            val syncRecord = syncRepository.startSync(
                if (runAttemptCount == 0) SyncRecordEntity.SyncType.AUTO else SyncRecordEntity.SyncType.MANUAL
            )
            
            // 1. 推送本地数据到服务器
            val pushResult = pushToServer()
            
            if (!pushResult) {
                syncRepository.failSync(syncRecord.id, "推送失败")
                return if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
            }
            
            // 2. 从服务器拉取数据
            val pullResult = pullFromServer()
            
            if (!pullResult) {
                syncRepository.failSync(syncRecord.id, "拉取失败")
                return if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
            }
            
            // 3. 获取同步状态
            val status = SyncService.getSyncStatus()
            
            syncRepository.completeSync(
                recordId = syncRecord.id,
                syncedSessions = 0,
                syncedRecords = status?.totalRecords ?: 0,
                bytesTransferred = 0L
            )
            
            Log.d(TAG, "同步完成，总记录数: ${status?.totalRecords}")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "同步失败", e)
            val syncRecord = syncRepository.getInProgressSync()
            syncRecord?.let { syncRepository.failSync(it.id, e.message ?: "Unknown error") }
            
            if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.failure()
        }
    }
    
    private suspend fun pushToServer(): Boolean {
        return try {
            // 获取本地未同步的记录
            val localRecords = inputRepository.getUnsyncedSessions()
            
            if (localRecords.isEmpty()) {
                Log.d(TAG, "没有需要推送的数据")
                return true
            }
            
            // 转换为网络数据格式
            val records = localRecords.map { session ->
                InputRecordData(
                    content = session.toString(), // 简化处理
                    app = "unknown",
                    timestamp = System.currentTimeMillis()
                )
            }
            
            // 获取设备ID
            val deviceId = android.provider.Settings.Secure.getString(
                applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            when (val result = SyncService.pushRecords(records, deviceId)) {
                is com.syncrime.android.network.SyncResult.Success -> {
                    Log.d(TAG, "推送成功: ${result.syncedCount} 条记录")
                    true
                }
                is com.syncrime.android.network.SyncResult.Error -> {
                    Log.e(TAG, "推送失败: ${result.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "推送异常", e)
            false
        }
    }
    
    private suspend fun pullFromServer(): Boolean {
        return try {
            // 获取上次同步时间
            val lastSyncRecord = syncRepository.getLatestSyncRecord().first()
            val lastSyncTime = lastSyncRecord?.endTime ?: 0L
            
            when (val result = SyncService.pullRecords(lastSyncTime)) {
                is com.syncrime.android.network.PullResult.Success -> {
                    Log.d(TAG, "拉取成功: ${result.records.size} 条记录")
                    // TODO: 将拉取的数据保存到本地数据库
                    true
                }
                is com.syncrime.android.network.PullResult.Error -> {
                    Log.e(TAG, "拉取失败: ${result.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "拉取异常", e)
            false
        }
    }
}
