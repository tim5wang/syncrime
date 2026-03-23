package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.android.network.AuthService
import com.syncrime.android.network.AuthResult
import com.syncrime.android.sync.SyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object { 
        private const val TAG = "SettingsViewModel"
        private const val PREFS_NAME = "syncrime_settings"
    }
    
    data class SettingsUiState(
        val autoSave: Boolean = true,
        val privacyFilter: Boolean = true,
        val cleanupDays: Int = 30,
        val isLoggedIn: Boolean = false,
        val userEmail: String? = null,
        val userNickname: String? = null,
        val isLoggingIn: Boolean = false,
        val loginError: String? = null,
        val message: String? = null,
        val loginSuccess: Boolean = false,
        val isSyncing: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val syncManager = SyncManager.getInstance(application)
    
    init {
        loadSettings()
        checkLogin()
    }
    
    private fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            autoSave = prefs.getBoolean("auto_save", true),
            privacyFilter = prefs.getBoolean("privacy_filter", true),
            cleanupDays = prefs.getInt("cleanup_days", 30)
        )
    }
    
    private fun checkLogin() {
        _uiState.value = _uiState.value.copy(
            isLoggedIn = AuthService.isLoggedIn(),
            userNickname = AuthService.getNickname(),
            userEmail = AuthService.getEmail()
        )
    }
    
    fun setAutoSave(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save", enabled).apply()
        _uiState.value = _uiState.value.copy(autoSave = enabled)
    }
    
    fun setPrivacyFilter(enabled: Boolean) {
        prefs.edit().putBoolean("privacy_filter", enabled).apply()
        _uiState.value = _uiState.value.copy(privacyFilter = enabled)
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null, loginSuccess = false)
            
            when (val result = AuthService.login(email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userNickname = AuthService.getNickname(),
                        userEmail = AuthService.getEmail(),
                        isLoggingIn = false,
                        message = "✅ 登录成功，正在同步数据...",
                        loginSuccess = true,
                        isSyncing = true
                    )
                    
                    // 先拉取云端数据，再推送本地数据
                    viewModelScope.launch {
                        val pullSuccess = syncManager.pullFromCloud()
                        if (pullSuccess) {
                            Log.d(TAG, "云端数据拉取成功")
                        }
                        
                        syncManager.syncNow { success, msg ->
                            _uiState.value = _uiState.value.copy(
                                isSyncing = false,
                                message = if (success) "✅ 登录成功，数据已同步" else "✅ 登录成功"
                            )
                            Log.d(TAG, "同步结果: $msg")
                        }
                    }
                    
                    Log.d(TAG, "登录成功")
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        loginError = result.message
                    )
                    Log.e(TAG, "登录失败: ${result.message}")
                }
            }
        }
    }
    
    fun register(email: String, password: String, nickname: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null, loginSuccess = false)
            
            when (val result = AuthService.register(email, password, nickname)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        userNickname = AuthService.getNickname(),
                        userEmail = AuthService.getEmail(),
                        isLoggingIn = false,
                        message = "✅ 注册成功",
                        loginSuccess = true
                    )
                    
                    // 启动同步
                    syncManager.syncNow()
                    
                    Log.d(TAG, "注册成功")
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggingIn = false,
                        loginError = result.message
                    )
                    Log.e(TAG, "注册失败: ${result.message}")
                }
            }
        }
    }
    
    fun logout() {
        AuthService.logout()
        _uiState.value = _uiState.value.copy(
            isLoggedIn = false,
            userNickname = null,
            userEmail = null,
            message = "已退出登录"
        )
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, loginError = null, loginSuccess = false)
    }
}