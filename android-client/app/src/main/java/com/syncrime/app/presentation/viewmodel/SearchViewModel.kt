package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.app.data.DataRepository
import com.syncrime.shared.model.InputRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object { private const val TAG = "SearchViewModel" }
    
    data class SearchUiState(
        val query: String = "",
        val isSearching: Boolean = false,
        val results: List<InputRecord> = emptyList(),
        val recentRecords: List<InputRecord> = emptyList(),
        val hasSearched: Boolean = false,
        val error: String? = null
    )
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val repository: DataRepository = DataRepository.getInstance(application)
    
    // Track collection jobs to prevent memory leaks
    private var recentRecordsJob: Job? = null
    private var searchJob: Job? = null
    
    init {
        // 加载最近记录作为默认展示
        loadRecentRecords()
    }
    
    private fun loadRecentRecords() {
        // Cancel previous job to prevent memory leaks
        recentRecordsJob?.cancel()
        
        recentRecordsJob = viewModelScope.launch {
            repository.getRecentRecords()
                .catch { e -> Log.e(TAG, "加载最近记录失败", e) }
                .collect { records ->
                    if (isActive) {
                        Log.d(TAG, "加载最近记录: ${records.size} 条")
                        _uiState.value = _uiState.value.copy(recentRecords = records)
                    }
                }
        }
    }
    
    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isNotBlank()) {
            search(query)
        } else {
            // 清空搜索，恢复显示最近记录
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false)
        }
    }
    
    fun search(query: String) {
        if (query.isBlank()) return
        
        Log.d(TAG, "搜索: $query")
        
        // Cancel previous search job to prevent memory leaks
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, hasSearched = true, error = null) }
            
            repository.searchRecords(query)
                .catch { e ->
                    if (isActive) {
                        Log.e(TAG, "搜索失败", e)
                        _uiState.update { it.copy(error = e.message, isSearching = false) }
                    }
                }
                .collect { results ->
                    if (isActive) {
                        Log.d(TAG, "搜索结果: ${results.size} 条")
                        _uiState.update { it.copy(results = results, isSearching = false) }
                    }
                }
        }
    }
    
    fun clearSearch() { 
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            query = "", 
            results = emptyList(), 
            hasSearched = false
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        recentRecordsJob?.cancel()
        searchJob?.cancel()
        Log.d(TAG, "SearchViewModel cleared, jobs cancelled")
    }
}
