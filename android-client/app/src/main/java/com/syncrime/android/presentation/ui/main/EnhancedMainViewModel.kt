package com.syncrime.android.presentation.ui.main

import androidx.lifecycle.viewModelScope
import com.syncrime.android.domain.usecase.*
import com.syncrime.android.intelligence.AndroidIntelligenceEngineManager
import com.syncrime.android.presentation.ui.main.MainUiEvent
import com.syncrime.android.presentation.ui.main.MainUiState
import com.syncrime.trime.plugin.intelligence.Recommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 增强的 MainViewModel
 * 集成智能化功能
 */
@HiltViewModel
class EnhancedMainViewModel @Inject constructor(
    private val manageInputUseCase: ManageInputUseCase,
    private val syncDataUseCase: SyncDataUseCase,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val getAvailableApplicationsUseCase: GetAvailableApplicationsUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getSyncStatusUseCase: GetSyncStatusUseCase,
    private val intelligenceEngine: AndroidIntelligenceEngineManager
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // 智能化状态
    private val _intelligenceState = MutableStateFlow(IntelligenceState())
    val intelligenceState: StateFlow<IntelligenceState> = _intelligenceState.asStateFlow()

    // 智能推荐
    private val _smartRecommendations = MutableStateFlow<List<RecommendationItem>>(emptyList())
    val smartRecommendations: StateFlow<List<RecommendationItem>> = _smartRecommendations.asStateFlow()

    // 输入分析结果
    private val _inputAnalysis = MutableStateFlow<InputAnalysisResult?>(null)
    val inputAnalysis: StateFlow<InputAnalysisResult?> = _inputAnalysis.asStateFlow()

    init {
        initialize()
        observeIntelligenceEngine()
    }

    /**
     * 初始化增强功能
     */
    private fun initialize() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 初始化基础功能
                loadUserProfile()
                loadAvailableApplications()
                loadStatistics()
                loadSyncStatus()

                // 初始化智能化引擎
                val initialized = intelligenceEngine.initialize()
                _intelligenceState.update { 
                    it.copy(
                        isInitialized = initialized,
                        isLoading = false,
                        error = if (initialized) null else "Failed to initialize intelligence engine"
                    )
                }

                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = if (initialized) null else "Failed to initialize intelligence engine"
                    )
                }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Initialization failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 增强的输入添加功能
     */
    fun addEnhancedInput(text: String, context: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            try {
                val sessionId = _uiState.value.currentSession?.id ?: return@launch

                // 1. 执行智能化分析
                val analysisResult = intelligenceEngine.analyzeInput(text, context)
                _inputAnalysis.value = analysisResult

                // 2. 更新智能推荐
                when (analysisResult) {
                    is InputAnalysisResult.Success -> {
                        val recommendations = analysisResult.recommendations
                            .map { RecommendationItem(it.text, it.type, it.confidence, it.source) }
                        _smartRecommendations.value = recommendations
                    }
                    is InputAnalysisResult.Error -> {
                        // 静默处理分析错误
                    }
                }

                // 3. 添加基础输入记录
                val recordResult = manageInputUseCase.addInputRecord(sessionId, text)
                recordResult.onSuccess { record ->
                    updateSessionStats(record)
                    updateRecentInputs(text)
                }

                // 4. 更新上下文
                val application = context["application"] as? String ?: ""
                val inputType = context["inputType"] as? String ?: "text"
                intelligenceEngine.updateContext(text, application, inputType)

            } catch (e: Exception) {
                // 静默处理错误，不影响用户体验
            }
        }
    }

    /**
     * 选择智能推荐
     */
    fun selectSmartRecommendation(recommendation: RecommendationItem) {
        viewModelScope.launch {
            try {
                // 1. 添加推荐到输入
                addEnhancedInput(recommendation.text)

                // 2. 学习用户行为
                intelligenceEngine.learnFromUserAction(
                    input = recommendation.text,
                    action = UserAction.AcceptRecommendation(
                        Recommendation(
                            text = recommendation.text,
                            type = recommendation.type,
                            confidence = recommendation.confidence,
                            source = recommendation.source
                        )
                    )
                )

                // 3. 从推荐列表中移除
                _smartRecommendations.update { recommendations ->
                    recommendations.filter { it.text != recommendation.text }
                }

            } catch (e: Exception) {
                // 静默处理错误
            }
        }
    }

    /**
     * 应用智能纠错
     */
    fun applySmartCorrection(correction: CorrectionSuggestion) {
        viewModelScope.launch {
            try {
                val originalText = _inputAnalysis.value?.let { 
                    when (it) {
                        is InputAnalysisResult.Success -> it.text
                        else -> ""
                    }
                } ?: return@launch

                // 1. 应用纠错
                val correctedText = intelligenceEngine.nativeCorrectionEngine
                    .applyCorrection(correction, originalText)

                // 2. 学习用户行为
                intelligenceEngine.learnFromUserAction(
                    input = originalText,
                    action = UserAction.AcceptCorrection(correction, originalText)
                )

                // 3. 添加纠错后的输入
                addEnhancedInput(correctedText)

            } catch (e: Exception) {
                // 静默处理错误
            }
        }
    }

    /**
     * 获取个性化洞察
     */
    fun getPersonalizationInsights() {
        viewModelScope.launch {
            try {
                val insights = intelligenceEngine.getPersonalizationInsights()
                _intelligenceState.update { 
                    it.copy(personalizationInsights = insights)
                }
            } catch (e: Exception) {
                _intelligenceState.update { 
                    it.copy(personalizationInsights = PersonalizationInsights.Error("Failed to get insights"))
                }
            }
        }
    }

    /**
     * 更新智能化配置
     */
    fun updateIntelligenceConfig(config: IntelligenceConfig) {
        viewModelScope.launch {
            try {
                intelligenceEngine.updateConfiguration(config)
                _intelligenceState.update { it.copy(config = config) }
            } catch (e: Exception) {
                _intelligenceState.update { 
                    it.copy(error = "Failed to update config: ${e.message}")
                }
            }
        }
    }

    /**
     * 刷新智能推荐
     */
    fun refreshSmartRecommendations(input: String = "") {
        viewModelScope.launch {
            try {
                val recommendations = if (input.isNotEmpty()) {
                    intelligenceEngine.getRecommendations(input)
                } else {
                    intelligenceEngine.getRecommendations("")
                }
                _smartRecommendations.value = recommendations
            } catch (e: Exception) {
                // 静默处理错误
            }
        }
    }

    /**
     * 增强的同步功能
     */
    override fun syncData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 1. 执行基础同步
                val syncResult = syncDataUseCase.syncData()
                syncResult.onSuccess {
                    updateSyncStatus(SyncStatus(Status.SUCCESS, System.currentTimeMillis()))
                }.onFailure { error ->
                    updateSyncStatus(SyncStatus(Status.ERROR, 0, errorMessage = error.message))
                }

                // 2. 同步智能化数据
                intelligenceEngine.updateConfiguration(_intelligenceState.value.config)

                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Enhanced sync failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 获取性能统计
     */
    fun getPerformanceStats() {
        viewModelScope.launch {
            try {
                val stats = intelligenceEngine.getPerformanceStats()
                _intelligenceState.update { 
                    it.copy(performanceStats = stats)
                }
            } catch (e: Exception) {
                _intelligenceState.update { 
                    it.copy(performanceStats = PerformanceStats.Error("Failed to get stats"))
                }
            }
        }
    }

    /**
     * 增强的事件处理
     */
    override fun handleEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.Refresh -> {
                initialize()
                refreshSmartRecommendations()
            }
            is MainUiEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
                _intelligenceState.update { it.copy(error = null) }
            }
            is MainUiEvent.SyncData -> {
                syncData()
            }
            is MainUiEvent.RefreshRecommendations -> {
                refreshSmartRecommendations(event.input)
            }
        }
    }

    // 私有方法

    private fun observeIntelligenceEngine() {
        viewModelScope.launch {
            // 监听智能引擎状态
            intelligenceEngine.isInitialized.collect { initialized ->
                _intelligenceState.update { it.copy(isInitialized = initialized) }
            }
        }
    }

    private fun updateSessionStats(record: InputRecord) {
        _uiState.value.currentSession?.let { session ->
            val updatedSession = session.copy(
                inputCount = session.inputCount + 1,
                lastInputTime = record.timestamp
            )
            _uiState.update { it.copy(currentSession = updatedSession) }
        }
    }

    private fun updateRecentInputs(input: String) {
        val currentInputs = _uiState.value.recentInputs.toMutableList()
        currentInputs.add(0, input) // 添加到开头
        
        if (currentInputs.size > 20) {
            currentInputs.removeAt(currentInputs.size - 1)
        }
        
        _uiState.update { it.copy(recentInputs = currentInputs) }
    }

    private fun updateSyncStatus(status: SyncStatus) {
        // 这里可以更新同步状态到UI
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            intelligenceEngine.cleanup()
        }
    }
}

// 增强的状态类
data class IntelligenceState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val config: IntelligenceConfig = IntelligenceConfig(),
    val personalizationInsights: PersonalizationInsights? = null,
    val performanceStats: PerformanceStats? = null,
    val error: String? = null
)

// 智能化配置
data class IntelligenceConfig(
    val enableRecommendations: Boolean = true,
    val enableContextAnalysis: Boolean = true,
    val enablePersonalizedLearning: Boolean = true,
    val enableSmartCorrection: Boolean = true,
    val enableSemanticAnalysis: Boolean = true,
    val enableMultilingualSupport: Boolean = true,
    val performanceMode: String = "balanced",
    val cacheSize: Int = 1000,
    val autoSyncInterval: Long = 300000L
)

// 基础 ViewModel 抽象类
abstract class BaseViewModel : ViewModel() {
    abstract fun handleEvent(event: MainUiEvent)
}

// 增强的UI事件
sealed class EnhancedMainUiEvent : MainUiEvent() {
    data class RefreshRecommendations(val input: String = "") : EnhancedMainUiEvent()
    data class UpdateIntelligenceConfig(val config: IntelligenceConfig) : EnhancedMainUiEvent()
    object GetPersonalizationInsights : EnhancedMainUiEvent()
    object GetPerformanceStats : EnhancedMainUiEvent()
}