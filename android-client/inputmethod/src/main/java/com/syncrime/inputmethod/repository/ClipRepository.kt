package com.syncrime.inputmethod.repository

import com.syncrime.shared.data.local.dao.ClipDao
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.shared.model.SourceType
import kotlinx.coroutines.flow.Flow

/**
 * 知识剪藏仓库
 */
class ClipRepository(
    private val clipDao: ClipDao
) {
    
    /**
     * 保存剪藏
     */
    suspend fun saveClip(clip: KnowledgeClip): Long {
        return clipDao.insert(clip)
    }
    
    /**
     * 批量保存
     */
    suspend fun saveClips(clips: List<KnowledgeClip>) {
        clipDao.insertAll(clips)
    }
    
    /**
     * 更新剪藏
     */
    suspend fun updateClip(clip: KnowledgeClip) {
        clipDao.update(clip)
    }
    
    /**
     * 删除剪藏
     */
    suspend fun deleteClip(clip: KnowledgeClip) {
        clipDao.delete(clip)
    }
    
    /**
     * 获取所有剪藏
     */
    fun getAllClips(limit: Int = 20, offset: Int = 0): Flow<List<KnowledgeClip>> {
        return clipDao.getAll(limit, offset)
    }
    
    /**
     * 根据 ID 获取
     */
    suspend fun getClipById(id: Long): KnowledgeClip? {
        return clipDao.getById(id)
    }
    
    /**
     * 按分类查询
     */
    fun getClipsByCategory(category: String): Flow<List<KnowledgeClip>> {
        return clipDao.getByCategory(category)
    }
    
    /**
     * 按标签查询
     */
    fun getClipsByTag(tag: String): Flow<List<KnowledgeClip>> {
        return clipDao.getByTag(tag)
    }
    
    /**
     * 按来源类型查询
     */
    fun getClipsBySourceType(type: SourceType): Flow<List<KnowledgeClip>> {
        return clipDao.getBySourceType(type)
    }
    
    /**
     * 搜索剪藏
     */
    fun searchClips(query: String): Flow<List<KnowledgeClip>> {
        return clipDao.search(query)
    }
    
    /**
     * 获取统计
     */
    suspend fun getStats(): ClipStats {
        val total = clipDao.getTotalCount()
        val today = clipDao.getTodayCount()
        val categoryStats = clipDao.getCategoryStats()
        
        return ClipStats(
            total = total,
            today = today,
            categoryStats = categoryStats
        )
    }
    
    /**
     * 增加查看次数
     */
    suspend fun incrementViewCount(id: Long) {
        clipDao.incrementViewCount(id)
    }
    
    /**
     * 增加收藏次数
     */
    suspend fun incrementFavoriteCount(id: Long) {
        clipDao.incrementFavoriteCount(id)
    }
    
    /**
     * 删除旧剪藏
     */
    suspend fun deleteOldClips(timestamp: Long): Int {
        return clipDao.deleteBefore(timestamp)
    }
}

/**
 * 剪藏统计
 */
data class ClipStats(
    val total: Int,
    val today: Int,
    val categoryStats: Flow<List<ClipDao.CategoryStat>>
)
