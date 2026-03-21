package com.syncrime.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.model.InputRecord
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class InputCaptureService : AccessibilityService() {
    
    companion object {
        private const val TAG = "InputCaptureService"
        private var instance: InputCaptureService? = null
        fun isRunning(): Boolean = instance != null
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var inputCount: Int = 0
    private var lastPackageName: String? = null
    private var currentSessionId: Long = System.currentTimeMillis()
    
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
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        setServiceInfo(serviceInfo)
        
        // 显示通知
        showNotification()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == packageName) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                lastPackageName = event.packageName?.toString()
                Log.d(TAG, "窗口切换: ${event.packageName}")
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text?.joinToString("") ?: return
                if (text.isBlank()) return
                
                // 隐私过滤
                if (shouldFilter(text)) {
                    Log.d(TAG, "已过滤敏感内容")
                    return
                }
                
                saveInput(text, event.packageName?.toString() ?: "unknown")
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                // 可用于检测输入框获得焦点
            }
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "无障碍服务被中断")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Log.i(TAG, "无障碍服务已销毁")
    }
    
    private fun shouldFilter(text: String): Boolean {
        val lowerText = text.lowercase()
        // 过滤密码、验证码等敏感内容
        val sensitiveKeywords = listOf("password", "密码", "验证码", "code", "pin", "cvv", "身份证")
        return sensitiveKeywords.any { lowerText.contains(it) }
    }
    
    private fun saveInput(text: String, packageName: String) {
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@InputCaptureService)
                val record = InputRecord(
                    id = System.currentTimeMillis(),
                    sessionId = currentSessionId,
                    content = text,
                    application = getAppName(packageName),
                    category = null,
                    tags = emptyList(),
                    isSensitive = false
                )
                database.inputDao().insert(record)
                inputCount++
                Log.d(TAG, "已保存输入 #$inputCount: ${text.take(20)}... (from $packageName)")
                
                // 更新通知
                updateNotification(inputCount)
            } catch (e: Exception) {
                Log.e(TAG, "保存输入失败", e)
            }
        }
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
    
    private fun showNotification() {
        // TODO: 显示前台服务通知
    }
    
    private fun updateNotification(count: Int) {
        // TODO: 更新通知计数
    }
}