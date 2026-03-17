package com.syncrime.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 输入会话实体
 * 记录每次输入会话的详细信息
 */
@Entity(tableName = "input_sessions")
data class InputSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val application: String,
    val packageName: String,
    val startTime: Long,
    var endTime: Long? = null,
    var inputCount: Int = 0,
    var characterCount: Int = 0,
    val isSynced: Boolean = false,
    val syncTimestamp: Long? = null,
    val metadata: String? = null // JSON 格式的额外元数据
) {
    val duration: Long?
        get() = endTime?.let { it - startTime }
    
    val isActive: Boolean
        get() = endTime == null
}
