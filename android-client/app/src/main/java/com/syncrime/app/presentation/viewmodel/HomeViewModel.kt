package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    data class HomeUiState(
        val isLoading: Boolean = false,
        val todayInputCount: Int = 0,
        val totalInputCount: Int = 0,
        val isServiceRunning: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "HomeViewModel init")
        loadStats()
    }
    
    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = false, todayInputCount = 0, totalInputCount = 0)
        }
    }
    
    fun refresh() { loadStats() }
}