package com.syncrime.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.syncrime.android.data.local.database.SyncRimeDatabase
import com.syncrime.android.data.local.entity.SyncRecordEntity
import com.syncrime.android.data.repository.InputRepository
import com.syncrime.android.data.repository.SyncRepository
import kotlinx.coroutines.flow.first

/**
 * 数据同步后台 Worker
 * 使用 WorkManager 实现定时同步
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    
    private val database by lazy { SyncRimeDatabase.getDatabase(appContext) }
    private val inputRepository by lazy {
        InputRepository(database.inputSessionDao(), database.inputRecordDao())
    }
    private val syncRepository by lazy { SyncRepository(database.syncRecordDao()) }
    
    override suspend fun doWork(): Result {
        return try {
            // 创建同步记录
            val syncRecord = syncRepository.startSync(
                if (runAttemptCount == 0) 
                    SyncRecordEntity.SyncType.AUTO 
                else 
                    SyncRecordEntity.SyncType.MANUAL
            )
            
            // 获取未同步的会话
            val unsyncedSessions = inputRepository.getUnsyncedSessions()
            
            if (unsyncedSessions.isEmpty()) {
                // 没有需要同步的数据
                syncRepository.completeSync(syncRecord.id, 0, 0)
                return Result.success()
            }
            
            // TODO: 实现实际的网络同步逻辑
            // 1. 上传数据到云端
            // 2. 下载云端数据
            // 3. 处理冲突
            // 4. 更新本地数据库
            
            // 模拟同步过程
            kotlinx.coroutines.delay(timeMillis = 1000L * (runAttemptCount + 1))
            
            // 标记为已同步
            inputRepository.markSessionsAsSynced(unsyncedSessions.map { it.id })
            
            // 完成同步
            syncRepository.completeSync(
                recordId = syncRecord.id,
                syncedSessions = unsyncedSessions.size,
                syncedRecords = 0, // TODO: 实际记录数
                bytesTransferred = 0L // TODO: 实际传输字节数
            )
            
            Result.success()
        } catch (e: Exception) {
            // 记录错误
            val syncRecord = syncRepository.getInProgressSync()
            syncRecord?.let {
                syncRepository.failSync(it.id, e.message ?: "Unknown error")
            }
            
            // 根据错误类型决定是否重试
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    companion object {
        const val WORK_NAME = "syncrime_sync_worker"
        private const val MAX_RETRY_COUNT = 3
    }
}
