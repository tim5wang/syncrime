package com.syncrime.android.accessibility

import android.content.Context
import android.util.Log
import com.syncrime.android.data.repository.InputRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 输入处理器
 * 
 * 负责处理采集到的输入内容，包括：
 * - 内容分析
 * - 分类标记
 * - 智能推荐
 * - 存储记录
 */
class InputProcessor(
    private val context: Context,
    private val inputRepository: InputRepository
) {
    
    companion object {
        private const val TAG = "InputProcessor"
    }
    
    /**
     * 处理输入内容
     */
    suspend fun processInput(
        sessionId: String,
        content: String,
        application: String,
        packageName: String,
        context: String? = null,
        characterCount: Int = content.length
    ) {
        withContext(Dispatchers.Default) {
            try {
                // 1. 内容分析
                val analysis = analyzeContent(content)
                
                // 2. 判断是否敏感
                val isSensitive = analysis.isSensitive
                
                // 3. 内容分类
                val category = analysis.category
                
                // 4. 计算置信度
                val confidence = calculateConfidence(content, analysis)
                
                // 5. 创建记录
                inputRepository.createRecord(
                    sessionId = sessionId,
                    content = content,
                    application = application,
                    context = context,
                    isSensitive = isSensitive,
                    category = category,
                    confidence = confidence,
                    isRecommended = false
                )
                
                Log.d(TAG, "处理输入：应用=$application, 长度=$characterCount, 分类=$category")
                
            } catch (e: Exception) {
                Log.e(TAG, "处理输入失败", e)
            }
        }
    }
    
    /**
     * 分析内容
     */
    private fun analyzeContent(content: String): ContentAnalysis {
        val analysis = ContentAnalysis()
        
        // 检查敏感内容
        val filterManager = InputFilterManager()
        analysis.isSensitive = filterManager.containsSensitiveContent(content)
        
        // 内容分类
        analysis.category = categorizeContent(content)
        
        // 语言检测
        analysis.language = detectLanguage(content)
        
        // 情感分析（简化版）
        analysis.sentiment = analyzeSentiment(content)
        
        return analysis
    }
    
    /**
     * 内容分类
     */
    private fun categorizeContent(content: String): String {
        // 问候语
        if (content.matches(Regex("(你好 | 您好|hello|hi|hey|早上好 | 下午好 | 晚上好).*"))) {
            return "greeting"
        }
        
        // 确认/同意
        if (content.matches(Regex("(好的 | 好的|ok|OK|好|可以|行|没问题|yes|sure).*"))) {
            return "confirmation"
        }
        
        // 感谢
        if (content.matches(Regex(".*(谢谢 | 感谢|thanks|thank you|thx).*"))) {
            return "gratitude"
        }
        
        // 告别
        if (content.matches(Regex(".*(再见 | bye|拜拜 | 回见|晚安).*"))) {
            return "farewell"
        }
        
        // 疑问句
        if (content.contains("?") || content.contains("?") || 
            content.matches(Regex(".*(吗 | 呢 | 什么 | 怎么 | 为什么|which|what|how|why).*"))) {
            return "question"
        }
        
        // 表情符号
        if (content.matches(Regex("^[\\u{1F600}-\\u{1F64F}\\u{1F300}-\\u{1F5FF}\\u{1F680}-\\u{1F6FF}\\u{1F1E0}-\\u{1F1FF}]+$", RegexOption.IGNORE_CASE))) {
            return "emoji"
        }
        
        // 链接
        if (content.contains("http://") || content.contains("https://") || content.contains("www.")) {
            return "link"
        }
        
        // 数字
        if (content.all { it.isDigit() }) {
            return "number"
        }
        
        // 英文
        if (content.all { it.isLetter() }) {
            return "english"
        }
        
        // 中文
        if (content.any { it in '\u4e00'..'\u9fff' }) {
            return "chinese"
        }
        
        return "general"
    }
    
    /**
     * 检测语言
     */
    private fun detectLanguage(content: String): String {
        return when {
            content.any { it in '\u4e00'..'\u9fff' } -> "zh"
            content.all { it.isLetter() && it.code < 128 } -> "en"
            else -> "unknown"
        }
    }
    
    /**
     * 情感分析（简化版）
     */
    private fun analyzeSentiment(content: String): String {
        val positiveWords = setOf("好", "棒", "赞", "开心", "快乐", "喜欢", "love", "like", "great", "good", "happy")
        val negativeWords = setOf("坏", "差", "讨厌", "生气", "难过", "hate", "dislike", "bad", "angry", "sad")
        
        val lowerContent = content.lowercase()
        
        val positiveCount = positiveWords.count { lowerContent.contains(it) }
        val negativeCount = negativeWords.count { lowerContent.contains(it) }
        
        return when {
            positiveCount > negativeCount -> "positive"
            negativeCount > positiveCount -> "negative"
            else -> "neutral"
        }
    }
    
    /**
     * 计算置信度
     */
    private fun calculateConfidence(content: String, analysis: ContentAnalysis): Float {
        var confidence = 1.0f
        
        // 敏感内容降低置信度
        if (analysis.isSensitive) {
            confidence *= 0.5f
        }
        
        // 过短内容降低置信度
        if (content.length < 3) {
            confidence *= 0.7f
        }
        
        // 未知语言降低置信度
        if (analysis.language == "unknown") {
            confidence *= 0.8f
        }
        
        return confidence.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * 内容分析结果
     */
    data class ContentAnalysis(
        var isSensitive: Boolean = false,
        var category: String = "general",
        var language: String = "unknown",
        var sentiment: String = "neutral"
    )
}
