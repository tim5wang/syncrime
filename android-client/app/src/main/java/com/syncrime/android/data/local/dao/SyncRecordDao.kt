package com.syncrime.android.data.local.dao

import androidx.room.*
import com.syncrime.android.data.local.entity.SyncRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 同步记录数据访问对象
 */
@Dao
interface SyncRecordDao {
    
    @Query("SELECT * FROM sync_records ORDER BY startTime DESC")
    fun getAllSyncRecords(): Flow<List<SyncRecordEntity>>
    
    @Query("SELECT * FROM sync_records WHERE id = :recordId")
    suspend fun getSyncRecordById(recordId: String): SyncRecordEntity?
    
    @Query("SELECT * FROM sync_records WHERE status = 'IN_PROGRESS'")
    suspend fun getInProgressSync(): SyncRecordEntity?
    
    @Query("SELECT * FROM sync_records ORDER BY startTime DESC LIMIT 1")
    fun getLatestSyncRecord(): Flow<SyncRecordEntity?>
    
    @Query("SELECT COUNT(*) FROM sync_records WHERE status = 'SUCCESS'")
    fun getSuccessfulSyncCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM sync_records WHERE date(startTime / 1000, 'unixepoch') = date('now')")
    fun getTodaySyncCount(): Flow<Int>
    
    @Query("SELECT AVG(duration) FROM sync_records WHERE status = 'SUCCESS'")
    fun getAverageSyncDuration(): Flow<Long?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncRecord(record: SyncRecordEntity): Long
    
    @Update
    suspend fun updateSyncRecord(record: SyncRecordEntity)
    
    @Delete
    suspend fun deleteSyncRecord(record: SyncRecordEntity)
    
    @Query("DELETE FROM sync_records WHERE startTime < :timestamp")
    suspend fun deleteSyncRecordsBefore(timestamp: Long): Int
}
