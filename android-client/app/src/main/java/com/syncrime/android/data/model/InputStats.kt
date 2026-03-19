package com.syncrime.android.data.model

/**
 * 输入统计相关数据类
 */
object InputStats {
    
    /**
     * 应用统计
     */
    data class AppStat(
        val application: String,
        val count: Int
    )
    
    /**
     * 词频统计
     */
    data class WordStat(
        val content: String,
        val count: Int
    )
}
