package com.syncrime.app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.inputmethod.repository.ClipRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 知识库 ViewModel
 */
class LibraryViewModel(
    private val clipRepository: ClipRepository
) : ViewModel() {
    
    data class LibraryUiState(
        val isLoading: Boolean = true,
        val clips: List<KnowledgeClip> = emptyList(),
        val selectedCategory: String? = null,
        val categories: List<String> = emptyList(),
        val totalCount: Int = 0,
        val todayCount: Int = 0
    )
    
    // 状态流
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadClips()
        loadStats()
    }
    
    /**
     * 加载剪藏列表
     */
    fun loadClips(category: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val flow = if (category != null) {
                clipRepository.getClipsByCategory(category)
            } else {
                clipRepository.getAllClips()
            }
            
            flow.collect { clips ->
                _uiState.value = _uiState.value.copy(
                    clips = clips,
                    isLoading = false,
                    selectedCategory = category
                )
            }
        }
    }
    
    /**
     * 加载统计
     */
    private fun loadStats() {
        viewModelScope.launch {
            val stats = clipRepository.getStats()
            _uiState.value = _uiState.value.copy(
                totalCount = stats.total,
                todayCount = stats.today
            )
        }
    }
    
    /**
     * 按分类筛选
     */
    fun filterByCategory(category: String?) {
        loadClips(category)
    }
    
    /**
     * 刷新
     */
    fun refresh() {
        loadClips(_uiState.value.selectedCategory)
        loadStats()
    }
    
    /**
     * 删除剪藏
     */
    fun deleteClip(clip: KnowledgeClip) {
        viewModelScope.launch {
            clipRepository.deleteClip(clip)
            loadClips(_uiState.value.selectedCategory)
        }
    }
    
    /**
     * 切换收藏
     */
    fun toggleFavorite(clip: KnowledgeClip) {
        viewModelScope.launch {
            clipRepository.incrementFavoriteCount(clip.id)
        }
    }
}

/**
 * ViewModel Factory
 */
class LibraryViewModelFactory(
    private val clipRepository: ClipRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(clipRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
