package com.syncrime.shared.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.syncrime.shared.util.Converters

/**
 * 知识剪藏实体
 * 保存用户剪藏的内容
 */
@Entity(tableName = "knowledge_clips")
data class KnowledgeClip(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    
    /** 标题 */
    val title: String,
    
    /** 内容 */
    val content: String,
    
    /** 来源 URL */
    val sourceUrl: String? = null,
    
    /** 来源类型 */
    val sourceType: SourceType = SourceType.CLIP,
    
    /** 分类 */
    val category: String? = null,
    
    /** 标签列表 */
    @TypeConverters(Converters::class)
    val tags: List<String> = emptyList(),
    
    /** AI 生成的摘要 */
    val summary: String? = null,
    
    /** 图片列表 */
    @TypeConverters(Converters::class)
    val images: List<String> = emptyList(),
    
    /** 附件列表 */
    @TypeConverters(Converters::class)
    val attachments: List<String> = emptyList(),
    
    /** 查看次数 */
    val viewCount: Int = 0,
    
    /** 收藏次数 */
    val favoriteCount: Int = 0,
    
    /** 提醒时间 */
    val reminderAt: Long? = null,
    
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 来源类型
 */
enum class SourceType {
    /** 输入内容 */
    INPUT,
    
    /** 剪藏 */
    CLIP,
    
    /** 分享 */
    SHARE,
    
    /** 导入 */
    IMPORT
}
