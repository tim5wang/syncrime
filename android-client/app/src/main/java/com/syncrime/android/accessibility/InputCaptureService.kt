package com.syncrime.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.model.InputRecord
import kotlinx.coroutines.*
import java.util.*

class InputCaptureService : AccessibilityService() {
    
    companion object {
        private const val TAG = "InputCaptureService"
        private const val SAVE_DELAY = 2000L // 2秒无输入后保存
        private var instance: InputCaptureService? = null
        fun isRunning(): Boolean = instance != null
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var inputCount: Int = 0
    
    // 输入缓冲
    private var currentText: StringBuilder = StringBuilder()
    private var currentPackage: String = ""
    private var saveJob: Job? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "=== 无障碍服务已连接 ===")
        
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        setServiceInfo(serviceInfo)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == packageName) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // 窗口切换时，保存当前输入并重置
                saveCurrentInput()
                currentPackage = event.packageName?.toString() ?: "unknown"
                Log.d(TAG, "窗口切换: $currentPackage")
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.joinToString("") ?: ""
                if (text.isBlank()) return
                
                // 过滤敏感内容
                if (shouldFilter(text)) return
                
                // 更新当前文本
                currentText.clear()
                currentText.append(text)
                
                // 延迟保存（等待用户输入完成）
                scheduleSave()
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                // 输入框失去焦点时保存
                if (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED != 0) {
                    saveCurrentInput()
                }
            }
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "无障碍服务被中断")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        saveCurrentInput() // 保存最后的输入
        instance = null
        serviceScope.cancel()
        Log.i(TAG, "无障碍服务已销毁")
    }
    
    private fun scheduleSave() {
        // 取消之前的保存任务
        saveJob?.cancel()
        
        // 延迟保存
        saveJob = serviceScope.launch {
            delay(SAVE_DELAY)
            saveCurrentInput()
        }
    }
    
    private fun saveCurrentInput() {
        val text = currentText.toString()
        if (text.isBlank()) return
        
        // 清空缓冲
        currentText.clear()
        
        // 过滤太短的内容
        if (text.length < 2) return
        
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@InputCaptureService)
                val record = InputRecord(
                    id = System.currentTimeMillis(),
                    sessionId = System.currentTimeMillis() / 1000, // 按秒分组会话
                    content = text,
                    application = getAppName(currentPackage),
                    category = null,
                    tags = emptyList(),
                    isSensitive = false
                )
                database.inputDao().insert(record)
                inputCount++
                Log.i(TAG, "✅ 已保存输入 #$inputCount: \"${text.take(30)}...\" (${text.length}字, 来源: ${getAppName(currentPackage)})")
            } catch (e: Exception) {
                Log.e(TAG, "保存输入失败", e)
            }
        }
    }
    
    private fun shouldFilter(text: String): Boolean {
        val lowerText = text.lowercase()
        val sensitiveKeywords = listOf("password", "密码", "验证码", "code", "pin", "cvv", "身份证", "卡号")
        return sensitiveKeywords.any { lowerText.contains(it) }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}