package com.syncrime.android.debug

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.android.network.ApiClient
import com.syncrime.android.network.AuthService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DebugViewModel : ViewModel() {
    
    companion object { private const val TAG = "DebugViewModel" }
    
    data class DebugState(
        val logs: List<LogEntry> = emptyList(),
        val apiStatus: String = "未测试",
        val isLoggedIn: Boolean = false,
        val userInfo: String = "",
        val accessibilityStatus: String = "未知"
    )
    
    data class LogEntry(
        val time: String,
        val tag: String,
        val message: String,
        val level: String = "INFO"
    )
    
    private val _state = MutableStateFlow(DebugState())
    val state: StateFlow<DebugState> = _state.asStateFlow()
    
    fun addLog(tag: String, message: String, level: String = "INFO") {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = LogEntry(time, tag, message, level)
        _state.value = _state.value.copy(logs = _state.value.logs + newLog)
        Log.d(TAG, "[$tag] $message")
    }
    
    fun checkApiStatus() {
        viewModelScope.launch {
            addLog("API", "测试 API 连接...")
            try {
                val response = ApiClient.get("/../health")
                if (response.isSuccess) {
                    _state.value = _state.value.copy(apiStatus = "✅ 正常")
                    addLog("API", "连接成功: ${response.body}")
                } else {
                    _state.value = _state.value.copy(apiStatus = "❌ 错误: ${response.code}")
                    addLog("API", "连接失败: ${response.code}", "ERROR")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(apiStatus = "❌ 异常: ${e.message}")
                addLog("API", "异常: ${e.message}", "ERROR")
            }
        }
    }
    
    fun checkLoginStatus() {
        val isLoggedIn = AuthService.isLoggedIn()
        val nickname = AuthService.getNickname()
        val email = AuthService.getEmail()
        _state.value = _state.value.copy(
            isLoggedIn = isLoggedIn,
            userInfo = if (isLoggedIn) "$nickname ($email)" else "未登录"
        )
        addLog("Auth", if (isLoggedIn) "已登录: $nickname" else "未登录")
    }
    
    fun checkAccessibilityStatus(context: Context) {
        val enabled = isAccessibilityServiceEnabled(context)
        _state.value = _state.value.copy(accessibilityStatus = if (enabled) "✅ 已开启" else "❌ 未开启")
        addLog("A11y", if (enabled) "无障碍服务已开启" else "无障碍服务未开启")
    }
    
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = "${context.packageName}/com.syncrime.android.accessibility.InputCaptureService"
        try {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(expectedComponentName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check accessibility status", e)
            return false
        }
    }
    
    fun clearLogs() {
        _state.value = _state.value.copy(logs = emptyList())
    }
}