package com.syncrime.app.ai.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.syncrime.app.ai.model.AIResponse
import com.syncrime.app.ai.model.ChatMessage
import com.syncrime.app.ai.service.AIServiceProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AI 功能 ViewModel
 */
class AIViewModel(
    private val application: Application
) : ViewModel() {
    
    /**
     * AI 功能 UI 状态
     */
    data class AIUiState(
        val isConfigured: Boolean = false,
        val isProcessing: Boolean = false,
        val currentResult: String = "",
        val errorMessage: String? = null,
        val chatMessages: List<ChatMessage> = emptyList(),
        val chatInput: String = ""
    )
    
    // 状态流
    private val _uiState = MutableStateFlow(AIUiState())
    val uiState: StateFlow<AIUiState> = _uiState.asStateFlow()
    
    // 流式输出流
    private val _streamingOutput = MutableStateFlow("")
    val streamingOutput: StateFlow<String> = _streamingOutput.asStateFlow()
    
    init {
        checkConfiguration()
    }
    
    /**
     * 检查 AI 配置
     */
    fun checkConfiguration() {
        _uiState.value = _uiState.value.copy(
            isConfigured = AIServiceProvider.isConfigured()
        )
    }
    
    /**
     * 配置 API Key
     */
    fun configureApiKey(apiKey: String, baseUrl: String? = null, model: String? = null) {
        AIServiceProvider.configure(apiKey, baseUrl, model)
        _uiState.value = _uiState.value.copy(
            isConfigured = true,
            errorMessage = null
        )
    }
    
    /**
     * 智能续写
     */
    fun continueWriting(context: String, prefix: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                errorMessage = null
            )
            
            val service = AIServiceProvider.getService()
            val response = service.continueWriting(context, prefix)
            
            when (response) {
                is AIResponse.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        currentResult = response.content,
                        errorMessage = null
                    )
                }
                is AIResponse.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = response.message
                    )
                }
                else -> {}
            }
        }
    }
    
    /**
     * 智能摘要
     */
    fun summarize(content: String, length: Int = 100) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                errorMessage = null
            )
            
            val service = AIServiceProvider.getService()
            val response = service.summarize(content, length)
            
            when (response) {
                is AIResponse.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        currentResult = response.content,
                        errorMessage = null
                    )
                }
                is AIResponse.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = response.message
                    )
                }
                else -> {}
            }
        }
    }
    
    /**
     * 文本润色
     */
    fun polish(content: String, style: String = "professional") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                errorMessage = null
            )
            
            val service = AIServiceProvider.getService()
            val response = service.polish(content, style)
            
            when (response) {
                is AIResponse.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        currentResult = response.content,
                        errorMessage = null
                    )
                }
                is AIResponse.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = response.message
                    )
                }
                else -> {}
            }
        }
    }
    
    /**
     * 发送对话消息
     */
    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        
        viewModelScope.launch {
            val userMessage = ChatMessage(ChatMessage.Role.USER, message)
            val updatedHistory = _uiState.value.chatMessages + userMessage
            
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                chatMessages = updatedHistory,
                chatInput = "",
                errorMessage = null
            )
            
            _streamingOutput.value = ""
            
            val service = AIServiceProvider.getService()
            service.chat(message, updatedHistory).collect { response ->
                when (response) {
                    is AIResponse.Success -> {
                        val assistantMessage = ChatMessage(ChatMessage.Role.ASSISTANT, response.content)
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            chatMessages = updatedHistory + assistantMessage,
                            currentResult = response.content
                        )
                        _streamingOutput.value = response.content
                    }
                    is AIResponse.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = response.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * 设置对话输入
     */
    fun setChatInput(input: String) {
        _uiState.value = _uiState.value.copy(chatInput = input)
    }
    
    /**
     * 清除当前结果
     */
    fun clearResult() {
        _uiState.value = _uiState.value.copy(
            currentResult = "",
            errorMessage = null
        )
    }
    
    /**
     * 清除对话历史
     */
    fun clearChatHistory() {
        _uiState.value = _uiState.value.copy(
            chatMessages = emptyList()
        )
    }
}

/**
 * ViewModel Factory
 */
class AIViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AIViewModel::class.java)) {
            return AIViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
