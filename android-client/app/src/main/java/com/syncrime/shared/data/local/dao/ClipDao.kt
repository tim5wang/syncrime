package com.syncrime.shared.data.local.dao

import androidx.room.*
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.shared.model.SourceType
import kotlinx.coroutines.flow.Flow

/**
 * 知识剪藏数据访问对象
 */
@Dao
interface ClipDao {
    
    /**
     * 插入剪藏
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clip: KnowledgeClip): Long
    
    /**
     * 批量插入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clips: List<KnowledgeClip>)
    
    /**
     * 更新剪藏
     */
    @Update
    suspend fun update(clip: KnowledgeClip)
    
    /**
     * 删除剪藏
     */
    @Delete
    suspend fun delete(clip: KnowledgeClip)
    
    /**
     * 根据 ID 查询
     */
    @Query("SELECT * FROM knowledge_clips WHERE id = :id")
    suspend fun getById(id: Long): KnowledgeClip?
    
    /**
     * 获取所有剪藏（分页）
     */
    @Query("SELECT * FROM knowledge_clips ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun getAll(limit: Int = 20, offset: Int = 0): Flow<List<KnowledgeClip>>
    
    /**
     * 按分类查询
     */
    @Query("SELECT * FROM knowledge_clips WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<KnowledgeClip>>
    
    /**
     * 按标签查询
     */
    @Query("SELECT * FROM knowledge_clips WHERE tags LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    fun getByTag(tag: String): Flow<List<KnowledgeClip>>
    
    /**
     * 按来源类型查询
     */
    @Query("SELECT * FROM knowledge_clips WHERE sourceType = :type ORDER BY createdAt DESC")
    fun getBySourceType(type: SourceType): Flow<List<KnowledgeClip>>
    
    /**
     * 全文搜索
     */
    @Query("""
        SELECT * FROM knowledge_clips 
        WHERE title LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%'
           OR summary LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN title LIKE '%' || :query || '%' THEN 1
                WHEN content LIKE '%' || :query || '%' THEN 2
                ELSE 3
            END,
            createdAt DESC
    """)
    fun search(query: String): Flow<List<KnowledgeClip>>
    
    /**
     * 统计总数
     */
    @Query("SELECT COUNT(*) FROM knowledge_clips")
    fun getTotalCount(): Flow<Int>
    
    /**
     * 统计今日新增
     */
    @Query("""
        SELECT COUNT(*) FROM knowledge_clips 
        WHERE date(createdAt / 1000, 'unixepoch') = date('now')
    """)
    fun getTodayCount(): Flow<Int>
    
    /**
     * 统计分类分布
     */
    @Query("""
        SELECT category, COUNT(*) as count 
        FROM knowledge_clips 
        WHERE category IS NOT NULL
        GROUP BY category 
        ORDER BY count DESC
    """)
    fun getCategoryStats(): Flow<List<CategoryStat>>
    
    /**
     * 更新查看次数
     */
    @Query("UPDATE knowledge_clips SET viewCount = viewCount + 1 WHERE id = :id")
    suspend fun incrementViewCount(id: Long)
    
    /**
     * 更新收藏次数
     */
    @Query("UPDATE knowledge_clips SET favoriteCount = favoriteCount + 1 WHERE id = :id")
    suspend fun incrementFavoriteCount(id: Long)
    
    /**
     * 删除旧记录
     */
    @Query("DELETE FROM knowledge_clips WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long): Int
    
    /**
     * 分类统计
     */
    data class CategoryStat(
        val category: String?,
        val count: Int
    )
}
