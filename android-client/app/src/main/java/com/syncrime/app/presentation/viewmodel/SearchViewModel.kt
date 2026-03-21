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
    
    init {
        // 加载最近记录作为默认展示
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, hasSearched = true, error = null)
            
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
                }
        }
    }
    
    fun clearSearch() { 
        _uiState.value = _uiState.value.copy(
            query = "", 
            results = emptyList(), 
            hasSearched = false
        )
    }
}