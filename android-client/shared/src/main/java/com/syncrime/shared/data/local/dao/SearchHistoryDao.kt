package com.syncrime.shared.data.local.dao

import androidx.room.*
import com.syncrime.shared.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 搜索历史数据访问对象
 */
@Dao
interface SearchHistoryDao {
    
    /**
     * 插入搜索历史
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistoryEntity): Long
    
    /**
     * 获取所有搜索历史（按时间倒序）
     */
    @Query("SELECT * FROM search_history ORDER BY createdAt DESC LIMIT :limit")
    fun getAll(limit: Int = 20): Flow<List<SearchHistoryEntity>>
    
    /**
     * 搜索建议（前缀匹配）
     */
    @Query("SELECT DISTINCT query FROM search_history WHERE query LIKE :prefix || '%' ORDER BY createdAt DESC LIMIT :limit")
    fun getSuggestions(prefix: String, limit: Int = 10): Flow<List<String>>
    
    /**
     * 删除单条历史
     */
    @Delete
    suspend fun delete(history: SearchHistoryEntity)
    
    /**
     * 清空指定类型的历史
     */
    @Query("DELETE FROM search_history WHERE searchType = :type")
    suspend fun clearByType(type: String)
    
    /**
     * 删除旧记录（保留最近N条）
     */
    @Query("DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY createdAt DESC LIMIT :keepCount)")
    suspend fun deleteOldRecords(keepCount: Int = 50)
}