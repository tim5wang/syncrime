package com.syncrime.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 输入记录实体
 * 记录单次输入内容（脱敏后）
 */
@Entity(tableName = "input_records")
data class InputRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val content: String, // 加密或脱敏后的内容
    val timestamp: Long,
    val context: String? = null, // 上下文信息
    val application: String,
    val isSensitive: Boolean = false,
    val category: String? = null, // 输入分类
    val confidence: Float = 1.0f, // 智能推荐置信度
    val isRecommended: Boolean = false // 是否为推荐输入
)
