package com.syncrime.shared.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.syncrime.shared.util.Converters

/**
 * 输入记录实体
 * 保存用户输入的内容
 */
@Entity(
    tableName = "input_records",
    indices = [
        androidx.room.Index(value = ["content"]),
        androidx.room.Index(value = ["summary"]),
        androidx.room.Index(value = ["content", "application"]),
        androidx.room.Index(value = ["createdAt"])
    ]
)
data class InputRecord(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    
    /** 会话 ID */
    val sessionId: Long,
    
    /** 输入内容 */
    val content: String,
    
    /** 应用包名 */
    val application: String,
    
    /** 分类 */
    val category: String? = null,
    
    /** 标签列表 */
    @TypeConverters(Converters::class)
    val tags: List<String> = emptyList(),
    
    /** AI 生成的摘要 */
    val summary: String? = null,
    
    /** 是否敏感信息 */
    val isSensitive: Boolean = false,
    
    /** 是否加密存储 */
    val isEncrypted: Boolean = false,
    
    /** 可见性 */
    val visibility: Visibility = Visibility.PRIVATE,
    
    /** 元数据 */
    @TypeConverters(Converters::class)
    val metadata: Map<String, String> = emptyMap(),
    
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 可见性级别
 */
enum class Visibility {
    /** 仅本地 */
    PRIVATE,
    
    /** 已同步 */
    SYNCED,
    
    /** 可分享 */
    SHARED
}
