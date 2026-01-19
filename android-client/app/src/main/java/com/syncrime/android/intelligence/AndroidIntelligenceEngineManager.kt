package com.syncrime.android.intelligence

import android.content.Context
import com.syncrime.android.intelligence.recommendation.AndroidRecommendationEngine
import com.syncrime.android.intelligence.context.AndroidContextEngine
import com.syncrime.android.intelligence.learning.AndroidLearningEngine
import com.syncrime.android.intelligence.correction.AndroidCorrectionEngine
import com.syncrime.android.intelligence.semantic.AndroidSemanticEngine
import com.syncrime.android.intelligence.multilingual.AndroidMultilingualEngine
import com.syncrime.trime.plugin.intelligence.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android 智能化引擎管理器
 * 
 * 管理 Android 平台上的所有智能化功能，
 * 将 Phase 2 的智能引擎与 Android 平台特性集成
 */
@Singleton
class AndroidIntelligenceEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recommendationEngine: AndroidRecommendationEngine,
    private val contextEngine: AndroidContextEngine,
    private val learningEngine: AndroidLearningEngine,
    private val correctionEngine: AndroidCorrectionEngine,
    private val semanticEngine: AndroidSemanticEngine,
    private val multilingualEngine: AndroidMultilingualEngine
) {
    
    // 原生智能引擎实例
    private val nativeRecommendationEngine = IntelligentRecommendationEngine(context)
    private val nativeContextEngine = ContextAwareInputEngine(context)
    private val nativeLearningEngine = PersonalizedLearningEngine(context)
    private val nativeCorrectionEngine = IntelligentCorrectionEngine(context)
    private val nativeSemanticEngine = SemanticAnalysisEngine(context)
    private val nativeMultilingualEngine = MultilingualIntelligentEngine(context)
    
    // 初始化状态
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: Flow<Boolean> = _isInitialized.asStateFlow()
    
    // 统一配置
    data class IntelligenceConfig(
        val enableRecommendations: Boolean = true,
        val enableContextAnalysis: Boolean = true,
        val enablePersonalizedLearning: Boolean = true,
        val enableSmartCorrection: Boolean = true,
        val enableSemanticAnalysis: Boolean = true,
        val enableMultilingualSupport: Boolean = true,
        val performanceMode: String = "balanced", // "performance", "balanced", "accuracy"
        val cacheSize: Int = 1000,
        val autoSyncInterval: Long = 300000L // 5分钟
    )
    
    private val _config = MutableStateFlow(IntelligenceConfig())
    val config: Flow<IntelligenceConfig> = _config.asStateFlow()
    
    /**
     * 初始化所有智能引擎
     */
    suspend fun initialize(): Boolean {
        return try {
            // 初始化 Android 平台特定引擎
            recommendationEngine.initialize()
            contextEngine.initialize()
            learningEngine.initialize()
            correctionEngine.initialize()
            semanticEngine.initialize()
            multilingualEngine.initialize()
            
            // 初始化原生引擎
            nativeRecommendationEngine.initialize()
            nativeContextEngine.initialize()
            nativeLearningEngine.initialize()
            nativeCorrectionEngine.initialize()
            nativeSemanticEngine.initialize()
            nativeMultilingualEngine.initialize()
            
            // 同步配置
            syncConfiguration()
            
            _isInitialized.value = true
            true
            
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 分析输入文本
     */
    suspend fun analyzeInput(
        text: String,
        context: Map<String, Any> = emptyMap()
    ): InputAnalysisResult {
        if (!_isInitialized.value) {
            return InputAnalysisResult.Error("Engine not initialized")
        }
        
        return try {
            // 并行执行多种分析
            val recommendationResult = nativeRecommendationEngine.generateRecommendations(text)
            val contextResult = nativeContextEngine.getContextualSuggestions(text, context)
            val correctionResult = nativeCorrectionEngine.checkAndCorrect(text)
            val semanticResult = nativeSemanticEngine.analyzeText(text)
            val multilingualResult = nativeMultilingualEngine.detectLanguage(text)
            
            // 融合分析结果
            InputAnalysisResult.Success(
                text = text,
                recommendations = recommendationResult.take(5),
                contextualSuggestions = contextResult.take(5),
                corrections = correctionResult.take(3),
                semantic = semanticResult,
                language = multilingualResult,
                confidence = calculateOverallConfidence(recommendationResult, contextResult, semanticResult),
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            InputAnalysisResult.Error("Analysis failed: ${e.message}")
        }
    }
    
    /**
     * 生成智能推荐
     */
    suspend fun getRecommendations(
        input: String,
        maxRecommendations: Int = 10
    ): List<RecommendationItem> {
        if (!_isInitialized.value) return emptyList()
        
        return try {
            val nativeRecommendations = nativeRecommendationEngine.generateRecommendations(input)
            val androidRecommendations = recommendationEngine.getRecommendations(input)
            
            // 合并并排序推荐
            (nativeRecommendations.map { it.toRecommendationItem() } + androidRecommendations)
                .distinctBy { it.text }
                .sortedByDescending { it.confidence }
                .take(maxRecommendations)
                
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 更新上下文
     */
    suspend fun updateContext(
        input: String,
        application: String,
        inputType: String
    ) {
        if (!_isInitialized.value) return
        
        try {
            nativeContextEngine.updateCurrentContext(input, application, inputType)
            contextEngine.updateContext(input, application, inputType)
            
        } catch (e: Exception) {
            // 静默处理错误
        }
    }
    
    /**
     * 学习用户行为
     */
    suspend fun learnFromUserAction(
        input: String,
        action: UserAction,
        context: Map<String, Any> = emptyMap()
    ) {
        if (!_isInitialized.value) return
        
        try {
            when (action) {
                is UserAction.AcceptRecommendation -> {
                    nativeRecommendationEngine.acceptRecommendation(action.recommendation)
                    recommendationEngine.learnRecommendationAccepted(input, action.recommendation)
                }
                is UserAction.RejectRecommendation -> {
                    nativeRecommendationEngine.rejectRecommendation(action.recommendation)
                    recommendationEngine.learnRecommendationRejected(input, action.recommendation)
                }
                is UserAction.AcceptCorrection -> {
                    nativeCorrectionEngine.acceptCorrection(action.correction, input)
                    correctionEngine.learnCorrectionAccepted(input, action.correction)
                }
                is UserAction.InputCompleted -> {
                    nativeLearningEngine.learnFromInput(input, context, true)
                    learningEngine.learnFromInput(input, context, true)
                }
            }
            
        } catch (e: Exception) {
            // 静默处理学习错误
        }
    }
    
    /**
     * 获取个性化洞察
     */
    suspend fun getPersonalizationInsights(): PersonalizationInsights {
        if (!_isInitialized.value) {
            return PersonalizationInsights.Error("Engine not initialized")
        }
        
        return try {
            val nativeInsights = nativeLearningEngine.getPersonalizationInsights()
            val androidInsights = learningEngine.getInsights()
            
            PersonalizationInsights.Success(
                userName = androidInsights["user_name"] ?: "用户",
                vocabularySize = androidInsights["vocabulary_size"]?.toString()?.toIntOrNull() ?: 0,
                favoriteWords = androidInsights["favorite_words"]?.toString()?.let { 
                    parseStringList(it)
                } ?: emptyList(),
                formalityLevel = androidInsights["formality_level"]?.toString() ?: "中性",
                typingSpeed = androidInsights["typing_speed"]?.toString() ?: "50 WPM",
                accuracyRate = androidInsights["correction_rate"]?.toString()?.let { 
                    it.replace("%", "").toFloatOrNull() / 100f
                } ?: 0.95f,
                learningProgress = androidInsights["learning_progress"]?.toString()?.toIntOrNull() ?: 0,
                adaptationCount = androidInsights["adaptation_count"]?.toString()?.toIntOrNull() ?: 0
            )
            
        } catch (e: Exception) {
            PersonalizationInsights.Error("Failed to get insights: ${e.message}")
        }
    }
    
    /**
     * 更新配置
     */
    suspend fun updateConfiguration(newConfig: IntelligenceConfig) {
        _config.value = newConfig
        
        try {
            // 更新原生引擎配置
            updateNativeEngineConfig(newConfig)
            
            // 更新 Android 引擎配置
            updateAndroidEngineConfig(newConfig)
            
        } catch (e: Exception) {
            // 静默处理配置更新错误
        }
    }
    
    /**
     * 获取性能统计
     */
    suspend fun getPerformanceStats(): PerformanceStats {
        if (!_isInitialized.value) {
            return PerformanceStats.Error("Engine not initialized")
        }
        
        return try {
            PerformanceStats.Success(
                recommendationLatency = measureTimeMillis {
                    nativeRecommendationEngine.generateRecommendations("test")
                },
                contextAnalysisLatency = measureTimeMillis {
                    nativeContextEngine.getContextualSuggestions("test")
                },
                correctionLatency = measureTimeMillis {
                    nativeCorrectionEngine.checkAndCorrect("test")
                },
                semanticAnalysisLatency = measureTimeMillis {
                    nativeSemanticEngine.analyzeText("test")
                },
                multilingualDetectionLatency = measureTimeMillis {
                    nativeMultilingualEngine.detectLanguage("test")
                },
                memoryUsage = getMemoryUsage(),
                cacheHitRate = getCacheHitRate()
            )
            
        } catch (e: Exception) {
            PerformanceStats.Error("Failed to get stats: ${e.message}")
        }
    }
    
    /**
     * 清理资源
     */
    suspend fun cleanup() {
        try {
            // 清理原生引擎
            nativeRecommendationEngine.cleanup()
            nativeContextEngine.cleanup()
            nativeLearningEngine.cleanup()
            nativeCorrectionEngine.cleanup()
            nativeSemanticEngine.cleanup()
            nativeMultilingualEngine.cleanup()
            
            // 清理 Android 引擎
            recommendationEngine.cleanup()
            contextEngine.cleanup()
            learningEngine.cleanup()
            correctionEngine.cleanup()
            semanticEngine.cleanup()
            multilingualEngine.cleanup()
            
            _isInitialized.value = false
            
        } catch (e: Exception) {
            // 静默处理清理错误
        }
    }
    
    // 私有方法
    
    private suspend fun syncConfiguration() {
        val currentConfig = _config.value
        
        try {
            // 同步推荐配置
            nativeRecommendationEngine.setJniCallbacks(object : SyncRimePlugin.PluginCallbacks {
                override fun onInputCaptured(text: String) {
                    recommendationEngine.onInputCaptured(text)
                }
                
                override fun onSyncStarted() {
                    // 处理同步开始
                }
                
                override fun onSyncCompleted(success: Boolean) {
                    // 处理同步完成
                }
                
                override fun onError(error: SyncRimePlugin.ErrorCode, message: String) {
                    // 处理错误
                }
            })
            
        } catch (e: Exception) {
            // 静默处理同步错误
        }
    }
    
    private suspend fun updateNativeEngineConfig(config: IntelligenceConfig) {
        // 这里可以更新原生引擎的配置
        // 例如调整缓存大小、性能模式等
    }
    
    private suspend fun updateAndroidEngineConfig(config: IntelligenceConfig) {
        // 更新 Android 引擎配置
        recommendationEngine.updateConfig(config.enableRecommendations, config.performanceMode)
        contextEngine.updateConfig(config.enableContextAnalysis, config.cacheSize)
        learningEngine.updateConfig(config.enablePersonalizedLearning)
        correctionEngine.updateConfig(config.enableSmartCorrection)
        semanticEngine.updateConfig(config.enableSemanticAnalysis)
        multilingualEngine.updateConfig(config.enableMultilingualSupport)
    }
    
    private fun calculateOverallConfidence(
        recommendations: List<Recommendation>,
        contextSuggestions: List<String>,
        semantic: SemanticAnalysisResult
    ): Float {
        val recConfidence = if (recommendations.isNotEmpty()) {
            recommendations.map { it.confidence }.average().toFloat()
        } else 0f
        
        val contextConfidence = if (contextSuggestions.isNotEmpty()) 0.7f else 0f
        
        val semanticConfidence = semantic.confidence
        
        return (recConfidence * 0.4f + contextConfidence * 0.3f + semanticConfidence * 0.3f)
            .coerceIn(0f, 1f)
    }
    
    private fun Recommendation.toRecommendationItem(): RecommendationItem {
        return RecommendationItem(
            text = this.text,
            type = this.type,
            confidence = this.confidence,
            source = this.source,
            metadata = this.metadata
        )
    }
    
    private fun parseStringList(stringList: String): List<String> {
        return stringList
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
    }
    
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun getCacheHitRate(): Float {
        // 这里可以实现缓存命中率统计
        return 0.85f // 示例值
    }
    
    private inline fun <T> measureTimeMillis(block: () -> T): Long {
        val start = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - start
    }
}

// 辅助数据类

sealed class InputAnalysisResult {
    data class Success(
        val text: String,
        val recommendations: List<Recommendation>,
        val contextualSuggestions: List<String>,
        val corrections: List<CorrectionSuggestion>,
        val semantic: SemanticAnalysisResult,
        val language: LanguageDetectionResult,
        val confidence: Float,
        val timestamp: Long
    ) : InputAnalysisResult()
    
    data class Error(val message: String) : InputAnalysisResult()
}

data class RecommendationItem(
    val text: String,
    val type: String,
    val confidence: Float,
    val source: String,
    val metadata: Map<String, Any> = emptyMap()
)

sealed class UserAction {
    data class AcceptRecommendation(val recommendation: RecommendationItem) : UserAction()
    data class RejectRecommendation(val recommendation: RecommendationItem) : UserAction()
    data class AcceptCorrection(val correction: CorrectionSuggestion, val originalText: String) : UserAction()
    data class InputCompleted(val input: String, val context: Map<String, Any>) : UserAction()
}

sealed class PersonalizationInsights {
    data class Success(
        val userName: String,
        val vocabularySize: Int,
        val favoriteWords: List<String>,
        val formalityLevel: String,
        val typingSpeed: String,
        val accuracyRate: Float,
        val learningProgress: Int,
        val adaptationCount: Int
    ) : PersonalizationInsights()
    
    data class Error(val message: String) : PersonalizationInsights()
}

sealed class PerformanceStats {
    data class Success(
        val recommendationLatency: Long,
        val contextAnalysisLatency: Long,
        val correctionLatency: Long,
        val semanticAnalysisLatency: Long,
        val multilingualDetectionLatency: Long,
        val memoryUsage: Long,
        val cacheHitRate: Float
    ) : PerformanceStats()
    
    data class Error(val message: String) : PerformanceStats()
}