package com.syncrime.android.data.local.dao

import androidx.room.*
import com.syncrime.android.data.local.entity.InputRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 输入记录数据访问对象
 */
@Dao
interface InputRecordDao {
    
    @Query("SELECT * FROM input_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<InputRecordEntity>>
    
    @Query("SELECT * FROM input_records WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getRecordsBySession(sessionId: String): Flow<List<InputRecordEntity>>
    
    @Query("SELECT * FROM input_records WHERE id = :recordId")
    suspend fun getRecordById(recordId: String): InputRecordEntity?
    
    @Query("SELECT * FROM input_records WHERE application = :application ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecordsByApp(application: String, limit: Int = 100): Flow<List<InputRecordEntity>>
    
    @Query("SELECT COUNT(*) FROM input_records")
    fun getTotalRecordsCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM input_records WHERE date(timestamp / 1000, 'unixepoch') = date('now')")
    fun getTodayRecordsCount(): Flow<Int>
    
    @Query("SELECT application, COUNT(*) as count FROM input_records WHERE date(timestamp / 1000, 'unixepoch') = date('now') GROUP BY application ORDER BY count DESC")
    fun getTodayAppStats(): Flow<List<AppStat>>
    
    @Query("SELECT content, COUNT(*) as count FROM input_records WHERE date(timestamp / 1000, 'unixepoch') = date('now') GROUP BY content ORDER BY count DESC LIMIT 20")
    fun getTodayWordFrequency(): Flow<List<WordStat>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: InputRecordEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<InputRecordEntity>)
    
    @Update
    suspend fun updateRecord(record: InputRecordEntity)
    
    @Delete
    suspend fun deleteRecord(record: InputRecordEntity)
    
    @Query("DELETE FROM input_records WHERE sessionId = :sessionId")
    suspend fun deleteRecordsBySession(sessionId: String): Int
    
    @Query("DELETE FROM input_records WHERE timestamp < :timestamp")
    suspend fun deleteRecordsBefore(timestamp: Long): Int
    
    @Query("SELECT * FROM input_records WHERE isRecommended = 1 ORDER BY timestamp DESC LIMIT 10")
    fun getRecommendedRecords(): Flow<List<InputRecordEntity>>
    
    data class AppStat(
        val application: String,
        val count: Int
    )
    
    data class WordStat(
        val content: String,
        val count: Int
    )
}
