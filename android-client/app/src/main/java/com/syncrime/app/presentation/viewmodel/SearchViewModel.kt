package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.app.data.DataRepository
import com.syncrime.shared.model.InputRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object { 
        private const val TAG = "SearchViewModel"
    }
    
    data class SearchUiState(
        val query: String = "",
        val isSearching: Boolean = false,
        val results: List<InputRecord> = emptyList(),
        val recentRecords: List<InputRecord> = emptyList(),
        val searchHistory: List<String> = emptyList(),
        val hasSearched: Boolean = false,
        val showHistory: Boolean = true,
        val error: String? = null
    )
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val repository: DataRepository = DataRepository.getInstance(application)
    private val searchHistory = mutableListOf<String>()
    
    init {
        loadRecentRecords()
    }
    
    private fun loadRecentRecords() {
        viewModelScope.launch {
            repository.getRecentRecords()
                .catch { e -> Log.e(TAG, "加载最近记录失败", e) }
                .collect { records ->
                    Log.d(TAG, "加载最近记录: ${records.size} 条")
                    _uiState.value = _uiState.value.copy(recentRecords = records)
                }
        }
    }
    
    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            showHistory = query.isBlank()
        )
        
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                results = emptyList(),
                hasSearched = false
            )
        }
    }
    
    fun search(query: String) {
        if (query.isBlank()) return
        
        Log.d(TAG, "搜索: $query")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                hasSearched = true,
                showHistory = false,
                error = null
            )
            
            repository.searchRecords(query)
                .catch { e ->
                    Log.e(TAG, "搜索失败", e)
                    _uiState.value = _uiState.value.copy(error = e.message, isSearching = false)
                }
                .collect { results ->
                    Log.d(TAG, "搜索结果: ${results.size} 条")
                    _uiState.value = _uiState.value.copy(
                        results = results,
                        isSearching = false
                    )
                    
                    // 添加到搜索历史
                    if (searchHistory.contains(query)) {
                        searchHistory.remove(query)
                    }
                    searchHistory.add(0, query)
                    if (searchHistory.size > 10) {
                        searchHistory.removeLast()
                    }
                    _uiState.value = _uiState.value.copy(searchHistory = searchHistory.toList())
                }
        }
    }
    
    fun searchFromHistory(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        search(query)
    }
    
    fun clearHistory() {
        searchHistory.clear()
        _uiState.value = _uiState.value.copy(searchHistory = emptyList())
    }
    
    fun clearSearch() { 
        _uiState.value = _uiState.value.copy(
            query = "", 
            results = emptyList(), 
            hasSearched = false,
            showHistory = true
        )
    }
}
