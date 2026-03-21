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
        val hasSearched: Boolean = false,
        val error: String? = null
    )
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val repository: DataRepository = DataRepository.getInstance(application)
    
    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isNotBlank()) {
            search(query)
        } else {
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false)
        }
    }
    
    fun search(query: String) {
        if (query.isBlank()) return
        
        Log.d(TAG, "搜索: $query")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, hasSearched = true, error = null)
            
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "搜索异常", e)
                _uiState.value = _uiState.value.copy(error = e.message, isSearching = false)
            }
        }
    }
    
    fun clearSearch() { 
        _uiState.value = SearchUiState() 
    }
}
