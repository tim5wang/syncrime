package com.syncrime.inputmethod.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.data.local.dao.InputDao
import com.syncrime.shared.model.InputRecord
import com.syncrime.shared.util.PrivacyFilter
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

/**
 * 输入采集无障碍服务
 * 
 * 监听用户输入并自动保存到知识库
 */
class InputCaptureService : AccessibilityService() {
    
    companion object {
        private const val TAG = "InputCaptureService"
        
        @Volatile
        private var instance: InputCaptureService? = null
        
        /**
         * 获取服务实例
         */
        fun getInstance(): InputCaptureService? = instance
        
        /**
         * 服务是否运行
         */
        fun isRunning(): Boolean = instance != null
    }
    
    // 协程作用域
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 数据库
    private lateinit var database: AppDatabase
    private lateinit var inputDao: InputDao
    
    // 隐私过滤器
    private val privacyFilter = PrivacyFilter()
    
    // 会话管理
    private var currentSessionId: Long = 0
    private var currentPackageName: String? = null
    private var lastEventTime: Long = 0
    private var inputCount: AtomicLong = AtomicLong(0)
    
    // 事件去重缓存 (基于内容的去重，防止重复输入)
    private val contentCache = mutableMapOf<String, Long>() // content to timestamp
    private val cacheCleanupInterval = 5000L // 5秒清理一次过期内容
    private val cacheScope = CoroutineScope(Dispatchers.Default)
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        Log.i(TAG, "✅ 无障碍服务已连接")
        
        // 初始化数据库
        database = AppDatabase.getDatabase(this)
        inputDao = database.inputDao()
        
        // 配置服务信息
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        setServiceInfo(serviceInfo)
        
        // 创建新会话
        createNewSession()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 忽略服务自身的事件
        if (event.packageName == packageName) return
        
        // 检查是否需要过滤
        if (shouldIgnoreEvent(event)) return
        
        serviceScope.launch {
            try {
                when (event.eventType) {
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                        handleTextChangedEvent(event)
                    }
                    AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                        handleViewFocusedEvent(event)
                    }
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        handleWindowStateChangedEvent(event)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理事件失败", e)
            }
        }
    }
    
    /**
     * 处理文本变化事件
     */
    private suspend fun handleTextChangedEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        val packageName = event.packageName?.toString() ?: return
        
        // 检查是否是敏感字段
        if (isSensitiveField(source)) {
            Log.d(TAG, "忽略敏感字段输入")
            return
        }
        
        // 获取输入内容
        val text = source.text?.toString() ?: return
        
        // 忽略空内容或过短内容
        if (text.isBlank() || text.length < 1) return
        
        // 隐私过滤
        val filtered = privacyFilter.filter(text)
        
        // 内容去重检查
        if (isDuplicateContent(filtered.content, packageName)) {
            Log.d(TAG, "发现重复内容，跳过保存")
            source.recycle()
            return
        }
        
        // 确保有活跃的会话
        ensureActiveSession(packageName, event.className?.toString() ?: "")
        
        // 创建输入记录
        currentSessionId.let { sessionId ->
            val record = InputRecord(
                sessionId = sessionId,
                content = filtered.content,
                application = packageName,
                category = guessCategory(packageName),
                isSensitive = filtered.isSensitive,
                metadata = mapOf(
                    "className" to (event.className?.toString() ?: ""),
                    "eventTime" to event.eventTime.toString()
                )
            )
            
            // 保存到数据库
            inputDao.insert(record)
            inputCount.incrementAndGet()
            
            // 添加到去重缓存
            addToContentCache(filtered.content, packageName)
            
            Log.d(TAG, "📝 采集输入：${filtered.content.length} 字符 | 应用：$packageName | 敏感：${filtered.isSensitive}")
        }
        
        // 回收节点
        source.recycle()
    }
    
    /**
     * 处理视图焦点事件
     */
    private suspend fun handleViewFocusedEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        val packageName = event.packageName?.toString() ?: return
        
        // 检查是否是输入框
        if (source.className?.contains("EditText") == true) {
            Log.d(TAG, "📍 输入框获得焦点：$packageName")
            ensureActiveSession(packageName, source.className?.toString() ?: "")
        }
        
        source.recycle()
    }
    
    /**
     * 处理窗口状态变化事件
     */
    private suspend fun handleWindowStateChangedEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // 窗口变化时结束当前会话
        if (currentPackageName != packageName) {
            Log.d(TAG, "🔄 窗口变化，包名从 $currentPackageName 变为 $packageName")
        }
        
        // 清除缓存
        eventCache.clear()
    }
    
    /**
     * 确保有活跃的会话
     */
    private suspend fun ensureActiveSession(packageName: String, className: String) {
        // 如果包名变化，创建新会话
        if (currentPackageName != packageName) {
            createNewSession()
            currentPackageName = packageName
            
            Log.i(TAG, "🆕 创建新会话：$currentSessionId | 应用：$packageName")
        }
    }
    
    /**
     * 创建新会话
     */
    private fun createNewSession() {
        currentSessionId = System.currentTimeMillis()
    }
    
    /**
     * 检查是否应该忽略该事件
     */
    private fun shouldIgnoreEvent(event: AccessibilityEvent): Boolean {
        val packageName = event.packageName?.toString() ?: return true
        
        // 忽略系统应用
        if (packageName.startsWith("com.android.") || 
            packageName.startsWith("com.google.android.") ||
            packageName == "android") {
            return true
        }
        
        // 忽略短时间内的重复事件
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEventTime < 50) {
            return true
        }
        lastEventTime = currentTime
        
        return false
    }
    
    /**
     * 检查是否是敏感字段
     */
    private fun isSensitiveField(node: AccessibilityNodeInfo): Boolean {
        // 检查是否是密码字段
        if (node.isPassword) {
            return true
        }
        
        // 检查类名是否包含敏感关键词
        val className = node.className?.toString() ?: false
        if (className.contains("Password", ignoreCase = true) ||
            className.contains("Secure", ignoreCase = true)) {
            return true
        }
        
        return false
    }
    
    /**
     * 猜测分类
     */
    private fun guessCategory(packageName: String): String {
        return when {
            packageName.contains("wechat") || packageName.contains("qq") || 
            packageName.contains("dingtalk") -> "聊天"
            packageName.contains("weibo") || packageName.contains("zhihu") -> "社交"
            packageName.contains("email") || packageName.contains("mail") -> "邮件"
            packageName.contains("note") || packageName.contains("memo") -> "笔记"
            packageName.contains("browser") || packageName.contains("chrome") -> "浏览"
            else -> "其他"
        }
    }
    
    /**
     * 检查是否为重复内容
     */
    private fun isDuplicateContent(content: String, packageName: String): Boolean {
        if (content.isBlank()) return true
        
        // 清理过期缓存
        cleanupExpiredCache()
        
        // 生成缓存键：应用包名 + 内容哈希
        val cacheKey = "$packageName:${content.trim()}"
        val currentTime = System.currentTimeMillis()
        
        // 检查是否已存在相同内容
        val lastTimestamp = contentCache[cacheKey]
        return if (lastTimestamp != null) {
            // 如果内容在短时间内再次出现，则认为是重复的
            val timeDiff = currentTime - lastTimestamp
            timeDiff < cacheCleanupInterval  // 5秒内认为是重复
        } else {
            false
        }
    }
    
    /**
     * 将内容添加到去重缓存
     */
    private fun addToContentCache(content: String, packageName: String) {
        if (content.isBlank()) return
        
        val cacheKey = "$packageName:${content.trim()}"
        contentCache[cacheKey] = System.currentTimeMillis()
    }
    
    /**
     * 清理过期的缓存内容
     */
    private fun cleanupExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val keysToRemove = contentCache.filter { (_, timestamp) ->
            currentTime - timestamp > cacheCleanupInterval
        }.keys
        
        keysToRemove.forEach { key ->
            contentCache.remove(key)
        }
    }
    
    /**
     * 获取采集统计
     */
    fun getStats(): CaptureStats {
        return CaptureStats(
            isRunning = isRunning(),
            sessionId = currentSessionId,
            currentApp = currentPackageName,
            totalInputs = inputCount.get()
        )
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "⚠️ 服务被中断")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        cacheScope.cancel()
        
        Log.i(TAG, "🛑 无障碍服务已销毁")
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "🔌 服务被解绑")
        return super.onUnbind(intent)
    }
}

/**
 * 采集统计
 */
data class CaptureStats(
    val isRunning: Boolean,
    val sessionId: Long,
    val currentApp: String?,
    val totalInputs: Long
)
