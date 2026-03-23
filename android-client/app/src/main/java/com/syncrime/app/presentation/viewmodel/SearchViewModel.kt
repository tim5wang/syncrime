package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.android.data.local.dao.SearchHistoryDao
import com.syncrime.android.data.local.entity.SearchHistoryEntity
import com.syncrime.app.data.DataRepository
import com.syncrime.shared.model.InputRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object { 
        private const val TAG = "SearchViewModel"
        private const val MAX_HISTORY_COUNT = 50
        private const val SUGGESTION_LIMIT = 10
    }
    
    data class SearchUiState(
        val query: String = "",
        val isSearching: Boolean = false,
        val results: List<InputRecord> = emptyList(),
        val recentRecords: List<InputRecord> = emptyList(),
        val searchHistory: List<String> = emptyList(),
        val suggestions: List<String> = emptyList(),
        val hasSearched: Boolean = false,
        val showHistory: Boolean = true,
        val showSuggestions: Boolean = false,
        val selectedRecord: InputRecord? = null,
        val message: String? = null,
        val error: String? = null
    )
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val repository: DataRepository = DataRepository.getInstance(application)
    private val searchHistoryDao: SearchHistoryDao = repository.getSearchHistoryDao()
    private val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    // 防抖相关
    private var searchJob: Job? = null
    
    init {
        loadRecentRecords()
        observeSearchHistory()
    }
    
    private fun observeSearchHistory() {
        viewModelScope.launch {
            searchHistoryDao.getAll(MAX_HISTORY_COUNT)
                .catch { e -> Log.e(TAG, "加载搜索历史失败", e) }
                .collect { history ->
                    _uiState.value = _uiState.value.copy(
                        searchHistory = history.map { it.query }.distinct()
                    )
                }
        }
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
    
    private fun getSearchSuggestions(query: String) {
        if (query.length >= 1) { // 至少输入1个字符就显示建议
            viewModelScope.launch {
                searchHistoryDao.getSuggestions(query, SUGGESTION_LIMIT)
                    .catch { e -> 
                        Log.e(TAG, "获取搜索建议失败", e)
                        _uiState.value = _uiState.value.copy(suggestions = emptyList())
                    }
                    .collect { suggestions ->
                        // 过滤掉当前正在输入的查询词本身
                        val filteredSuggestions = suggestions.filter { it != query }
                        Log.d(TAG, "获取搜索建议: ${filteredSuggestions.size} 条")
                        _uiState.value = _uiState.value.copy(suggestions = filteredSuggestions)
                    }
            }
        } else {
            _uiState.value = _uiState.value.copy(suggestions = emptyList())
        }
    }
    
    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            showHistory = query.isBlank(),
            showSuggestions = query.length >= 1 && query.isNotBlank()
        )
        
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                results = emptyList(),
                hasSearched = false,
                showSuggestions = false
            )
        } else if (query.length >= 1) {
            // 获取搜索建议
            getSearchSuggestions(query)
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
                showSuggestions = false,
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
                    addToSearchHistory(query, results.size)
                }
        }
    }
    
    private suspend fun addToSearchHistory(query: String, resultCount: Int) {
        // 检查是否已存在相同的查询，如果存在则先删除
        val existing = searchHistoryDao.getAll(1).firstOrNull { it.query == query }
        if (existing != null) {
            searchHistoryDao.delete(existing)
        }
        
        // 插入新记录
        val entity = SearchHistoryEntity(
            query = query,
            resultCount = resultCount,
            searchType = "input"
        )
        searchHistoryDao.insert(entity)
        
        // 保持历史记录数量在限制范围内
        searchHistoryDao.deleteOldRecords(MAX_HISTORY_COUNT)
    }
    
    /**
     * 带防抖的搜索函数，避免频繁搜索
     */
    fun searchWithDebounce(query: String) {
        if (query.isBlank()) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    results = emptyList(),
                    hasSearched = false,
                    showSuggestions = false
                )
            }
            return
        }
        
        // 取消之前的搜索任务
        searchJob?.cancel()
        
        // 启动新的带延迟的搜索任务
        searchJob = viewModelScope.launch {
            delay(300) // 300ms 防抖延迟
            search(query)
        }
    }
    
    fun searchFromHistory(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        search(query)
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
            _uiState.value = _uiState.value.copy(searchHistory = emptyList())
        }
    }
    
    fun clearSearch() { 
        _uiState.value = _uiState.value.copy(
            query = "", 
            results = emptyList(), 
            hasSearched = false,
            showHistory = true,
            showSuggestions = false,
            suggestions = emptyList()
        )
    }
    
    fun selectRecord(record: InputRecord) {
        _uiState.value = _uiState.value.copy(selectedRecord = record)
    }
    
    fun clearSelectedRecord() {
        _uiState.value = _uiState.value.copy(selectedRecord = null)
    }
    
    fun copyRecord(record: InputRecord) {
        val clip = ClipData.newPlainText("输入记录", record.content)
        clipboardManager.setPrimaryClip(clip)
        _uiState.value = _uiState.value.copy(message = "已复制到剪贴板")
        Log.d(TAG, "复制记录: ${record.content.take(30)}...")
    }
    
    fun deleteRecord(record: InputRecord) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(record.id)
                _uiState.value = _uiState.value.copy(
                    selectedRecord = null,
                    message = "已删除"
                )
                Log.d(TAG, "删除记录: ${record.id}")
            } catch (e: Exception) {
                Log.e(TAG, "删除失败", e)
                _uiState.value = _uiState.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}