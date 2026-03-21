package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "SettingsViewModel"
        private const val PREFS_NAME = "syncrime_settings"
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_PRIVACY_FILTER = "privacy_filter"
        private const val KEY_CLEANUP_DAYS = "cleanup_days"
    }
    
    data class SettingsUiState(
        val autoSave: Boolean = true,
        val privacyFilter: Boolean = true,
        val cleanupDays: Int = 30,
        val isLoggedIn: Boolean = false,
        val userEmail: String? = null,
        val userNickname: String? = null,
        val message: String? = null
    )
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        Log.d(TAG, "SettingsViewModel init")
        loadSettings()
    }
    
    private fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            autoSave = prefs.getBoolean(KEY_AUTO_SAVE, true),
            privacyFilter = prefs.getBoolean(KEY_PRIVACY_FILTER, true),
            cleanupDays = prefs.getInt(KEY_CLEANUP_DAYS, 30)
        )
    }
    
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
}