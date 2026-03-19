package com.syncrime.app.presentation.viewmodel

import android.app.Application
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
    private val clipRepository: ClipRepository
) : ViewModel() {
    
    data class SearchUiState(
        val query: String = "",
        val isSearching: Boolean = false,
        val inputResults: List<InputRecord> = emptyList(),
        val clipResults: List<KnowledgeClip> = emptyList(),
        val hasSearched: Boolean = false
    )
    
    // 状态流
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    /**
     * 设置搜索关键词
     */
    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }
    
    /**
     * 执行搜索
     */
    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                hasSearched = false,
                inputResults = emptyList(),
                clipResults = emptyList()
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                query = query,
                isSearching = true
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
        _uiState.value = SearchUiState()
    }
}

/**
 * ViewModel Factory
 */
class SearchViewModelFactory(
    private val inputRepository: InputRepository,
    private val clipRepository: ClipRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(inputRepository, clipRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
