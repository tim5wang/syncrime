package com.syncrime.android.data.repository

import com.syncrime.android.data.local.dao.SyncRecordDao
import com.syncrime.android.data.local.entity.SyncRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 同步数据仓库
 * 管理同步历史记录
 */
class SyncRepository(
    private val syncRecordDao: SyncRecordDao
) {
    
    fun getAllSyncRecords(): Flow<List<SyncRecordEntity>> {
        return syncRecordDao.getAllSyncRecords()
    }
    
    fun getLatestSyncRecord(): Flow<SyncRecordEntity?> {
        return syncRecordDao.getLatestSyncRecord()
    }
    
    fun getTodaySyncCount(): Flow<Int> {
        return syncRecordDao.getTodaySyncCount()
    }
    
    fun getSuccessfulSyncCount(): Flow<Int> {
        return syncRecordDao.getSuccessfulSyncCount()
    }
    
    fun getAverageSyncDuration(): Flow<Long?> {
        return syncRecordDao.getAverageSyncDuration()
    }
    
    suspend fun createSyncRecord(
        status: SyncRecordEntity.SyncStatus = SyncRecordEntity.SyncStatus.PENDING,
        syncType: SyncRecordEntity.SyncType = SyncRecordEntity.SyncType.AUTO
    ): SyncRecordEntity {
        val record = SyncRecordEntity(
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis(),
            status = status,
            syncType = syncType
        )
        syncRecordDao.insertSyncRecord(record)
        return record
    }
    
    suspend fun startSync(syncType: SyncRecordEntity.SyncType = SyncRecordEntity.SyncType.AUTO): SyncRecordEntity {
        val record = SyncRecordEntity(
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis(),
            status = SyncRecordEntity.SyncStatus.IN_PROGRESS,
            syncType = syncType
        )
        syncRecordDao.insertSyncRecord(record)
        return record
    }
    
    suspend fun completeSync(
        recordId: String,
        syncedSessions: Int,
        syncedRecords: Int,
        bytesTransferred: Long = 0L
    ) {
        val record = syncRecordDao.getSyncRecordById(recordId)
        record?.let {
            syncRecordDao.updateSyncRecord(
                it.copy(
                    endTime = System.currentTimeMillis(),
                    status = SyncRecordEntity.SyncStatus.SUCCESS,
                    syncedSessions = syncedSessions,
                    syncedRecords = syncedRecords,
                    bytesTransferred = bytesTransferred
                )
            )
        }
    }
    
    suspend fun failSync(recordId: String, errorMessage: String) {
        val record = syncRecordDao.getSyncRecordById(recordId)
        record?.let {
            syncRecordDao.updateSyncRecord(
                it.copy(
                    endTime = System.currentTimeMillis(),
                    status = SyncRecordEntity.SyncStatus.FAILED,
                    errorMessage = errorMessage
                )
            )
        }
    }
    
    suspend fun getInProgressSync(): SyncRecordEntity? {
        return syncRecordDao.getInProgressSync()
    }
}
