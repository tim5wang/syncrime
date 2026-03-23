package com.syncrime.app.data

import android.content.Context
import android.util.Log
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.data.local.dao.InputDao
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
    
    fun getTodayCount(): Flow<Int> = inputDao.getTodayCount().catch { emit(0) }
    fun getTotalCount(): Flow<Int> = inputDao.getTotalCount().catch { emit(0) }
    
    // 获取最近记录（默认展示）
    fun getRecentRecords(): Flow<List<com.syncrime.shared.model.InputRecord>> {
        return inputDao.getRecent().catch { emit(emptyList()) }
    }
    
    // 搜索记录
    fun searchRecords(query: String): Flow<List<com.syncrime.shared.model.InputRecord>> {
        return if (query.isBlank()) flowOf(emptyList())
        else inputDao.search(query).catch { emit(emptyList()) }
    }
    
    // 删除记录
    suspend fun deleteRecord(id: Long) {
        inputDao.deleteById(id)
        Log.d(TAG, "删除记录: $id")
    }
}