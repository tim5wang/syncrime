package com.syncrime.shared.data.local.dao

import androidx.room.*
import com.syncrime.shared.model.InputRecord
import com.syncrime.shared.model.Visibility
import kotlinx.coroutines.flow.Flow

/**
 * 输入记录数据访问对象
 */
@Dao
interface InputDao {
    
    /**
     * 插入输入记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InputRecord): Long
    
    /**
     * 批量插入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<InputRecord>)
    
    /**
     * 更新记录
     */
    @Update
    suspend fun update(record: InputRecord)
    
    /**
     * 删除记录
     */
    @Delete
    suspend fun delete(record: InputRecord)
    
    /**
     * 根据 ID 查询
     */
    @Query("SELECT * FROM input_records WHERE id = :id")
    suspend fun getById(id: Long): InputRecord?
    
    /**
     * 获取所有记录（分页）
     */
    @Query("SELECT * FROM input_records ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun getAll(limit: Int = 20, offset: Int = 0): Flow<List<InputRecord>>
    
    /**
     * 获取最近记录（用于默认展示）
     */
    @Query("SELECT * FROM input_records ORDER BY createdAt DESC LIMIT 50")
    fun getRecent(): Flow<List<InputRecord>>
    
    /**
     * 按应用查询
     */
    @Query("SELECT * FROM input_records WHERE application = :application ORDER BY createdAt DESC")
    fun getByApplication(application: String): Flow<List<InputRecord>>
    
    /**
     * 按分类查询
     */
    @Query("SELECT * FROM input_records WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<InputRecord>>
    
    /**
     * 按标签查询
     */
    @Query("SELECT * FROM input_records WHERE tags LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    fun getByTag(tag: String): Flow<List<InputRecord>>
    
    /**
     * 全文搜索
     */
    @Query("""
        SELECT * FROM input_records 
        WHERE content LIKE '%' || :query || '%' 
           OR summary LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun search(query: String): Flow<List<InputRecord>>
    
    /**
     * 统计今日输入数量
     */
    @Query("""
        SELECT COUNT(*) FROM input_records 
        WHERE date(createdAt / 1000, 'unixepoch') = date('now')
    """)
    fun getTodayCount(): Flow<Int>
    
    /**
     * 统计总数量
     */
    @Query("SELECT COUNT(*) FROM input_records")
    fun getTotalCount(): Flow<Int>
    
    /**
     * 统计应用分布
     */
    @Query("""
        SELECT application, COUNT(*) as count 
        FROM input_records 
        WHERE date(createdAt / 1000, 'unixepoch') = date('now')
        GROUP BY application 
        ORDER BY count DESC
    """)
    fun getTodayAppStats(): Flow<List<AppStat>>
    
    /**
     * 删除旧记录
     */
    @Query("DELETE FROM input_records WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long): Int
    
    /**
     * 清空所有记录
     */
    @Query("DELETE FROM input_records")
    suspend fun deleteAll()
    
    /**
     * 应用统计
     */
    data class AppStat(
        val application: String,
        val count: Int
    )
}
