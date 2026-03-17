package com.syncrime.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 同步记录实体
 * 记录数据同步历史
 */
@Entity(tableName = "sync_records")
data class SyncRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val status: SyncStatus,
    val syncedSessions: Int = 0,
    val syncedRecords: Int = 0,
    val errorMessage: String? = null,
    val syncType: SyncType = SyncType.AUTO,
    val bytesTransferred: Long = 0L
) {
    enum class SyncStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED,
        PARTIAL_SUCCESS
    }
    
    enum class SyncType {
        AUTO,
        MANUAL,
        INITIAL
    }
    
    val duration: Long
        get() = endTime - startTime
}
