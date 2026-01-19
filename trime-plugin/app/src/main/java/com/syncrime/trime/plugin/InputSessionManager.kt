package com.syncrime.trime.plugin

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 输入会话管理器
 * 
 * 负责管理输入会话，跟踪输入行为模式，
 * 并提供智能的内容分类和重要性分析。
 */
class InputSessionManager {
    
    companion object {
        private const val TAG = "InputSessionManager"
        
        // 会话超时时间（毫秒）
        private const val SESSION_TIMEOUT = 5 * 60 * 1000L // 5分钟
        
        // 最小会话长度
        private const val MIN_SESSION_LENGTH = 3
        
        // 重要内容关键词
        private val IMPORTANT_KEYWORDS = setOf(
            "密码", "password", "账号", "account", "用户名", "username",
            "手机号", "phone", "邮箱", "email", "地址", "address",
            "身份证", "id", "银行卡", "bank", "信用卡", "credit"
        )
    }
    
    // 输入会话数据类
    data class InputSession(
        val id: String,
        val startTime: Long,
        val endTime: Long = 0,
        val inputs: MutableList<InputRecord> = mutableListOf(),
        var isActive: Boolean = true,
        var importance: Importance = Importance.NORMAL
    )
    
    // 输入记录数据类
    data class InputRecord(
        val text: String,
        val timestamp: Long,
        val metadata: String = "",
        val inputType: InputType = InputType.TEXT,
        var importance: Importance = Importance.NORMAL,
        var category: String = ""
    )
    
    // 输入类型枚举
    enum class InputType {
        TEXT,           // 普通文本
        PASSWORD,       // 密码
        EMAIL,          // 邮箱
        PHONE,          // 电话号码
        URL,            // 网址
        NUMBER,         // 数字
        SYMBOL,         // 符号
        EMOJI,          // 表情符号
        COMMAND         // 命令
    }
    
    // 重要性级别枚举
    enum class Importance(val value: Int) {
        LOW(1),         // 低重要性
        NORMAL(2),      // 普通重要性
        HIGH(3),        // 高重要性
        CRITICAL(4)     // 关键重要性
    }
    
    // 会话状态
    private val _currentSession = MutableStateFlow<InputSession?>(null)
    val currentSession: StateFlow<InputSession?> = _currentSession.asStateFlow()
    
    private val _sessionCount = MutableStateFlow(0)
    val sessionCount: StateFlow<Int> = _sessionCount.asStateFlow()
    
    // 所有会话历史
    private val sessionHistory = ConcurrentHashMap<String, InputSession>()
    
    // 协程作用域
    private val sessionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 上下文
    private lateinit var context: Context
    
    // 会话清理任务
    private var cleanupJob: Job? = null
    
    /**
     * 初始化
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
        
        // 启动会话清理任务
        startCleanupTask()
        
        Log.i(TAG, "InputSessionManager initialized")
    }
    
    /**
     * 开始新的输入会话
     */
    fun startSession(): InputSession {
        // 结束当前会话
        _currentSession.value?.let { endSession(it) }
        
        // 创建新会话
        val sessionId = generateSessionId()
        val newSession = InputSession(
            id = sessionId,
            startTime = System.currentTimeMillis()
        )
        
        _currentSession.value = newSession
        sessionHistory[sessionId] = newSession
        _sessionCount.value = sessionHistory.size
        
        Log.d(TAG, "Started new session: $sessionId")
        return newSession
    }
    
    /**
     * 结束输入会话
     */
    fun endSession(session: InputSession? = _currentSession.value) {
        session?.let {
            it.isActive = false
            it.endTime = System.currentTimeMillis()
            
            // 分析会话重要性
            analyzeSessionImportance(it)
            
            // 保存会话
            saveSession(it)
            
            Log.d(TAG, "Ended session: ${it.id}, duration: ${it.endTime - it.startTime}ms")
        }
        
        _currentSession.value = null
    }
    
    /**
     * 添加输入记录
     */
    fun addInput(text: String, metadata: String = ""): InputRecord {
        val session = _currentSession.value ?: startSession()
        
        val inputRecord = InputRecord(
            text = text,
            timestamp = System.currentTimeMillis(),
            metadata = metadata,
            inputType = detectInputType(text),
            category = categorizeInput(text)
        )
        
        // 分析输入重要性
        inputRecord.importance = analyzeInputImportance(text, inputRecord.inputType)
        
        // 添加到会话
        session.inputs.add(inputRecord)
        
        // 更新会话重要性
        if (inputRecord.importance.value > session.importance.value) {
            session.importance = inputRecord.importance
        }
        
        Log.d(TAG, "Added input: ${text.take(20)}..., type: ${inputRecord.inputType}, importance: ${inputRecord.importance}")
        
        return inputRecord
    }
    
    /**
     * 获取会话统计
     */
    fun getSessionStatistics(): SessionStatistics {
        val allSessions = sessionHistory.values.toList()
        
        return SessionStatistics(
            totalSessions = allSessions.size,
            activeSessions = allSessions.count { it.isActive },
            totalInputs = allSessions.sumOf { it.inputs.size },
            averageSessionLength = if (allSessions.isNotEmpty()) {
                allSessions.map { it.inputs.size }.average()
            } else 0.0,
            averageSessionDuration = if (allSessions.isNotEmpty()) {
                allSessions.filter { it.endTime > 0 }
                    .map { it.endTime - it.startTime }
                    .average()
            } else 0.0,
            highImportanceSessions = allSessions.count { it.importance >= Importance.HIGH },
            criticalInputs = allSessions.sumOf { session ->
                session.inputs.count { it.importance == Importance.CRITICAL }
            }
        )
    }
    
    /**
     * 获取最近的会话
     */
    fun getRecentSessions(limit: Int = 10): List<InputSession> {
        return sessionHistory.values
            .sortedByDescending { it.startTime }
            .take(limit)
    }
    
    /**
     * 获取高重要性输入
     */
    fun getHighImportanceInputs(minImportance: Importance = Importance.HIGH): List<InputRecord> {
        return sessionHistory.values
            .flatMap { it.inputs }
            .filter { it.importance >= minImportance }
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * 搜索输入内容
     */
    fun searchInputs(query: String, limit: Int = 50): List<InputRecord> {
        return sessionHistory.values
            .flatMap { it.inputs }
            .filter { it.text.contains(query, ignoreCase = true) }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 清理旧会话
     */
    fun cleanupOldSessions(maxAge: Long = 7 * 24 * 60 * 60 * 1000L) { // 7天
        val cutoffTime = System.currentTimeMillis() - maxAge
        
        val toRemove = sessionHistory.values.filter { session ->
            !session.isActive && session.endTime < cutoffTime
        }
        
        toRemove.forEach { session ->
            sessionHistory.remove(session.id)
        }
        
        _sessionCount.value = sessionHistory.size
        
        Log.d(TAG, "Cleaned up ${toRemove.size} old sessions")
    }
    
    /**
     * 销毁管理器
     */
    fun destroy() {
        cleanupJob?.cancel()
        sessionScope.cancel()
        
        // 结束当前会话
        endSession()
        
        Log.i(TAG, "InputSessionManager destroyed")
    }
    
    // 私有方法
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }
    
    private fun detectInputType(text: String): InputType {
        return when {
            text.contains("*") && text.length > 6 -> InputType.PASSWORD
            text.contains("@") && text.contains(".") -> InputType.EMAIL
            text.matches(Regex("^1[3-9]\\d{9}$")) -> InputType.PHONE
            text.startsWith("http") -> InputType.URL
            text.matches(Regex("^\\d+$")) -> InputType.NUMBER
            text.matches(Regex("^[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$")) -> InputType.SYMBOL
            text.matches(Regex("^[\\p{So}\\p{Sk}]+$")) -> InputType.EMOJI
            text.startsWith("/") -> InputType.COMMAND
            else -> InputType.TEXT
        }
    }
    
    private fun categorizeInput(text: String): String {
        return when {
            IMPORTANT_KEYWORDS.any { keyword -> 
                text.contains(keyword, ignoreCase = true) 
            } -> "sensitive"
            
            text.matches(Regex("^\\d+$")) -> "number"
            text.contains("@") -> "contact"
            text.startsWith("http") -> "url"
            text.length > 100 -> "long_text"
            text.length < 3 -> "short_text"
            else -> "general"
        }
    }
    
    private fun analyzeInputImportance(text: String, inputType: InputType): Importance {
        // 关键信息检测
        if (IMPORTANT_KEYWORDS.any { keyword -> 
            text.contains(keyword, ignoreCase = true) 
        }) {
            return Importance.CRITICAL
        }
        
        // 输入类型重要性
        return when (inputType) {
            InputType.PASSWORD -> Importance.CRITICAL
            InputType.EMAIL, InputType.PHONE -> Importance.HIGH
            InputType.URL -> Importance.NORMAL
            InputType.COMMAND -> Importance.HIGH
            InputType.NUMBER -> Importance.NORMAL
            else -> Importance.NORMAL
        }
    }
    
    private fun analyzeSessionImportance(session: InputSession) {
        // 基于输入记录分析会话重要性
        val maxImportance = session.inputs
            .map { it.importance.value }
            .maxOrNull() ?: Importance.NORMAL.value
        
        session.importance = Importance.fromValue(maxImportance)
        
        // 长会话可能更重要
        if (session.inputs.size > 20) {
            session.importance = Importance.values()[
                minOf(session.importance.value + 1, Importance.CRITICAL.value)
            ]
        }
    }
    
    private fun saveSession(session: InputSession) {
        // 这里可以保存到数据库或文件
        // 暂时只保存在内存中
        sessionScope.launch {
            try {
                // TODO: 实现持久化存储
                Log.d(TAG, "Session saved: ${session.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save session", e)
            }
        }
    }
    
    private fun startCleanupTask() {
        cleanupJob = sessionScope.launch {
            while (isActive) {
                delay(SESSION_TIMEOUT)
                
                try {
                    // 检查并结束超时的活跃会话
                    _currentSession.value?.let { session ->
                        if (System.currentTimeMillis() - session.startTime > SESSION_TIMEOUT) {
                            endSession(session)
                        }
                    }
                    
                    // 清理旧会话
                    cleanupOldSessions()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Cleanup task failed", e)
                }
            }
        }
    }
    
    // 会话统计数据类
    data class SessionStatistics(
        val totalSessions: Int,
        val activeSessions: Int,
        val totalInputs: Int,
        val averageSessionLength: Double,
        val averageSessionDuration: Double,
        val highImportanceSessions: Int,
        val criticalInputs: Int
    )
}