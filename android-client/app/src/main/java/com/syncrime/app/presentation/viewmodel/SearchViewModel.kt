package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.syncrime.shared.model.InputRecord
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.inputmethod.repository.ClipRepository
import com.syncrime.inputmethod.repository.InputRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 搜索 ViewModel
 */
class SearchViewModel(
    private val inputRepository: InputRepository,
    private val clipRepository: ClipRepository,
    private val application: Application
) : ViewModel() {
    
    companion object {
        private const val PREFS_NAME = "search_history"
        private const val KEY_HISTORY = "history"
        private const val MAX_HISTORY_SIZE = 20
    }
    
    data class SearchUiState(
        val query: String = "",
        val isSearching: Boolean = false,
        val inputResults: List<InputRecord> = emptyList(),
        val clipResults: List<KnowledgeClip> = emptyList(),
        val hasSearched: Boolean = false,
        val searchHistory: List<String> = emptyList(),
        val showHistory: Boolean = true
    )
    
    // 状态流
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    init {
        loadSearchHistory()
    }
    
    /**
     * 加载搜索历史
     */
    private fun loadSearchHistory() {
        val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = prefs.getStringSet(KEY_HISTORY, emptySet())?.toList() ?: emptyList()
        _uiState.value = _uiState.value.copy(
            searchHistory = history.sortedByDescending { it }.take(MAX_HISTORY_SIZE)
        )
    }
    
    /**
     * 保存搜索历史
     */
    private fun saveSearchHistory(query: String) {
        val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingHistory = prefs.getStringSet(KEY_HISTORY, emptySet())?.toMutableSet() ?: mutableSetOf()
        
        // 添加新查询（如果已存在则移除旧的）
        existingHistory.remove(query)
        existingHistory.add(query)
        
        // 保持最大数量
        val finalHistory = if (existingHistory.size > MAX_HISTORY_SIZE) {
            existingHistory.sortedByDescending { it }.take(MAX_HISTORY_SIZE).toSet()
        } else {
            existingHistory
        }
        
        prefs.edit().putStringSet(KEY_HISTORY, finalHistory).apply()
        
        _uiState.value = _uiState.value.copy(
            searchHistory = finalHistory.toList().sortedByDescending { it }
        )
    }
    
    /**
     * 清除搜索历史
     */
    fun clearSearchHistory() {
        val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
        _uiState.value = _uiState.value.copy(searchHistory = emptyList())
    }
    
    /**
     * 从历史中选择查询
     */
    fun selectFromHistory(query: String) {
        search(query)
    }
    
    /**
     * 设置搜索关键词
     */
    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            showHistory = query.isBlank()
        )
    }
    
    /**
     * 执行搜索
     */
    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                hasSearched = false,
                inputResults = emptyList(),
                clipResults = emptyList(),
                showHistory = true
            )
            return
        }
        
        // 保存到搜索历史
        saveSearchHistory(query)
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                query = query,
                isSearching = true,
                showHistory = false
            )
            
            // 搜索输入记录
            inputRepository.searchRecords(query).collect { inputResults ->
                _uiState.value = _uiState.value.copy(
                    inputResults = inputResults,
                    isSearching = false,
                    hasSearched = true
                )
            }
            
            // 搜索知识剪藏
            clipRepository.searchClips(query).collect { clipResults ->
                _uiState.value = _uiState.value.copy(
                    clipResults = clipResults
                )
            }
        }
    }
    
    /**
     * 清除搜索
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            query = "",
            hasSearched = false,
            inputResults = emptyList(),
            clipResults = emptyList(),
            showHistory = true
        )
    }
}

/**
 * ViewModel Factory
 */
class SearchViewModelFactory(
    private val inputRepository: InputRepository,
    private val clipRepository: ClipRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(inputRepository, clipRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
