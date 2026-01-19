package com.syncrime.trime.plugin.intelligence

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 智能输入推荐系统
 * 
 * 基于用户输入历史、上下文信息和机器学习算法，
 * 为用户提供个性化的输入推荐和预测。
 */
class IntelligentRecommendationEngine(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "IntelligentRecommendationEngine"
        
        // 推荐类型
        const val TYPE_WORD = "word"
        const val TYPE_PHRASE = "phrase"
        const val TYPE_SENTENCE = "sentence"
        const val TYPE_EMOJI = "emoji"
        const val TYPE_CONTACT = "contact"
        const val TYPE_URL = "url"
        const val TYPE_EMAIL = "email"
        
        // 推荐来源
        const val SOURCE_HISTORY = "history"
        const val SOURCE_CONTEXT = "context"
        const val SOURCE_ML = "ml"
        const val SOURCE_DICTIONARY = "dictionary"
        const val SOURCE_CONTACTS = "contacts"
        
        // 配置参数
        private const val MAX_RECOMMENDATIONS = 10
        private const val MIN_CONFIDENCE = 0.1f
        private const val HISTORY_WEIGHT = 0.4f
        private const val CONTEXT_WEIGHT = 0.3f
        private const val ML_WEIGHT = 0.3f
    }
    
    // 推荐数据类
    data class Recommendation(
        val text: String,
        val type: String,
        val confidence: Float,
        val source: String,
        val metadata: Map<String, Any> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // 上下文信息
    data class InputContext(
        val currentInput: String = "",
        val previousInputs: List<String> = emptyList(),
        val application: String = "",
        val inputType: String = "",
        val timeOfDay: Int = 0,
        val dayOfWeek: Int = 0,
        val location: String = "",
        val recentContacts: List<String> = emptyList()
    )
    
    // 用户偏好
    data class UserPreferences(
        val frequentWords: Map<String, Float> = emptyMap(),
        val wordPairs: Map<String, Map<String, Float>> = emptyMap(),
        val phrasePatterns: Map<String, Float> = emptyMap(),
        val timePatterns: Map<String, Map<String, Float>> = emptyMap(),
        val appPatterns: Map<String, Map<String, Float>> = emptyMap()
    )
    
    // 机器学习模型接口
    interface MLModel {
        suspend fun predict(input: String, context: InputContext): List<Recommendation>
        fun train(data: List<Pair<String, Recommendation>>)
        fun saveModel(path: String)
        fun loadModel(path: String)
    }
    
    // 核心组件
    private val recommendationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val contextMutex = Mutex()
    private val preferencesMutex = Mutex()
    
    // 数据存储
    private val inputHistory = mutableListOf<String>()
    private val userPreferences = UserPreferences()
    private val contextCache = ConcurrentHashMap<String, InputContext>()
    private val mlModels = mutableMapOf<String, MLModel>()
    
    // 状态流
    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()
    
    private val _context = MutableStateFlow(InputContext())
    val inputContext: StateFlow<InputContext> = _context.asStateFlow()
    
    private val _userPreferences = MutableStateFlow(userPreferences)
    val userPreferencesState: StateFlow<UserPreferences> = _userPreferences.asStateFlow()
    
    // 统计信息
    private var totalRecommendations = 0L
    private var acceptedRecommendations = 0L
    private var lastUpdateTime = 0L
    
    /**
     * 初始化推荐引擎
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 加载用户偏好
            loadUserPreferences()
            
            // 加载机器学习模型
            loadMLModels()
            
            // 初始化上下文
            initializeContext()
            
            lastUpdateTime = System.currentTimeMillis()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 生成推荐
     */
    suspend fun generateRecommendations(
        input: String,
        context: InputContext = _context.value
    ): List<Recommendation> = withContext(Dispatchers.Default) {
        if (input.length < 2) return@withContext emptyList()
        
        val allRecommendations = mutableListOf<Recommendation>()
        
        // 基于历史的推荐
        val historyRecs = generateHistoryRecommendations(input, context)
        allRecommendations.addAll(historyRecs)
        
        // 基于上下文的推荐
        val contextRecs = generateContextRecommendations(input, context)
        allRecommendations.addAll(contextRecs)
        
        // 基于机器学习的推荐
        val mlRecs = generateMLRecommendations(input, context)
        allRecommendations.addAll(mlRecs)
        
        // 基于字典的推荐
        val dictRecs = generateDictionaryRecommendations(input, context)
        allRecommendations.addAll(dictRecs)
        
        // 基于联系人的推荐
        val contactRecs = generateContactRecommendations(input, context)
        allRecommendations.addAll(contactRecs)
        
        // 合并和排序推荐
        mergeAndSortRecommendations(allRecommendations)
    }
    
    /**
     * 更新输入上下文
     */
    suspend fun updateContext(
        input: String,
        application: String = "",
        inputType: String = ""
    ) = contextMutex.withLock {
        val calendar = Calendar.getInstance()
        val newContext = _context.value.copy(
            currentInput = input,
            previousInputs = (_context.value.previousInputs + input).takeLast(10),
            application = application,
            inputType = inputType,
            timeOfDay = calendar.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            location = getCurrentLocation()
        )
        
        _context.value = newContext
        
        // 添加到历史记录
        addToHistory(input)
    }
    
    /**
     * 用户接受推荐
     */
    suspend fun acceptRecommendation(recommendation: Recommendation) {
        acceptedRecommendations++
        totalRecommendations++
        
        // 更新用户偏好
        updateUserPreferences(recommendation)
        
        // 训练机器学习模型
        trainMLModels(recommendation)
        
        // 更新推荐
        updateRecommendations()
    }
    
    /**
     * 拒绝推荐
     */
    suspend fun rejectRecommendation(recommendation: Recommendation) {
        totalRecommendations++
        
        // 降低推荐权重
        lowerRecommendationWeight(recommendation)
        
        // 更新推荐
        updateRecommendations()
    }
    
    /**
     * 获取推荐统计
     */
    fun getRecommendationStats(): Map<String, Any> {
        return mapOf(
            "total_recommendations" to totalRecommendations,
            "accepted_recommendations" to acceptedRecommendations,
            "acceptance_rate" to if (totalRecommendations > 0) {
                acceptedRecommendations.toFloat() / totalRecommendations.toFloat()
            } else 0f,
            "last_update_time" to lastUpdateTime,
            "active_models" to mlModels.size,
            "context_cache_size" to contextCache.size
        )
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        recommendationScope.cancel()
        
        // 保存用户偏好
        recommendationScope.launch {
            saveUserPreferences()
            saveMLModels()
        }
    }
    
    // 私有方法实现
    
    private suspend fun loadUserPreferences() = preferencesMutex.withLock {
        // TODO: 从本地存储加载用户偏好
    }
    
    private suspend fun saveUserPreferences() = preferencesMutex.withLock {
        // TODO: 保存用户偏好到本地存储
    }
    
    private suspend fun loadMLModels() = withContext(Dispatchers.IO) {
        try {
            // 加载词汇预测模型
            val wordModel = WordPredictionModel(context)
            if (wordModel.loadModel("word_prediction.bin")) {
                mlModels["word"] = wordModel
            }
            
            // 加载短语预测模型
            val phraseModel = PhrasePredictionModel(context)
            if (phraseModel.loadModel("phrase_prediction.bin")) {
                mlModels["phrase"] = phraseModel
            }
            
            // 加载上下文预测模型
            val contextModel = ContextPredictionModel(context)
            if (contextModel.loadModel("context_prediction.bin")) {
                mlModels["context"] = contextModel
            }
            
        } catch (e: Exception) {
            // 模型加载失败，使用默认算法
        }
    }
    
    private suspend fun saveMLModels() = withContext(Dispatchers.IO) {
        mlModels.values.forEach { model ->
            try {
                model.saveModel(getModelPath(model))
            } catch (e: Exception) {
                // 保存失败
            }
        }
    }
    
    private suspend fun initializeContext() {
        val calendar = Calendar.getInstance()
        _context.value = InputContext(
            timeOfDay = calendar.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            location = getCurrentLocation()
        )
    }
    
    private fun generateHistoryRecommendations(
        input: String,
        context: InputContext
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // 查找历史中相似的输入
        userPreferences.frequentWords.forEach { (word, frequency) ->
            if (word.startsWith(input, ignoreCase = true)) {
                recommendations.add(
                    Recommendation(
                        text = word,
                        type = TYPE_WORD,
                        confidence = frequency * HISTORY_WEIGHT,
                        source = SOURCE_HISTORY,
                        metadata = mapOf("frequency" to frequency)
                    )
                )
            }
        }
        
        // 查找常见词对
        if (context.previousInputs.isNotEmpty()) {
            val lastWord = context.previousInputs.last()
            userPreferences.wordPairs[lastWord]?.forEach { (nextWord, probability) ->
                if (nextWord.startsWith(input, ignoreCase = true)) {
                    recommendations.add(
                        Recommendation(
                            text = nextWord,
                            type = TYPE_WORD,
                            confidence = probability * HISTORY_WEIGHT,
                            source = SOURCE_HISTORY,
                            metadata = mapOf("pair" to "$lastWord -> $nextWord")
                        )
                    )
                }
            }
        }
        
        return recommendations
    }
    
    private fun generateContextRecommendations(
        input: String,
        context: InputContext
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // 基于应用上下文的推荐
        userPreferences.appPatterns[context.application]?.forEach { (word, probability) ->
            if (word.startsWith(input, ignoreCase = true)) {
                recommendations.add(
                    Recommendation(
                        text = word,
                        type = TYPE_WORD,
                        confidence = probability * CONTEXT_WEIGHT,
                        source = SOURCE_CONTEXT,
                        metadata = mapOf("app" to context.application)
                    )
                )
            }
        }
        
        // 基于时间模式的推荐
        val timeKey = "${context.timeOfDay}_${context.dayOfWeek}"
        userPreferences.timePatterns[timeKey]?.forEach { (word, probability) ->
            if (word.startsWith(input, ignoreCase = true)) {
                recommendations.add(
                    Recommendation(
                        text = word,
                        type = TYPE_WORD,
                        confidence = probability * CONTEXT_WEIGHT,
                        source = SOURCE_CONTEXT,
                        metadata = mapOf("time" to timeKey)
                    )
                )
            }
        }
        
        return recommendations
    }
    
    private suspend fun generateMLRecommendations(
        input: String,
        context: InputContext
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        mlModels.forEach { (modelType, model) ->
            try {
                val modelRecommendations = model.predict(input, context)
                recommendations.addAll(
                    modelRecommendations.map { rec ->
                        rec.copy(
                            confidence = rec.confidence * ML_WEIGHT,
                            source = SOURCE_ML,
                            metadata = rec.metadata + ("model_type" to modelType)
                        )
                    }
                )
            } catch (e: Exception) {
                // 模型预测失败
            }
        }
        
        return recommendations
    }
    
    private fun generateDictionaryRecommendations(
        input: String,
        context: InputContext
    ): List<Recommendation> {
        // TODO: 实现基于字典的推荐
        return emptyList()
    }
    
    private fun generateContactRecommendations(
        input: String,
        context: InputContext
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // 搜索联系人姓名
        context.recentContacts.forEach { contact ->
            if (contact.startsWith(input, ignoreCase = true)) {
                recommendations.add(
                    Recommendation(
                        text = contact,
                        type = TYPE_CONTACT,
                        confidence = 0.7f,
                        source = SOURCE_CONTACTS,
                        metadata = mapOf("contact" to contact)
                    )
                )
            }
        }
        
        return recommendations
    }
    
    private fun mergeAndSortRecommendations(
        recommendations: List<Recommendation>
    ): List<Recommendation> {
        return recommendations
            .groupBy { it.text }
            .map { (text, recs) ->
                // 合并相同文本的推荐
                val maxConfidence = recs.maxOf { it.confidence }
                val combinedSources = recs.joinToString(",") { it.source }
                val combinedMetadata = recs.flatMap { it.metadata.entries }.toMap()
                
                recs.first().copy(
                    confidence = maxConfidence,
                    source = combinedSources,
                    metadata = combinedMetadata
                )
            }
            .filter { it.confidence >= MIN_CONFIDENCE }
            .sortedByDescending { it.confidence }
            .take(MAX_RECOMMENDATIONS)
    }
    
    private suspend fun addToHistory(input: String) {
        inputHistory.add(input)
        
        // 保持历史记录在合理大小
        if (inputHistory.size > 10000) {
            inputHistory.removeAt(0)
        }
    }
    
    private suspend fun updateUserPreferences(recommendation: Recommendation) = preferencesMutex.withLock {
        // 更新词频
        val currentFreq = userPreferences.frequentWords[recommendation.text] ?: 0f
        val newFreq = (currentFreq + 1).coerceAtMost(100f)
        
        val newFrequentWords = userPreferences.frequentWords.toMutableMap()
        newFrequentWords[recommendation.text] = newFreq
        
        // 更新词对
        if (_context.value.previousInputs.isNotEmpty()) {
            val lastWord = _context.value.previousInputs.last()
            val wordPairs = userPreferences.wordPairs.toMutableMap()
            val nextWords = wordPairs[lastWord]?.toMutableMap() ?: mutableMapOf()
            nextWords[recommendation.text] = (nextWords[recommendation.text] ?: 0f) + 1f
            wordPairs[lastWord] = nextWords
            
            _userPreferences.value = userPreferences.copy(
                frequentWords = newFrequentWords,
                wordPairs = wordPairs
            )
        } else {
            _userPreferences.value = userPreferences.copy(
                frequentWords = newFrequentWords
            )
        }
    }
    
    private suspend fun trainMLModels(recommendation: Recommendation) {
        mlModels.values.forEach { model ->
            try {
                model.train(listOf(Pair(_context.value.currentInput, recommendation)))
            } catch (e: Exception) {
                // 训练失败
            }
        }
    }
    
    private suspend fun lowerRecommendationWeight(recommendation: Recommendation) {
        // 降低推荐权重
        val currentFreq = userPreferences.frequentWords[recommendation.text] ?: 0f
        val newFreq = (currentFreq * 0.9f).coerceAtLeast(0.1f)
        
        val newFrequentWords = userPreferences.frequentWords.toMutableMap()
        newFrequentWords[recommendation.text] = newFreq
        
        _userPreferences.value = userPreferences.copy(frequentWords = newFrequentWords)
    }
    
    private suspend fun updateRecommendations() {
        val currentRecommendations = generateRecommendations(_context.value.currentInput)
        _recommendations.value = currentRecommendations
    }
    
    private fun getCurrentLocation(): String {
        // TODO: 实现位置获取
        return ""
    }
    
    private fun getModelPath(model: MLModel): String {
        return "${context.filesDir}/models/${model.javaClass.simpleName}.bin"
    }
}