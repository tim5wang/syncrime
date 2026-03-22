package com.syncrime.android.presentation.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.android.data.local.database.SyncRimeDatabase
import com.syncrime.android.data.local.entity.SyncRecordEntity
import com.syncrime.android.data.repository.InputRepository
import com.syncrime.android.data.repository.SyncRepository
import com.syncrime.android.domain.usecase.EndInputSessionUseCase
import com.syncrime.android.domain.usecase.GetStatisticsUseCase
import com.syncrime.android.domain.usecase.RecordInputUseCase
import com.syncrime.android.domain.usecase.StartInputSessionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 主界面 ViewModel
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object { private const val TAG = "MainViewModel" }
    
    private val database = SyncRimeDatabase.getDatabase(application)
    private val inputRepository = InputRepository(
        database.inputSessionDao(),
        database.inputRecordDao()
    )
    private val syncRepository = SyncRepository(database.syncRecordDao())
    
    private val startSessionUseCase = StartInputSessionUseCase(inputRepository)
    private val endSessionUseCase = EndInputSessionUseCase(inputRepository)
    private val recordInputUseCase = RecordInputUseCase(inputRepository)
    private val getStatisticsUseCase = GetStatisticsUseCase(inputRepository, syncRepository)
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    // Track collection jobs to prevent memory leaks
    private var statisticsJob: Job? = null
    private var syncStatusJob: Job? = null
    
    init {
        loadStatistics()
        observeSyncStatus()
    }
    
    private fun loadStatistics() {
        // Cancel previous job to prevent memory leaks
        statisticsJob?.cancel()
        
        statisticsJob = viewModelScope.launch {
            getStatisticsUseCase()
                .catch { e ->
                    if (isActive) {
                        _uiState.update { it.copy(error = e.message) }
                    }
                }
                .collect { stats ->
                    if (isActive) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }
    
    private fun observeSyncStatus() {
        // Cancel previous job to prevent memory leaks
        syncStatusJob?.cancel()
        
        syncStatusJob = viewModelScope.launch {
            syncRepository.getLatestSyncRecord()
                .catch { e -> /* handle error */ }
                .collect { record ->
                    if (isActive) {
                        record?.let { rec ->
                            val status = when (rec.status) {
                                SyncRecordEntity.SyncStatus.IN_PROGRESS -> SyncStatus.Status.SYNCING
                                SyncRecordEntity.SyncStatus.SUCCESS -> SyncStatus.Status.SUCCESS
                                SyncRecordEntity.SyncStatus.FAILED -> SyncStatus.Status.ERROR
                                else -> SyncStatus.Status.IDLE
                            }
                            _syncStatus.value = SyncStatus(
                                status = status,
                                lastSyncTime = rec.endTime
                            )
                        }
                    }
                }
        }
    }
    
    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.Refresh -> loadStatistics()
            is MainUiEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is MainUiEvent.SyncData -> syncData()
            is MainUiEvent.ShowError -> _uiState.update { it.copy(error = event.message) }
        }
    }
    
    fun toggleCapture() {
        val currentState = _uiState.value
        if (currentState.isCapturing) {
            stopCapture()
        } else {
            startCapture()
        }
    }
    
    private fun startCapture() {
        viewModelScope.launch {
            try {
                val session = startSessionUseCase(
                    application = "示例应用",
                    packageName = "com.example.app"
                )
                _uiState.update { it.copy(
                    isCapturing = true,
                    currentSession = InputSession(
                        id = session.id,
                        application = session.application,
                        startTime = session.startTime,
                        inputCount = session.inputCount
                    )
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun stopCapture() {
        viewModelScope.launch {
            try {
                _uiState.value.currentSession?.let { session ->
                    endSessionUseCase(session.id)
                }
                _uiState.update { it.copy(
                    isCapturing = false,
                    currentSession = null
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun recordInput(content: String, characterCount: Int = content.length) {
        viewModelScope.launch {
            try {
                _uiState.value.currentSession?.let { session ->
                    recordInputUseCase(
                        sessionId = session.id,
                        content = content,
                        application = session.application,
                        characterCount = characterCount
                    )
                    val updatedSession = session.copy(inputCount = session.inputCount + 1)
                    _uiState.update { it.copy(currentSession = updatedSession) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun syncData() {
        viewModelScope.launch {
            try {
                _syncStatus.update { it.copy(status = SyncStatus.Status.SYNCING) }
                // TODO: 实现实际同步逻辑
                // 模拟同步延迟
                kotlinx.coroutines.delay(1000)
                _syncStatus.update { it.copy(
                    status = SyncStatus.Status.SUCCESS,
                    lastSyncTime = System.currentTimeMillis()
                )}
            } catch (e: Exception) {
                _syncStatus.update { it.copy(status = SyncStatus.Status.ERROR) }
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        statisticsJob?.cancel()
        syncStatusJob?.cancel()
        Log.d(TAG, "MainViewModel cleared, jobs cancelled")
    }
}
