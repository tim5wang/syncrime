package com.syncrime.android.presentation.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val isLoading: Boolean = false,
    val captureEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 15,
    val notificationsEnabled: Boolean = true,
    val syncOnWifiOnly: Boolean = false,
    val dataEncryptionEnabled: Boolean = true,
    val privacyModeEnabled: Boolean = false,
    val language: String = "zh-CN",
    val theme: String = "system",
    val smartRecommendationEnabled: Boolean = true,
    val personalizedLearningEnabled: Boolean = true,
    val contextAwareEnabled: Boolean = true,
    val error: String? = null
)

class SettingsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    fun setCaptureEnabled(enabled: Boolean) {
        _uiState.update { it.copy(captureEnabled = enabled) }
    }
    
    fun setAutoSyncEnabled(enabled: Boolean) {
        _uiState.update { it.copy(autoSyncEnabled = enabled) }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }
    
    fun setPrivacyModeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(privacyModeEnabled = enabled) }
    }
    
    fun setDataEncryptionEnabled(enabled: Boolean) {
        _uiState.update { it.copy(dataEncryptionEnabled = enabled) }
    }
}
