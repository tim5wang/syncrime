package com.syncrime.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 搜索历史实体
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 搜索关键词 */
    val query: String,
    
    /** 搜索类型：input - 输入记录搜索，clip - 剪藏搜索 */
    val searchType: String = "input",
    
    /** 搜索结果数量 */
    val resultCount: Int = 0,
    
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis()
)