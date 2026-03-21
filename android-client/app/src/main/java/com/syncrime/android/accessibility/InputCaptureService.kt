package com.syncrime.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.model.InputRecord
import kotlinx.coroutines.*

class InputCaptureService : AccessibilityService() {
    
    companion object {
        private const val TAG = "InputCaptureService"
        private const val SAVE_DELAY = 3000L // 3秒无输入后保存
        private const val MIN_LENGTH = 2 // 最小保存长度
        private var instance: InputCaptureService? = null
        fun isRunning(): Boolean = instance != null
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var inputCount: Int = 0
    private var currentText: String = ""
    private var currentPackage: String = ""
    private var lastSavedText: String = ""
    private var saveJob: Job? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "=== 无障碍服务已连接 ===")
        
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        setServiceInfo(serviceInfo)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == packageName) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                saveCurrentInput()
                currentPackage = event.packageName?.toString() ?: "unknown"
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.joinToString("") ?: ""
                if (text.isBlank() || shouldFilter(text)) return
                currentText = text
                scheduleSave()
            }
        }
    }
    
    override fun onInterrupt() {}
    
    override fun onDestroy() {
        saveCurrentInput()
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
    
    private fun saveCurrentInput() {
        val text = currentText
        currentText = ""
        
        if (text.isBlank() || text.length < MIN_LENGTH) return
        
        // 前缀去重：新输入包含上次保存的内容，更新而不是新增
        val isUpdate = lastSavedText.isNotEmpty() && text.startsWith(lastSavedText) && text.length > lastSavedText.length
        
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@InputCaptureService)
                
                if (isUpdate) {
                    // 删除旧记录
                    cleanupShortRecords(database, text)
                }
                
                val record = InputRecord(
                    id = System.currentTimeMillis(),
                    sessionId = System.currentTimeMillis() / 10000,
                    content = text,
                    application = getAppName(currentPackage),
                    category = null,
                    tags = emptyList(),
                    isSensitive = false
                )
                database.inputDao().insert(record)
                lastSavedText = text
                inputCount++
                Log.i(TAG, "✅ 保存输入 #$inputCount (${if (isUpdate) "更新" else "新增"}): \"${text.take(30)}...\"")
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