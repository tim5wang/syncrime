package com.syncrime.android.data.repository

import com.syncrime.android.data.local.dao.InputRecordDao
import com.syncrime.android.data.local.dao.InputSessionDao
import com.syncrime.android.data.local.entity.InputRecordEntity
import com.syncrime.android.data.local.entity.InputSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 输入数据仓库
 * 统一管理输入会话和记录的访问
 */
class InputRepository(
    private val inputSessionDao: InputSessionDao,
    private val inputRecordDao: InputRecordDao
) {
    
    // ============ Session Operations ============
    
    fun getAllSessions(): Flow<List<InputSessionEntity>> {
        return inputSessionDao.getAllSessions()
    }
    
    suspend fun getSessionById(sessionId: String): InputSessionEntity? {
        return inputSessionDao.getSessionById(sessionId)
    }
    
    suspend fun getActiveSessions(): List<InputSessionEntity> {
        return inputSessionDao.getActiveSessions()
    }
    
    suspend fun createSession(
        application: String,
        packageName: String,
        metadata: String? = null
    ): InputSessionEntity {
        val session = InputSessionEntity(
            application = application,
            packageName = packageName,
            startTime = System.currentTimeMillis(),
            metadata = metadata
        )
        inputSessionDao.insertSession(session)
        return session
    }
    
    suspend fun endSession(sessionId: String, endTime: Long = System.currentTimeMillis()) {
        inputSessionDao.endSession(sessionId, endTime)
    }
    
    suspend fun updateSessionInputCount(sessionId: String, inputCount: Int, characterCount: Int) {
        val session = inputSessionDao.getSessionById(sessionId)
        session?.let {
            inputSessionDao.updateSession(
                it.copy(
                    inputCount = inputCount,
                    characterCount = characterCount
                )
            )
        }
    }
    
    suspend fun incrementSessionInputCount(sessionId: String, characterCount: Int = 0) {
        val session = inputSessionDao.getSessionById(sessionId)
        session?.let {
            inputSessionDao.updateSession(
                it.copy(
                    inputCount = it.inputCount + 1,
                    characterCount = it.characterCount + characterCount
                )
            )
        }
    }
    
    // ============ Record Operations ============
    
    fun getAllRecords(): Flow<List<InputRecordEntity>> {
        return inputRecordDao.getAllRecords()
    }
    
    fun getRecordsBySession(sessionId: String): Flow<List<InputRecordEntity>> {
        return inputRecordDao.getRecordsBySession(sessionId)
    }
    
    suspend fun createRecord(
        sessionId: String,
        content: String,
        application: String,
        context: String? = null,
        isSensitive: Boolean = false,
        category: String? = null,
        confidence: Float = 1.0f,
        isRecommended: Boolean = false
    ): InputRecordEntity {
        val record = InputRecordEntity(
            sessionId = sessionId,
            content = content,
            timestamp = System.currentTimeMillis(),
            application = application,
            context = context,
            isSensitive = isSensitive,
            category = category,
            confidence = confidence,
            isRecommended = isRecommended
        )
        inputRecordDao.insertRecord(record)
        return record
    }
    
    suspend fun createRecords(records: List<InputRecordEntity>) {
        inputRecordDao.insertRecords(records)
    }
    
    // ============ Statistics ============
    
    fun getTotalSessionsCount(): Flow<Int> {
        return inputSessionDao.getTotalSessionsCount()
    }
    
    fun getTodaySessionsCount(): Flow<Int> {
        return inputSessionDao.getTodaySessionsCount()
    }
    
    fun getTodayTotalInputs(): Flow<Int?> {
        return inputSessionDao.getTodayTotalInputs()
    }
    
    fun getTotalRecordsCount(): Flow<Int> {
        return inputRecordDao.getTotalRecordsCount()
    }
    
    fun getTodayRecordsCount(): Flow<Int> {
        return inputRecordDao.getTodayRecordsCount()
    }
    
    fun getTodayAppStats(): Flow<List<InputRecordDao.AppStat>> {
        return inputRecordDao.getTodayAppStats()
    }
    
    fun getTodayWordFrequency(): Flow<List<InputRecordDao.WordStat>> {
        return inputRecordDao.getTodayWordFrequency()
    }
    
    // ============ Sync Operations ============
    
    suspend fun getUnsyncedSessions(): List<InputSessionEntity> {
        return inputSessionDao.getUnsyncedSessions()
    }
    
    suspend fun markSessionsAsSynced(sessionIds: List<String>) {
        inputSessionDao.markSessionsAsSynced(sessionIds, System.currentTimeMillis())
    }
    
    // ============ Cleanup ============
    
    suspend fun deleteOldSessions(daysToKeep: Int = 30) {
        val timestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        inputSessionDao.deleteSessionsBefore(timestamp)
    }
    
    suspend fun deleteOldRecords(daysToKeep: Int = 30) {
        val timestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        inputRecordDao.deleteRecordsBefore(timestamp)
    }
}
