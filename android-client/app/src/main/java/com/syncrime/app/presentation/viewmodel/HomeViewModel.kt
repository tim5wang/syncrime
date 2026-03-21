package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.app.data.DataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object { private const val TAG = "HomeViewModel" }
    
    data class HomeUiState(
        val isLoading: Boolean = false,
        val todayInputCount: Int = 0,
        val totalInputCount: Int = 0,
        val isServiceRunning: Boolean = false,
        val error: String? = null
    )
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val repository: DataRepository = DataRepository.getInstance(application)
    
    init {
        Log.d(TAG, "HomeViewModel init")
        loadStats()
    }
    
    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getTodayCount().catch { 
                _uiState.value = _uiState.value.copy(error = it.message)
            }.collect { _uiState.value = _uiState.value.copy(todayInputCount = it) }
        }
        viewModelScope.launch {
            repository.getTotalCount().catch { }.collect { 
                _uiState.value = _uiState.value.copy(totalInputCount = it, isLoading = false)
            }
        }
    }
    
    fun refresh() { loadStats() }
}
