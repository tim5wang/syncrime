package com.syncrime.app.ai.service

import android.util.Log
import com.syncrime.app.ai.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI 服务接口
 */
interface AIService {
    
    /**
     * 智能续写
     */
    suspend fun continueWriting(context: String, prefix: String, maxLength: Int = 200): AIResponse
    
    /**
     * 智能摘要
     */
    suspend fun summarize(content: String, length: Int = 100): AIResponse
    
    /**
     * AI 对话（流式）
     */
    fun chat(message: String, history: List<ChatMessage>): Flow<AIResponse>
    
    /**
     * 文本润色
     */
    suspend fun polish(content: String, style: String = "professional"): AIResponse
    
    /**
     * 检查配置是否有效
     */
    fun isConfigured(): Boolean
}

/**
 * AI 服务实现 - 阿里云百炼（通义千问）
 */
class AIServiceImpl(
    private var config: AIConfig? = null
) : AIService {
    
    companion object {
        private const val TAG = "AIServiceImpl"
        private const val CONTENT_TYPE = "application/json"
        private const val CHARSET = "UTF-8"
    }
    
    override fun isConfigured(): Boolean = config?.apiKey?.isNotBlank() == true
    
    /**
     * 配置 AI 服务
     */
    fun configure(apiKey: String, baseUrl: String? = null, model: String? = null) {
        config = AIConfig(
            apiKey = apiKey,
            baseUrl = baseUrl ?: config?.baseUrl ?: "https://dashscope.aliyuncs.com/api/v1",
            model = model ?: config?.model ?: "qwen-plus"
        )
        Log.i(TAG, "AI 服务已配置，模型：${config?.model}")
    }
    
    override suspend fun continueWriting(
        context: String,
        prefix: String,
        maxLength: Int
    ): AIResponse = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext AIResponse.Error("AI 服务未配置，请先设置 API Key", 401)
        }
        
        try {
            val prompt = buildContinueWritingPrompt(context, prefix)
            return@withContext callAI(prompt, maxLength)
        } catch (e: Exception) {
            Log.e(TAG, "智能续写失败", e)
            return@withContext AIResponse.Error("请求失败：${e.message}", -1)
        }
    }
    
    override suspend fun summarize(content: String, length: Int): AIResponse = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext AIResponse.Error("AI 服务未配置", 401)
        }
        
        try {
            val prompt = """请用${length}字以内总结以下内容：

$content

总结："""
            return@withContext callAI(prompt, length)
        } catch (e: Exception) {
            Log.e(TAG, "智能摘要失败", e)
            return@withContext AIResponse.Error("请求失败：${e.message}", -1)
        }
    }
    
    override fun chat(message: String, history: List<ChatMessage>): Flow<AIResponse> = flow {
        if (!isConfigured()) {
            emit(AIResponse.Error("AI 服务未配置", 401))
            return@flow
        }
        
        try {
            val messages = buildChatMessages(history, message)
            val response = callChatAPI(messages)
            emit(response)
        } catch (e: Exception) {
            Log.e(TAG, "AI 对话失败", e)
            emit(AIResponse.Error("请求失败：${e.message}", -1))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun polish(content: String, style: String): AIResponse = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext AIResponse.Error("AI 服务未配置", 401)
        }
        
        try {
            val stylePrompt = when (style) {
                "formal" -> "正式、礼貌的语气"
                "casual" -> "轻松、随意的语气"
                "professional" -> "专业、商务的语气"
                else -> "流畅、自然的语气"
            }
            
            val prompt = """请用${stylePrompt}润色以下文本，保持原意但让表达更优美：

$content

润色后："""
            return@withContext callAI(prompt, 500)
        } catch (e: Exception) {
            Log.e(TAG, "文本润色失败", e)
            return@withContext AIResponse.Error("请求失败：${e.message}", -1)
        }
    }
    
    /**
     * 构建续写提示词
     */
    private fun buildContinueWritingPrompt(context: String, prefix: String): String {
        return if (context.isNotBlank()) {
            """根据以下上下文，续写内容（200 字以内）：

上下文：
$context

续写：
$prefix"""
        } else {
            """续写以下内容（200 字以内）：

$prefix"""
        }
    }
    
    /**
     * 构建对话消息
     */
    private fun buildChatMessages(
        history: List<ChatMessage>,
        newMessage: String
    ): JSONArray {
        val messages = JSONArray()
        
        // 添加系统消息
        messages.put(
            JSONObject().apply {
                put("role", "system")
                put("content", "你是一个有帮助的 AI 助手，叫 SyncRime。请用简洁、友好的中文回复。")
            }
        )
        
        // 添加历史消息
        history.forEach { msg ->
            messages.put(
                JSONObject().apply {
                    put("role", msg.role.name.lowercase())
                    put("content", msg.content)
                }
            )
        }
        
        // 添加新消息
        messages.put(
            JSONObject().apply {
                put("role", "user")
                put("content", newMessage)
            }
        )
        
        return messages
    }
    
    /**
     * 调用 AI API
     */
    private suspend fun callAI(prompt: String, maxLength: Int): AIResponse {
        val url = URL("${config?.baseUrl}/services/aigc/text-generation/generation")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", CONTENT_TYPE)
            connection.setRequestProperty("Authorization", "Bearer ${config?.apiKey}")
            connection.doOutput = true
            connection.connectTimeout = config?.timeoutSeconds?.times(1000) ?: 30000
            connection.readTimeout = config?.timeoutSeconds?.times(1000) ?: 30000
            
            // 构建请求体
            val requestBody = JSONObject().apply {
                put("model", config?.model ?: "qwen-plus")
                put("input", JSONObject().apply {
                    put("prompt", prompt)
                })
                put("parameters", JSONObject().apply {
                    put("max_tokens", maxLength)
                    put("temperature", config?.temperature ?: 0.7f)
                })
            }
            
            // 发送请求
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(charset(CHARSET)))
            }
            
            // 读取响应
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                return parseResponse(response)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "API 错误：$responseCode, $errorBody")
                return AIResponse.Error("API 错误：$responseCode", responseCode)
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * 调用对话 API
     */
    private suspend fun callChatAPI(messages: JSONArray): AIResponse {
        val url = URL("${config?.baseUrl}/services/aigc/text-generation/generation")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", CONTENT_TYPE)
            connection.setRequestProperty("Authorization", "Bearer ${config?.apiKey}")
            connection.doOutput = true
            connection.connectTimeout = config?.timeoutSeconds?.times(1000) ?: 30000
            connection.readTimeout = config?.timeoutSeconds?.times(1000) ?: 30000
            
            // 构建请求体（messages 模式）
            val requestBody = JSONObject().apply {
                put("model", config?.model ?: "qwen-plus")
                put("input", JSONObject().apply {
                    put("messages", messages)
                })
                put("parameters", JSONObject().apply {
                    put("max_tokens", config?.maxTokens ?: 1000)
                    put("temperature", config?.temperature ?: 0.7f)
                })
            }
            
            // 发送请求
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(charset(CHARSET)))
            }
            
            // 读取响应
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                return parseChatResponse(response)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "API 错误：$responseCode, $errorBody")
                return AIResponse.Error("API 错误：$responseCode", responseCode)
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * 解析续写/摘要响应
     */
    private fun parseResponse(response: String): AIResponse {
        return try {
            val json = JSONObject(response)
            val output = json.getJSONObject("output")
            val text = output.getString("text")
            
            val usage = if (output.has("usage")) {
                val usageJson = output.getJSONObject("usage")
                TokenUsage(
                    promptTokens = usageJson.optInt("input_tokens", 0),
                    completionTokens = usageJson.optInt("output_tokens", 0),
                    totalTokens = usageJson.optInt("total_tokens", 0)
                )
            } else null
            
            AIResponse.Success(text, config?.model ?: "qwen-plus", usage)
        } catch (e: Exception) {
            Log.e(TAG, "解析响应失败", e)
            AIResponse.Error("响应解析失败：${e.message}", -1)
        }
    }
    
    /**
     * 解析对话响应
     */
    private fun parseChatResponse(response: String): AIResponse {
        return try {
            val json = JSONObject(response)
            val output = json.getJSONObject("output")
            val choices = output.getJSONArray("choices")
            
            if (choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                val message = choice.getJSONObject("message")
                val content = message.getString("content")
                
                AIResponse.Success(content, config?.model ?: "qwen-plus")
            } else {
                AIResponse.Error("无响应内容", -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析对话响应失败", e)
            AIResponse.Error("响应解析失败：${e.message}", -1)
        }
    }
}

/**
 * AI 服务单例
 */
object AIServiceProvider {
    private var instance: AIServiceImpl? = null
    
    fun getService(): AIServiceImpl {
        if (instance == null) {
            instance = AIServiceImpl()
        }
        return instance!!
    }
    
    fun configure(apiKey: String, baseUrl: String? = null, model: String? = null) {
        getService().configure(apiKey, baseUrl, model)
    }
    
    fun isConfigured(): Boolean = instance?.isConfigured() == true
}
