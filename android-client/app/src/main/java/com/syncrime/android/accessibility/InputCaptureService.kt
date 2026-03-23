package com.syncrime.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.model.InputRecord
import com.syncrime.android.util.TextSimilarity
import kotlinx.coroutines.*

class InputCaptureService : AccessibilityService() {
    
    companion object {
        private const val TAG = "InputCaptureService"
        private const val SAVE_DELAY = 3000L // 3秒无输入后保存
        private const val MIN_LENGTH = 2 // 最小保存长度
        private const val DEDUP_TIME_WINDOW = 60_000L // 60秒内的去重时间窗口
        private const val SIMILARITY_THRESHOLD = 0.85f // 相似度阈值
        private var instance: InputCaptureService? = null
        fun isRunning(): Boolean = instance != null
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var inputCount: Int = 0
    private var currentText: String = ""
    private var currentPackage: String = ""
    private var lastSavedText: String = ""
    private var lastSavedTime: Long = 0
    private var saveJob: Job? = null
    private var hasLeftInputField: Boolean = false // 是否离开输入框
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "=== 无障碍服务已连接 ===")
        
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        setServiceInfo(serviceInfo)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == packageName) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // 离开当前界面，标记输入结束
                if (currentText.isNotEmpty()) {
                    hasLeftInputField = true
                    Log.d(TAG, "离开界面，标记输入结束")
                    saveCurrentInput(forceSave = true)
                }
                currentPackage = event.packageName?.toString() ?: "unknown"
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                // 焦点变化，可能是切换输入框
                if (currentText.isNotEmpty()) {
                    hasLeftInputField = true
                    Log.d(TAG, "焦点变化，保存当前输入")
                    scheduleSave()
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.joinToString("") ?: ""
                if (text.isBlank() || shouldFilter(text)) return
                
                // 重置离开标记
                hasLeftInputField = false
                
                currentText = text
                currentPackage = event.packageName?.toString() ?: currentPackage
                scheduleSave()
            }
        }
    }
    
    override fun onInterrupt() {}
    
    override fun onDestroy() {
        saveCurrentInput(forceSave = true)
        instance = null
        serviceScope.cancel()
    }
    
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = serviceScope.launch {
            delay(SAVE_DELAY)
            saveCurrentInput()
        }
    }
    
    private fun saveCurrentInput(forceSave: Boolean = false) {
        val text = currentText
        
        if (!forceSave) {
            currentText = ""
        }
        
        if (text.isBlank() || text.length < MIN_LENGTH) return
        
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@InputCaptureService)
                val currentTime = System.currentTimeMillis()
                
                // 1. 前缀去重：新输入包含上次保存的内容，更新而不是新增
                val isPrefixUpdate = lastSavedText.isNotEmpty() && 
                    text.startsWith(lastSavedText) && 
                    text.length > lastSavedText.length + 2
                
                // 2. 时间窗口内相似度去重
                var shouldSkip = false
                if (!isPrefixUpdate && currentTime - lastSavedTime < DEDUP_TIME_WINDOW) {
                    val recentRecords = database.inputDao().getRecentSync(5)
                    for (record in recentRecords) {
                        // 如果离开输入框后重新输入，不做相似度去重
                        if (hasLeftInputField) {
                            Log.d(TAG, "离开后重新输入，跳过去重")
                            break
                        }
                        
                        val similarity = TextSimilarity.calculateSimilarity(text, record.content)
                        if (similarity > SIMILARITY_THRESHOLD) {
                            Log.d(TAG, "相似度去重: ${(similarity * 100).toInt()}% - 跳过")
                            shouldSkip = true
                            break
                        }
                    }
                }
                
                if (shouldSkip) return@launch
                
                // 删除被更新的旧记录
                if (isPrefixUpdate) {
                    cleanupShortRecords(database, text)
                }
                
                val record = InputRecord(
                    id = currentTime,
                    sessionId = currentTime / 10000,
                    content = text,
                    application = getAppName(currentPackage),
                    category = null,
                    tags = emptyList(),
                    isSensitive = false
                )
                database.inputDao().insert(record)
                lastSavedText = text
                lastSavedTime = currentTime
                inputCount++
                
                Log.i(TAG, "✅ 保存输入 #$inputCount (${if (isPrefixUpdate) "更新" else "新增"}${if (hasLeftInputField) ", 离开后" else ""}): \"${text.take(30)}...\"")
                
                // 重置标记
                hasLeftInputField = false
                
            } catch (e: Exception) {
                Log.e(TAG, "保存失败", e)
            }
        }
    }
    
    private suspend fun cleanupShortRecords(database: AppDatabase, newText: String) {
        try {
            val recentRecords = database.inputDao().getRecentSync(5)
            for (record in recentRecords) {
                if (newText.contains(record.content) && newText.length > record.content.length + 2) {
                    database.inputDao().delete(record)
                    Log.d(TAG, "删除短记录: ${record.content.take(20)}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理失败", e)
        }
    }
    
    private fun shouldFilter(text: String): Boolean {
        val sensitiveKeywords = listOf("password", "密码", "验证码", "code", "pin", "cvv", "身份证")
        return sensitiveKeywords.any { text.lowercase().contains(it) }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) { packageName }
    }
}