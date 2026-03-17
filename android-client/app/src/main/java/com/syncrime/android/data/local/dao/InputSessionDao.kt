package com.syncrime.android.data.local.dao

import androidx.room.*
import com.syncrime.android.data.local.entity.InputSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 输入会话数据访问对象
 */
@Dao
interface InputSessionDao {
    
    @Query("SELECT * FROM input_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<InputSessionEntity>>
    
    @Query("SELECT * FROM input_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): InputSessionEntity?
    
    @Query("SELECT * FROM input_sessions WHERE endTime IS NULL")
    suspend fun getActiveSessions(): List<InputSessionEntity>
    
    @Query("SELECT * FROM input_sessions WHERE isSynced = 0 ORDER BY startTime ASC")
    suspend fun getUnsyncedSessions(): List<InputSessionEntity>
    
    @Query("SELECT COUNT(*) FROM input_sessions")
    fun getTotalSessionsCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM input_sessions WHERE date(startTime / 1000, 'unixepoch') = date('now')")
    fun getTodaySessionsCount(): Flow<Int>
    
    @Query("SELECT SUM(inputCount) FROM input_sessions WHERE date(startTime / 1000, 'unixepoch') = date('now')")
    fun getTodayTotalInputs(): Flow<Int?>
    
    @Query("SELECT SUM(characterCount) FROM input_sessions WHERE date(startTime / 1000, 'unixepoch') = date('now')")
    fun getTodayTotalCharacters(): Flow<Int?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InputSessionEntity): Long
    
    @Update
    suspend fun updateSession(session: InputSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: InputSessionEntity)
    
    @Query("DELETE FROM input_sessions WHERE startTime < :timestamp")
    suspend fun deleteSessionsBefore(timestamp: Long): Int
    
    @Query("UPDATE input_sessions SET isSynced = 1, syncTimestamp = :timestamp WHERE id IN (:sessionIds)")
    suspend fun markSessionsAsSynced(sessionIds: List<String>, timestamp: Long)
    
    @Transaction
    suspend fun endSession(sessionId: String, endTime: Long = System.currentTimeMillis()) {
        val session = getSessionById(sessionId)
        session?.let {
            updateSession(it.copy(endTime = endTime))
        }
    }
}
