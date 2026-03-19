package com.syncrime.app.ai.model

/**
 * AI 请求类型
 */
sealed class AIRequest {
    /**
     * 智能续写
     * @param context 上下文文本
     * @param prefix 已输入的前缀
     */
    data class ContinueWriting(
        val context: String,
        val prefix: String,
        val maxLength: Int = 200
    ) : AIRequest()
    
    /**
     * 智能摘要
     * @param content 原始内容
     * @param length 摘要长度（字数）
     */
    data class Summarize(
        val content: String,
        val length: Int = 100
    ) : AIRequest()
    
    /**
     * AI 对话
     * @param message 用户消息
     * @param conversationHistory 对话历史
     */
    data class Chat(
        val message: String,
        val conversationHistory: List<ChatMessage> = emptyList()
    ) : AIRequest()
    
    /**
     * 文本润色
     * @param content 原始文本
     * @param style 风格（formal, casual, professional）
     */
    data class Polish(
        val content: String,
        val style: String = "professional"
    ) : AIRequest()
}

/**
 * 对话消息
 */
data class ChatMessage(
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
}

/**
 * AI 响应
 */
sealed class AIResponse {
    /**
     * 成功响应
     */
    data class Success(
        val content: String,
        val model: String = "qwen",
        val usage: TokenUsage? = null
    ) : AIResponse()
    
    /**
     * 错误响应
     */
    data class Error(
        val message: String,
        val code: Int = -1
    ) : AIResponse()
    
    /**
     * 流式响应片段
     */
    data class Streaming(
        val content: String,
        val isComplete: Boolean = false
    ) : AIResponse()
}

/**
 * Token 使用统计
 */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * AI 配置
 */
data class AIConfig(
    val apiKey: String,
    val baseUrl: String = "https://dashscope.aliyuncs.com/api/v1",
    val model: String = "qwen-plus",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val timeoutSeconds: Int = 30
)
