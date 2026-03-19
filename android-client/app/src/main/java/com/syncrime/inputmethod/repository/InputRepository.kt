package com.syncrime.inputmethod.repository

import com.syncrime.shared.data.local.dao.InputDao
import com.syncrime.shared.model.InputRecord
import kotlinx.coroutines.flow.Flow

/**
 * 输入记录仓库
 */
class InputRepository(
    private val inputDao: InputDao
) {
    
    /**
     * 保存输入记录
     */
    suspend fun saveRecord(record: InputRecord): Long {
        return inputDao.insert(record)
    }
    
    /**
     * 批量保存
     */
    suspend fun saveRecords(records: List<InputRecord>) {
        inputDao.insertAll(records)
    }
    
    /**
     * 获取所有记录
     */
    fun getAllRecords(limit: Int = 20, offset: Int = 0): Flow<List<InputRecord>> {
        return inputDao.getAll(limit, offset)
    }
    
    /**
     * 根据 ID 获取
     */
    suspend fun getRecordById(id: Long): InputRecord? {
        return inputDao.getById(id)
    }
    
    /**
     * 按应用查询
     */
    fun getRecordsByApp(application: String): Flow<List<InputRecord>> {
        return inputDao.getByApplication(application)
    }
    
    /**
     * 按分类查询
     */
    fun getRecordsByCategory(category: String): Flow<List<InputRecord>> {
        return inputDao.getByCategory(category)
    }
    
    /**
     * 按标签查询
     */
    fun getRecordsByTag(tag: String): Flow<List<InputRecord>> {
        return inputDao.getByTag(tag)
    }
    
    /**
     * 搜索记录
     */
    fun searchRecords(query: String): Flow<List<InputRecord>> {
        return inputDao.search(query)
    }
    
    /**
     * 获取今日统计
     */
    fun getTodayStats(): Flow<Int> {
        return inputDao.getTodayCount()
    }
    
    /**
     * 获取总统计
     */
    fun getTotalCount(): Flow<Int> {
        return inputDao.getTotalCount()
    }
    
    /**
     * 删除记录
     */
    suspend fun deleteRecord(record: InputRecord) {
        inputDao.delete(record)
    }
    
    /**
     * 清空所有记录
     */
    suspend fun deleteAllRecords() {
        inputDao.deleteAll()
    }
}

/**
 * 今日统计
 */
data class TodayStats(
    val count: Int,
    val appStats: List<InputDao.AppStat>
)
