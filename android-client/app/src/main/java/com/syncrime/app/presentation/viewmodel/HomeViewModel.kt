package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// import com.syncrime.inputmethod.core.CaptureStats
// import com.syncrime.inputmethod.core.InputCaptureService
import com.syncrime.inputmethod.repository.InputRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * HomeViewModel 日志标签
 */
private const val TAG = "HomeViewModel"

/**
 * 首页 ViewModel
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val inputRepository = InputRepository(
        com.syncrime.shared.data.local.AppDatabase.getDatabase(application).inputDao()
    )
    
    // UI 状态
    data class HomeUiState(
        val isLoading: Boolean = true,
        val todayInputCount: Int = 0,
        val totalInputCount: Int = 0,
        val isServiceRunning: Boolean = false,
        val currentApp: String? = null,
        val sessionId: Long? = null,
        val appStats: List<AppStatItem> = emptyList()
    )
    
    data class AppStatItem(
        val appName: String,
        val count: Int,
        val percentage: Float
    )
    
    // 状态流
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadStats()
        observeServiceStatus()
    }
    
    /**
     * 加载统计数据
     */
    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // 获取今日统计
            inputRepository.getTodayStats().collect { todayCount ->
                _uiState.value = _uiState.value.copy(
                    todayInputCount = todayCount,
                    isLoading = false
                )
            }
            
            // 获取总统计
            inputRepository.getTotalCount().collect { totalCount ->
                _uiState.value = _uiState.value.copy(
                    totalInputCount = totalCount
                )
            }
        }
    }
    
    /**
     * 观察服务状态 (暂时简化)
     */
    private fun observeServiceStatus() {
        viewModelScope.launch {
            // 暂时返回静态状态，后续完善
            _uiState.value = _uiState.value.copy(
                isServiceRunning = false,
                currentApp = null,
                sessionId = null
            )
        }
    }
    
    /**
     * 刷新统计
     */
    fun refresh() {
        loadStats()
    }
}
