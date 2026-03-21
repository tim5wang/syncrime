package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.shared.model.SourceType
import com.syncrime.android.sync.SyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object { private const val TAG = "LibraryViewModel" }
    
    data class LibraryUiState(
        val recentClipboard: List<ClipboardItem> = emptyList(),
        val clips: List<KnowledgeClip> = emptyList(),
        val editingClip: KnowledgeClip? = null,
        val message: String? = null
    )
    
    data class ClipboardItem(val text: String, val time: Long = System.currentTimeMillis())
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    private val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val database = AppDatabase.getDatabase(application)
    private val syncManager = SyncManager.getInstance(application)
    
    init {
        loadClips()
        loadClipboardHistory()
    }
    
    private fun loadClips() {
        viewModelScope.launch {
            database.clipDao().getAll().catch { }.collect { clips ->
                _uiState.value = _uiState.value.copy(clips = clips)
            }
        }
    }
    
    fun loadClipboardHistory() {
        val clipText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        if (!clipText.isNullOrBlank() && clipText.length >= 2) {
            val currentList = _uiState.value.recentClipboard.toMutableList()
            if (currentList.none { it.text == clipText }) {
                currentList.add(0, ClipboardItem(clipText))
                if (currentList.size > 10) currentList.removeLast()
                _uiState.value = _uiState.value.copy(recentClipboard = currentList)
            }
        }
    }
    
    fun addToClip(text: String, title: String? = null) {
        viewModelScope.launch {
            try {
                val clip = KnowledgeClip(
                    id = System.currentTimeMillis(),
                    title = title ?: text.take(50),
                    content = text,
                    sourceType = SourceType.CLIP,
                    createdAt = System.currentTimeMillis()
                )
                database.clipDao().insert(clip)
                
                // 同步到云端
                syncManager.syncClip(clip.id, "create")
                
                _uiState.value = _uiState.value.copy(message = "已添加到剪藏")
                Log.d(TAG, "已添加剪藏: ${clip.title}")
            } catch (e: Exception) {
                Log.e(TAG, "添加失败", e)
                _uiState.value = _uiState.value.copy(message = "添加失败")
            }
        }
    }
    
    fun startEdit(clip: KnowledgeClip) {
        _uiState.value = _uiState.value.copy(editingClip = clip)
    }
    
    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(editingClip = null)
    }
    
    fun updateClip(clipId: Long, newTitle: String, newContent: String) {
        viewModelScope.launch {
            try {
                val clip = _uiState.value.clips.find { it.id == clipId }
                if (clip != null) {
                    val updated = clip.copy(
                        title = newTitle,
                        content = newContent
                    )
                    database.clipDao().update(updated)
                    
                    // 同步到云端
                    syncManager.syncClip(clipId, "update")
                    
                    _uiState.value = _uiState.value.copy(
                        editingClip = null,
                        message = "已更新"
                    )
                    Log.d(TAG, "已更新剪藏: $newTitle")
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新失败", e)
                _uiState.value = _uiState.value.copy(message = "更新失败")
            }
        }
    }
    
    fun deleteClip(clipId: Long) {
        viewModelScope.launch {
            try {
                val clip = _uiState.value.clips.find { it.id == clipId }
                if (clip != null) {
                    database.clipDao().delete(clip)
                    
                    // 同步到云端
                    syncManager.syncClip(clipId, "delete")
                    
                    _uiState.value = _uiState.value.copy(message = "已删除")
                    Log.d(TAG, "已删除剪藏: ${clip.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除失败", e)
                _uiState.value = _uiState.value.copy(message = "删除失败")
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}