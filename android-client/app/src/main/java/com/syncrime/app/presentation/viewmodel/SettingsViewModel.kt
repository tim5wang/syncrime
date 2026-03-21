package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.android.network.ApiClient
import com.syncrime.android.network.AuthService
import com.syncrime.android.network.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "SettingsViewModel"
        private const val PREFS_NAME = "syncrime_settings"
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_PRIVACY_FILTER = "privacy_filter"
        private const val KEY_CLEANUP_DAYS = "cleanup_days"
        private const val DEFAULT_CLEANUP_DAYS = 30
    }
    
    data class SettingsUiState(
        val autoSave: Boolean = true,
        val privacyFilter: Boolean = true,
        val cleanupDays: Int = DEFAULT_CLEANUP_DAYS,
        val isCleaning: Boolean = false,
        val isExporting: Boolean = false,
        val lastCleanupTime: String? = null,
        val lastExportPath: String? = null,
        val message: String? = null,
        // 用户认证状态
        val isLoggedIn: Boolean = false,
        val userEmail: String? = null,
        val userNickname: String? = null,
        val isLoggingIn: Boolean = false,
        val loginError: String? = null
    )
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        loadSettings()
        checkLoginStatus()
    }
    
    private fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            autoSave = prefs.getBoolean(KEY_AUTO_SAVE, true),
            privacyFilter = prefs.getBoolean(KEY_PRIVACY_FILTER, true),
            cleanupDays = prefs.getInt(KEY_CLEANUP_DAYS, DEFAULT_CLEANUP_DAYS)
        )
    }
    
    private fun checkLoginStatus() {
        val isLoggedIn = AuthService.isLoggedIn()
        val nickname = AuthService.getNickname()
        _uiState.value = _uiState.value.copy(
            isLoggedIn = isLoggedIn,
            userNickname = nickname
        )
    }
    
    // ============ 用户认证 ============
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            
            when (val result = AuthService.login(email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userEmail = email,
                        userNickname = AuthService.getNickname(),
                        isLoggingIn = false,
                        message = "登录成功"
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        loginError = result.message
                    )
                }
            }
        }
    }
    
    fun register(email: String, password: String, nickname: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            
            when (val result = AuthService.register(email, password, nickname)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userEmail = email,
                        userNickname = nickname,
                        isLoggingIn = false,
                        message = "注册成功"
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        loginError = result.message
                    )
                }
            }
        }
    }
    
    fun logout() {
        AuthService.logout()
        _uiState.value = _uiState.value.copy(
            isLoggedIn = false,
            userEmail = null,
            userNickname = null,
            message = "已退出登录"
        )
    }
    
    // ============ 设置功能 ============
    
    fun setAutoSave(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SAVE, enabled).apply()
        _uiState.value = _uiState.value.copy(autoSave = enabled)
    }
    
    fun setPrivacyFilter(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_FILTER, enabled).apply()
        _uiState.value = _uiState.value.copy(privacyFilter = enabled)
    }
    
    fun setCleanupDays(days: Int) {
        prefs.edit().putInt(KEY_CLEANUP_DAYS, days).apply()
        _uiState.value = _uiState.value.copy(cleanupDays = days)
    }
    
    fun cleanupOldData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true, message = null)
            try {
                withContext(Dispatchers.IO) { clearAppCache(getApplication()) }
                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                _uiState.value = _uiState.value.copy(
                    isCleaning = false,
                    lastCleanupTime = timeStr,
                    message = "已清理缓存数据"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isCleaning = false, message = "清理失败: ${e.message}")
            }
        }
    }
    
    fun exportData(): Uri? {
        viewModelScope.launch { _uiState.value = _uiState.value.copy(isExporting = true, message = null) }
        return try {
            val context = getApplication<Application>()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "syncrime_export_$timestamp.json"
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val exportFile = File(exportsDir, fileName)
            
            viewModelScope.launch {
                try {
                    val json = buildString {
                        append("{\n")
                        append("  \"exportTime\": \"$timestamp\",\n")
                        append("  \"settings\": {\n")
                        append("    \"autoSave\": ${_uiState.value.autoSave},\n")
                        append("    \"privacyFilter\": ${_uiState.value.privacyFilter},\n")
                        append("    \"cleanupDays\": ${_uiState.value.cleanupDays}\n")
                        append("  }\n")
                        append("}")
                    }
                    FileWriter(exportFile).use { it.write(json) }
                    _uiState.value = _uiState.value.copy(isExporting = false, lastExportPath = exportFile.absolutePath, message = "导出成功: $fileName")
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isExporting = false, message = "导出失败: ${e.message}")
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
        } catch (e: Exception) {
            viewModelScope.launch { _uiState.value = _uiState.value.copy(isExporting = false, message = "导出失败: ${e.message}") }
            null
        }
    }
    
    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null, loginError = null) }
    
    private fun clearAppCache(context: Context) {
        context.cacheDir?.takeIf { it.exists() }?.let { deleteDir(it) }
        context.externalCacheDir?.takeIf { it.exists() }?.let { deleteDir(it) }
        context.codeCacheDir?.takeIf { it.exists() }?.let { deleteDir(it) }
    }
    
    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) dir.listFiles()?.forEach { deleteDir(it) }
        return dir.delete()
    }
}
