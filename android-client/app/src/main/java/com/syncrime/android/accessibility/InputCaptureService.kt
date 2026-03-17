package com.syncrime.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.syncrime.android.data.local.database.SyncRimeDatabase
import com.syncrime.android.data.repository.InputRepository
import kotlinx.coroutines.*
import android.util.Log

/**
 * 输入采集无障碍服务
 * 
 * 负责监听和采集用户输入内容，用于智能推荐和同步
 * 
 * ⚠️ 隐私保护：
 * - 自动过滤密码等敏感字段
 * - 支持敏感词过滤
 * - 用户可随时关闭服务
 */
class InputCaptureService : AccessibilityService() {
    
    companion object {
        private const val TAG = "InputCaptureService"
        
        @Volatile
        private var instance: InputCaptureService? = null
        
        fun getInstance(): InputCaptureService? = instance
        
        fun isRunning(): Boolean = instance != null
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private lateinit var database: SyncRimeDatabase
    private lateinit var inputRepository: InputRepository
    private lateinit var inputFilterManager: InputFilterManager
    private lateinit var inputProcessor: InputProcessor
    
    private var currentSessionId: String? = null
    private var currentPackageName: String? = null
    private var lastEventTime: Long = 0
    private var inputCount: Int = 0
    
    // 事件去重缓存
    private val eventCache = mutableSetOf<String>()
    private val cacheScope = CoroutineScope(Dispatchers.Default)
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        Log.i(TAG, "无障碍服务已连接")
        
        // 初始化组件
        database = SyncRimeDatabase.getDatabase(this)
        inputRepository = InputRepository(database.inputSessionDao(), database.inputRecordDao())
        inputFilterManager = InputFilterManager()
        inputProcessor = InputProcessor(this, inputRepository)
        
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
        
        // 显示服务启动通知
        CaptureNotificationManager.showServiceActiveNotification(this)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = getEventTypeString(event.eventType)
        Log.d(TAG, "收到事件：$eventType, 包名：${event.packageName}")
        
        // 忽略服务自身的事件
        if (event.packageName == packageName) return
        
        // 检查是否需要过滤
        if (shouldIgnoreEvent(event)) return
        
        // 事件去重（100ms 内的相同事件只处理一次）
        val eventKey = "${event.packageName}_${event.eventType}_${System.currentTimeMillis() / 100}"
        if (!eventCache.add(eventKey)) return
        
        // 清理缓存
        cacheScope.launch {
            delay(100)
            eventCache.remove(eventKey)
        }
        
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
        if (inputFilterManager.isSensitiveField(source)) {
            Log.d(TAG, "忽略敏感字段输入")
            return
        }
        
        // 获取输入内容
        val text = source.text?.toString() ?: return
        
        // 忽略空内容或过短内容
        if (text.isBlank() || text.length < 1) return
        
        // 检查是否包含敏感信息
        if (inputFilterManager.containsSensitiveContent(text)) {
            Log.d(TAG, "过滤敏感内容")
            return
        }
        
        // 确保有活跃的会话
        ensureActiveSession(packageName, event.className?.toString() ?: "")
        
        // 记录输入
        currentSessionId?.let { sessionId ->
            val characterCount = text.length
            inputProcessor.processInput(
                sessionId = sessionId,
                content = text,
                application = getApplicationName(packageName),
                packageName = packageName,
                context = event.className?.toString(),
                characterCount = characterCount
            )
            inputCount++
            
            // 更新通知
            if (inputCount % 10 == 0) {
                CaptureNotificationManager.updateInputCountNotification(this, inputCount)
            }
        }
    }
    
    /**
     * 处理视图焦点事件
     */
    private suspend fun handleViewFocusedEvent(event: AccessibilityEvent) {
        val source = event.source ?: return
        val packageName = event.packageName?.toString() ?: return
        
        // 检查是否是输入框
        if (source.className?.contains("EditText") == true) {
            Log.d(TAG, "输入框获得焦点：$packageName")
            ensureActiveSession(packageName, source.className?.toString() ?: "")
        }
    }
    
    /**
     * 处理窗口状态变化事件
     */
    private suspend fun handleWindowStateChangedEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // 窗口变化时结束当前会话
        currentSessionId?.let { sessionId ->
            inputRepository.endSession(sessionId)
            currentSessionId = null
            Log.d(TAG, "窗口变化，结束会话：$sessionId")
        }
        
        // 清除缓存
        eventCache.clear()
    }
    
    /**
     * 确保有活跃的会话
     */
    private suspend fun ensureActiveSession(packageName: String, className: String) {
        // 如果包名变化，结束旧会话
        if (currentPackageName != packageName) {
            currentSessionId?.let { sessionId ->
                inputRepository.endSession(sessionId)
            }
            currentSessionId = null
            
            // 创建新会话
            val session = inputRepository.createSession(
                application = getApplicationName(packageName),
                packageName = packageName,
                metadata = "{\"className\":\"$className\"}"
            )
            currentSessionId = session.id
            currentPackageName = packageName
            inputCount = 0
            
            Log.i(TAG, "创建新会话：${session.id}, 应用：${session.application}")
        }
    }
    
    /**
     * 检查是否应该忽略该事件
     */
    private fun shouldIgnoreEvent(event: AccessibilityEvent): Boolean {
        // 忽略系统应用
        val packageName = event.packageName?.toString() ?: return true
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
     * 获取应用名称
     */
    private fun getApplicationName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val appInfo = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    /**
     * 获取事件类型字符串
     */
    private fun getEventTypeString(eventType: Int): String {
        return when (eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TYPE_VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "TYPE_VIEW_FOCUSED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
            else -> "TYPE_$eventType"
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "服务被中断")
        serviceScope.launch {
            currentSessionId?.let { sessionId ->
                inputRepository.endSession(sessionId)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        cacheScope.cancel()
        
        Log.i(TAG, "无障碍服务已销毁")
        
        // 结束当前会话
        serviceScope.launch {
            currentSessionId?.let { sessionId ->
                inputRepository.endSession(sessionId)
            }
        }
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "服务被解绑")
        return super.onUnbind(intent)
    }
}
