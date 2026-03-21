package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "HomeViewModel"

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    data class HomeUiState(
        val isLoading: Boolean = false,
        val todayInputCount: Int = 0,
        val totalInputCount: Int = 0,
        val isServiceRunning: Boolean = false,
        val currentApp: String? = null,
        val sessionId: Long? = null,
        val appStats: List<AppStatItem> = emptyList(),
        val error: String? = null
    )
    
    data class AppStatItem(
        val appName: String,
        val count: Int,
        val percentage: Float
    )
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "HomeViewModel init")
        loadStats()
    }
    
    fun loadStats() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // 暂时使用模拟数据，避免数据库问题
                _uiState.value = _uiState.value.copy(
                    todayInputCount = 0,
                    totalInputCount = 0,
                    isLoading = false
                )
                
                Log.d(TAG, "Stats loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load stats", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun refresh() {
        loadStats()
    }
}