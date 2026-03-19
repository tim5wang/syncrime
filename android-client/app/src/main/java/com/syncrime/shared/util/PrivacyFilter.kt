package com.syncrime.shared.util

import android.util.Log

/**
 * 隐私过滤器
 * 识别和过滤敏感信息
 */
class PrivacyFilter {
    
    companion object {
        private const val TAG = "PrivacyFilter"
    }
    
    // 敏感信息正则表达式
    private val sensitivePatterns = listOf(
        // 密码相关
        Regex("(?i)password[:\\s=]+\\S+"),
        Regex("(?i)passwd[:\\s=]+\\S+"),
        Regex("(?i)pwd[:\\s=]+\\S+"),
        
        // 银行卡号
        Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"),
        Regex("\\b\\d{16}\\b"),
        
        // 身份证号
        Regex("\\b\\d{17}[\\dXx]\\b"),
        Regex("\\b\\d{18}[Xx]?\\b"),
        
        // 手机号
        Regex("\\b1[3-9]\\d{9}\\b"),
        
        // 邮箱
        Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
        
        // 机密信息
        Regex("(?i)secret[:\\s=]+\\S+"),
        Regex("(?i)confidential[:\\s=]+\\S+")
    )
    
    /**
     * 检查内容是否敏感
     */
    fun isSensitive(content: String): Boolean {
        return sensitivePatterns.any { it.containsMatchIn(content) }
    }
    
    /**
     * 过滤敏感内容
     */
    fun filter(content: String): FilteredResult {
        var filtered = content
        var foundSensitive = false
        val detectedTypes = mutableListOf<String>()
        
        sensitivePatterns.forEachIndexed { index, pattern ->
            if (pattern.containsMatchIn(filtered)) {
                foundSensitive = true
                filtered = pattern.replace(filtered, "[REDACTED]")
                detectedTypes.add(getPatternName(index))
            }
        }
        
        Log.d(TAG, "Filter result: sensitive=$foundSensitive, types=$detectedTypes")
        
        return FilteredResult(
            content = filtered,
            isSensitive = foundSensitive,
            detectedTypes = detectedTypes
        )
    }
    
    /**
     * 获取模式名称
     */
    private fun getPatternName(index: Int): String {
        return when (index) {
            0, 1, 2 -> "PASSWORD"
            3, 4, 5 -> "BANK_CARD"
            6, 7 -> "ID_CARD"
            8 -> "PHONE"
            9 -> "EMAIL"
            10, 11 -> "CONFIDENTIAL"
            else -> "UNKNOWN"
        }
    }
}

/**
 * 过滤结果
 */
data class FilteredResult(
    val content: String,
    val isSensitive: Boolean,
    val detectedTypes: List<String>
)
