package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.app.data.DataRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object { private const val TAG = "HomeViewModel" }
    
    data class HomeUiState(
        val isLoading: Boolean = false,
        val todayInputCount: Int = 0,
        val totalInputCount: Int = 0,
        val isAccessibilityEnabled: Boolean = false,
        val error: String? = null
    )
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val repository: DataRepository = DataRepository.getInstance(application)
    
    // Track collection jobs to prevent memory leaks
    private var todayCountJob: Job? = null
    private var totalCountJob: Job? = null
    
    init {
        Log.d(TAG, "HomeViewModel init")
        loadStats()
        checkAccessibilityStatus()
    }
    
    fun loadStats() {
        // Cancel previous jobs to prevent memory leaks from multiple collectors
        todayCountJob?.cancel()
        totalCountJob?.cancel()
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        todayCountJob = viewModelScope.launch {
            repository.getTodayCount()
                .catch { e -> 
                    if (isActive) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
                .collect { count -> 
                    if (isActive) {
                        _uiState.update { it.copy(todayInputCount = count) }
                    }
                }
        }
        
        totalCountJob = viewModelScope.launch {
            repository.getTotalCount()
                .catch { /* silently handle */ }
                .collect { count -> 
                    if (isActive) {
                        _uiState.update { it.copy(totalInputCount = count, isLoading = false) }
                    }
                }
        }
    }
    
    fun checkAccessibilityStatus() {
        val enabled = isAccessibilityServiceEnabled()
        _uiState.update { it.copy(isAccessibilityEnabled = enabled) }
        Log.d(TAG, "Accessibility enabled: $enabled")
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "${getApplication<Application>().packageName}/com.syncrime.android.accessibility.InputCaptureService"
        return try {
            val enabledServices = Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(expectedComponentName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check accessibility", e)
            false
        }
    }
    
    fun refresh() { 
        loadStats()
        checkAccessibilityStatus()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up jobs when ViewModel is destroyed
        todayCountJob?.cancel()
        totalCountJob?.cancel()
        Log.d(TAG, "HomeViewModel cleared, jobs cancelled")
    }
}
