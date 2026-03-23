package com.syncrime.app.data

import android.content.Context
import android.util.Log
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.data.local.dao.InputDao
import com.syncrime.android.data.local.dao.SearchHistoryDao
import kotlinx.coroutines.flow.*

class DataRepository private constructor(context: Context) {
    
    companion object {
        private const val TAG = "DataRepository"
        @Volatile private var INSTANCE: DataRepository? = null
        
        fun getInstance(context: Context): DataRepository {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Creating DataRepository")
                DataRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    private val inputDao: InputDao by lazy { database.inputDao() }
    private val searchHistoryDao: SearchHistoryDao by lazy { database.searchHistoryDao() }
    
    fun getTodayCount(): Flow<Int> = inputDao.getTodayCount().catch { emit(0) }
    fun getTotalCount(): Flow<Int> = inputDao.getTotalCount().catch { emit(0) }
    
    // 获取最近记录（默认展示）
    fun getRecentRecords(): Flow<List<com.syncrime.shared.model.InputRecord>> {
        return inputDao.getRecent().catch { emit(emptyList()) }
    }
    
    // 搜索记录
    fun searchRecords(query: String): Flow<List<com.syncrime.shared.model.InputRecord>> {
        return if (query.isBlank()) flowOf(emptyList())
        else inputDao.search("%$query%").catch { emit(emptyList()) }
    }
    
    // 模糊搜索记录
    fun fuzzySearchRecords(query: String): Flow<List<com.syncrime.shared.model.InputRecord>> {
        return if (query.isBlank()) flowOf(emptyList())
        else inputDao.fuzzySearch(query).catch { emit(emptyList()) }
    }
    
    // 获取搜索建议
    fun getSearchSuggestions(query: String): Flow<List<String>> {
        return if (query.isBlank()) flowOf(emptyList())
        else inputDao.getSuggestions("%$query%").catch { emit(emptyList()) }
    }
    
    // 获取搜索历史
    fun getSearchHistory(limit: Int = 20): Flow<List<String>> {
        return searchHistoryDao.getAll(limit).map { entities ->
            entities.map { it.query }.distinct()
        }.catch { emit(emptyList()) }
    }
    
    // 添加搜索历史
    suspend fun addSearchHistory(query: String, resultCount: Int = 0) {
        val entity = com.syncrime.android.data.local.entity.SearchHistoryEntity(
            query = query,
            resultCount = resultCount,
            searchType = "input"
        )
        searchHistoryDao.insert(entity)
        // 限制历史记录数量，只保留最新的50条
        searchHistoryDao.deleteOldRecords(50)
    }
    
    // 清空搜索历史
    suspend fun clearSearchHistory() {
        searchHistoryDao.deleteOldRecords(0)
    }
    
    // 获取搜索历史DAO（供外部使用）
    fun getSearchHistoryDao(): SearchHistoryDao {
        return searchHistoryDao
    }
    
    // 删除记录
    suspend fun deleteRecord(id: Long) {
        inputDao.deleteById(id)
        Log.d(TAG, "删除记录: $id")
    }
}