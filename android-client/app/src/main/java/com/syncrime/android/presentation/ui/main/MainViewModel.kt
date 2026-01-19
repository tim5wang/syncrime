package com.syncrime.android.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncrime.android.domain.model.*
import com.syncrime.android.domain.usecase.*
import com.syncrime.android.presentation.ui.main.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主界面 ViewModel
 * 
 * 管理主界面的状态和业务逻辑
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val manageInputUseCase: ManageInputUseCase,
    private val syncDataUseCase: SyncDataUseCase,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val getAvailableApplicationsUseCase: GetAvailableApplicationsUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getSyncStatusUseCase: GetSyncStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()

    private val _statistics = MutableStateFlow<Statistics?>(null)
    val statistics: StateFlow<Statistics?> = _statistics.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _availableApplications = MutableStateFlow<List<ApplicationInfo>>(emptyList())
    val availableApplications: StateFlow<List<ApplicationInfo>> = _availableApplications.asStateFlow()

    init {
        initialize()
        observeData()
    }

    /**
     * 初始化 ViewModel
     */
    fun initialize() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 加载用户资料
                loadUserProfile()

                // 加载可用应用
                loadAvailableApplications()

                // 加载统计信息
                loadStatistics()

                // 加载同步状态
                loadSyncStatus()

                _uiState.update { 
                    it.copy(isLoading = false, error = null)
                }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    /**
     * 显示应用选择器
     */
    fun showApplicationSelector() {
        _uiState.update { it.copy(showApplicationSelector = true) }
    }

    /**
     * 隐藏应用选择器
     */
    fun hideApplicationSelector() {
        _uiState.update { it.copy(showApplicationSelector = false) }
    }

    /**
     * 开始输入会话
     */
    fun startInputSession(packageName: String, appName: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val sessionResult = manageInputUseCase.startInputSession(packageName, "text")
                sessionResult.onSuccess { session ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isCapturing = true,
                            currentSession = session,
                            showApplicationSelector = false,
                            error = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(isLoading = false, error = error.message)
                    }
                }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    /**
     * 停止输入采集
     */
    fun stopCapture() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val result = manageInputUseCase.endInputSession()
                result.onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isCapturing = false,
                            currentSession = null,
                            error = null
                        )
                    }

                    // 更新统计信息
                    loadStatistics()

                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(isLoading = false, error = error.message)
                    }
                }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    /**
     * 添加输入记录
     */
    fun addInput(text: String) {
        viewModelScope.launch {
            try {
                val sessionId = _uiState.value.currentSession?.id ?: return@launch
                
                val recordResult = manageInputUseCase.addInputRecord(sessionId, text)
                recordResult.onSuccess { record ->
                    // 更新推荐
                    updateRecommendations(text)

                    // 更新当前会话
                    _uiState.value.currentSession?.let { session ->
                        val updatedSession = session.copy(
                            inputCount = session.inputCount + 1,
                            lastInputTime = record.timestamp
                        )
                        _uiState.update { it.copy(currentSession = updatedSession) }
                    }

                    // 添加到最近输入
                    addToRecentInputs(text)

                }.onFailure { error ->
                    // 静默处理错误，不影响用户体验
                }

            } catch (e: Exception) {
                // 静默处理错误
            }
        }
    }

    /**
     * 选择推荐
     */
    fun selectRecommendation(recommendation: Recommendation) {
        viewModelScope.launch {
            try {
                // 添加输入记录
                addInput(recommendation.text)

                // 从推荐列表中移除已选择的项
                _recommendations.update { recommendations ->
                    recommendations.filter { it.text != recommendation.text }
                }

            } catch (e: Exception) {
                // 静默处理错误
            }
        }
    }

    /**
     * 选择输入
     */
    fun selectInput(input: String) {
        viewModelScope.launch {
            try {
                // 添加输入记录
                addInput(input)

            } catch (e: Exception) {
                // 静默处理错误
            }
        }
    }

    /**
     * 同步数据
     */
    fun syncData() {
        viewModelScope.launch {
            try {
                _syncStatus.update { it.copy(status = SyncStatus.Status.SYNCING) }

                val syncResult = syncDataUseCase.syncData()
                syncResult.onSuccess { result ->
                    _syncStatus.update { 
                        it.copy(
                            status = SyncStatus.Status.SUCCESS,
                            lastSyncTime = System.currentTimeMillis(),
                            error = null
                        )
                    }

                    // 更新统计信息
                    loadStatistics()

                }.onFailure { error ->
                    _syncStatus.update { 
                        it.copy(
                            status = SyncStatus.Status.ERROR,
                            error = error.message
                        )
                    }
                }

            } catch (e: Exception) {
                _syncStatus.update { 
                    it.copy(
                        status = SyncStatus.Status.ERROR,
                        error = e.message
                    )
                }
            }
        }
    }

    /**
     * 处理界面事件
     */
    fun handleEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.Refresh -> {
                initialize()
            }
            is MainUiEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
            is MainUiEvent.SyncData -> {
                syncData()
            }
        }
    }

    // 私有方法

    private fun observeData() {
        // 监听推荐更新
        viewModelScope.launch {
            // 这里可以添加定期更新推荐的逻辑
        }

        // 监听同步状态更新
        viewModelScope.launch {
            // 这里可以添加定期检查同步状态的逻辑
        }
    }

    private suspend fun loadUserProfile() {
        getUserProfileUseCase.getUserProfile().onSuccess { profile ->
            _uiState.update { 
                it.copy(
                    userName = profile.name,
                    userAvatar = profile.avatarUrl
                )
            }
        }
    }

    private suspend fun loadAvailableApplications() {
        getAvailableApplicationsUseCase.getAvailableApplications().onSuccess { applications ->
            _availableApplications.value = applications
        }
    }

    private suspend fun loadStatistics() {
        getStatisticsUseCase.getStatistics().onSuccess { stats ->
            _statistics.value = stats
        }
    }

    private suspend fun loadSyncStatus() {
        getSyncStatusUseCase.getSyncStatus().onSuccess { status ->
            _syncStatus.value = status
        }
    }

    private suspend fun updateRecommendations(input: String) {
        try {
            getRecommendationsUseCase.getRecommendations(input).onSuccess { recommendations ->
                _recommendations.value = recommendations
            }
        } catch (e: Exception) {
            // 静默处理推荐更新失败
        }
    }

    private fun addToRecentInputs(input: String) {
        val currentInputs = _uiState.value.recentInputs.toMutableList()
        currentInputs.add(0, input) // 添加到开头
        
        // 保持最多 20 条记录
        if (currentInputs.size > 20) {
            currentInputs.removeAt(currentInputs.size - 1)
        }
        
        _uiState.update { it.copy(recentInputs = currentInputs) }
    }
}