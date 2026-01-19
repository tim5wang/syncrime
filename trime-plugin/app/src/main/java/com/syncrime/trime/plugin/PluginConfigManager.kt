package com.syncrime.trime.plugin

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 插件配置管理器
 * 
 * 负责管理插件的配置文件，包括加载、保存、验证和监听配置变化。
 */
class PluginConfigManager {
    
    companion object {
        private const val TAG = "PluginConfigManager"
        private const val CONFIG_FILE_NAME = "syncrime_config.json"
        private const val CONFIG_VERSION = 1
        
        // 默认配置
        private val DEFAULT_CONFIG = mapOf(
            "server" to mapOf(
                "url" to "https://api.syncrime.com",
                "timeout" to 30000,
                "retry_count" to 3
            ),
            "sync" to mapOf(
                "auto_sync" to true,
                "sync_interval" to 300,
                "wifi_only" to true,
                "compression" to true,
                "encryption" to true
            ),
            "capture" to mapOf(
                "capture_mode" to "smart",
                "session_timeout" to 300000,
                "min_session_length" to 3,
                "capture_sensitive_data" to false,
                "capture_emoji" to true,
                "capture_gestures" to true
            ),
            "analysis" to mapOf(
                "enable_pattern_analysis" to true,
                "enable_importance_analysis" to true,
                "enable_category_analysis" to true,
                "min_text_length" to 3,
                "max_text_length" to 10000
            ),
            "storage" to mapOf(
                "max_sessions" to 1000,
                "max_session_age" to 604800000, // 7天
                "cache_size" to 100,
                "backup_enabled" to true,
                "backup_interval" to 86400000 // 24小时
            ),
            "privacy" to mapOf(
                "anonymize_data" to true,
                "exclude_passwords" to true,
                "exclude_sensitive_info" to true,
                "data_retention_days" to 30,
                "local_encryption" to true
            ),
            "ui" to mapOf(
                "theme" to "auto",
                "language" to "auto",
                "notifications_enabled" to true,
                "debug_mode" to false
            )
        )
    }
    
    // 配置状态
    private val _configLoaded = MutableStateFlow(false)
    val configLoaded: StateFlow<Boolean> = _configLoaded.asStateFlow()
    
    private val _config = MutableStateFlow(DEFAULT_CONFIG)
    val config: StateFlow<Map<String, Any>> = _config.asStateFlow()
    
    // 配置文件路径
    private lateinit var configFile: File
    
    // 上下文
    private lateinit var context: Context
    
    // 配置监听器
    private val configListeners = mutableListOf<ConfigListener>()
    
    /**
     * 初始化配置管理器
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
        configFile = File(context.filesDir, CONFIG_FILE_NAME)
        
        // 加载配置
        loadConfig()
        
        Log.i(TAG, "PluginConfigManager initialized")
    }
    
    /**
     * 获取配置值
     */
    fun <T> getValue(key: String, defaultValue: T): T {
        try {
            val keys = key.split(".")
            var current: Any? = _config.value
            
            for (k in keys) {
                when (current) {
                    is Map<*, *> -> current = current[k]
                    else -> return defaultValue
                }
            }
            
            @Suppress("UNCHECKED_CAST")
            return current as? T ?: defaultValue
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get config value: $key", e)
            return defaultValue
        }
    }
    
    /**
     * 设置配置值
     */
    fun setValue(key: String, value: Any): Boolean {
        try {
            val keys = key.split(".")
            val configMap = _config.value.toMutableMap()
            
            // 导航到目标位置
            var current: MutableMap<String, Any> = configMap
            for (k in keys.dropLast(1)) {
                current = when (val next = current[k]) {
                    is Map<*, *> -> next.toMutableMap() as MutableMap<String, Any>
                    else -> {
                        val newMap = mutableMapOf<String, Any>()
                        current[k] = newMap
                        newMap
                    }
                }
            }
            
            // 设置值
            current[keys.last()] = value
            _config.value = configMap
            
            // 通知监听器
            notifyConfigChanged(key, value)
            
            Log.d(TAG, "Config updated: $key = $value")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set config value: $key = $value", e)
            return false
        }
    }
    
    /**
     * 获取服务器配置
     */
    fun getServerConfig(): ServerConfig {
        return ServerConfig(
            url = getValue("server.url", "https://api.syncrime.com"),
            timeout = getValue("server.timeout", 30000),
            retryCount = getValue("server.retry_count", 3)
        )
    }
    
    /**
     * 获取同步配置
     */
    fun getSyncConfig(): SyncConfig {
        return SyncConfig(
            autoSync = getValue("sync.auto_sync", true),
            syncInterval = getValue("sync.sync_interval", 300),
            wifiOnly = getValue("sync.wifi_only", true),
            compression = getValue("sync.compression", true),
            encryption = getValue("sync.encryption", true)
        )
    }
    
    /**
     * 获取采集配置
     */
    fun getCaptureConfig(): CaptureConfig {
        return CaptureConfig(
            captureMode = getValue("capture.capture_mode", "smart"),
            sessionTimeout = getValue("capture.session_timeout", 300000L),
            minSessionLength = getValue("capture.min_session_length", 3),
            captureSensitiveData = getValue("capture.capture_sensitive_data", false),
            captureEmoji = getValue("capture.capture_emoji", true),
            captureGestures = getValue("capture.capture_gestures", true)
        )
    }
    
    /**
     * 获取分析配置
     */
    fun getAnalysisConfig(): AnalysisConfig {
        return AnalysisConfig(
            enablePatternAnalysis = getValue("analysis.enable_pattern_analysis", true),
            enableImportanceAnalysis = getValue("analysis.enable_importance_analysis", true),
            enableCategoryAnalysis = getValue("analysis.enable_category_analysis", true),
            minTextLength = getValue("analysis.min_text_length", 3),
            maxTextLength = getValue("analysis.max_text_length", 10000)
        )
    }
    
    /**
     * 获取存储配置
     */
    fun getStorageConfig(): StorageConfig {
        return StorageConfig(
            maxSessions = getValue("storage.max_sessions", 1000),
            maxSessionAge = getValue("storage.max_session_age", 604800000L),
            cacheSize = getValue("storage.cache_size", 100),
            backupEnabled = getValue("storage.backup_enabled", true),
            backupInterval = getValue("storage.backup_interval", 86400000L)
        )
    }
    
    /**
     * 获取隐私配置
     */
    fun getPrivacyConfig(): PrivacyConfig {
        return PrivacyConfig(
            anonymizeData = getValue("privacy.anonymize_data", true),
            excludePasswords = getValue("privacy.exclude_passwords", true),
            excludeSensitiveInfo = getValue("privacy.exclude_sensitive_info", true),
            dataRetentionDays = getValue("privacy.data_retention_days", 30),
            localEncryption = getValue("privacy.local_encryption", true)
        )
    }
    
    /**
     * 获取UI配置
     */
    fun getUiConfig(): UiConfig {
        return UiConfig(
            theme = getValue("ui.theme", "auto"),
            language = getValue("ui.language", "auto"),
            notificationsEnabled = getValue("ui.notifications_enabled", true),
            debugMode = getValue("ui.debug_mode", false)
        )
    }
    
    /**
     * 重置配置为默认值
     */
    fun resetToDefault(): Boolean {
        try {
            _config.value = DEFAULT_CONFIG
            saveConfig()
            notifyConfigReset()
            Log.i(TAG, "Configuration reset to default")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset configuration", e)
            return false
        }
    }
    
    /**
     * 验证配置
     */
    fun validateConfig(): ConfigValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            // 验证服务器配置
            val serverUrl = getValue("server.url", "")
            if (serverUrl.isBlank()) {
                errors.add("Server URL cannot be empty")
            } else if (!isValidUrl(serverUrl)) {
                errors.add("Invalid server URL format")
            }
            
            // 验证同步间隔
            val syncInterval = getValue("sync.sync_interval", 300)
            if (syncInterval < 60) {
                warnings.add("Sync interval too short, recommended minimum is 60 seconds")
            }
            
            // 验证存储配置
            val maxSessions = getValue("storage.max_sessions", 1000)
            if (maxSessions < 100) {
                warnings.add("Max sessions too low, may affect performance")
            }
            
            // 验证隐私配置
            val dataRetentionDays = getValue("privacy.data_retention_days", 30)
            if (dataRetentionDays < 1) {
                errors.add("Data retention days must be at least 1")
            }
            
        } catch (e: Exception) {
            errors.add("Configuration validation failed: ${e.message}")
        }
        
        return ConfigValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 导出配置
     */
    fun exportConfig(): String {
        try {
            return JSONObject(_config.value).toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export configuration", e)
            return ""
        }
    }
    
    /**
     * 导入配置
     */
    fun importConfig(configJson: String): Boolean {
        try {
            val json = JSONObject(configJson)
            val configMap = parseJsonToMap(json)
            
            // 验证导入的配置
            _config.value = configMap
            val validation = validateConfig()
            
            if (!validation.isValid) {
                Log.e(TAG, "Imported configuration is invalid: ${validation.errors}")
                _config.value = DEFAULT_CONFIG
                return false
            }
            
            saveConfig()
            notifyConfigImported()
            Log.i(TAG, "Configuration imported successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import configuration", e)
            return false
        }
    }
    
    /**
     * 添加配置监听器
     */
    fun addConfigListener(listener: ConfigListener) {
        configListeners.add(listener)
    }
    
    /**
     * 移除配置监听器
     */
    fun removeConfigListener(listener: ConfigListener) {
        configListeners.remove(listener)
    }
    
    /**
     * 销毁配置管理器
     */
    fun destroy() {
        saveConfig()
        configListeners.clear()
        Log.i(TAG, "PluginConfigManager destroyed")
    }
    
    // 私有方法
    
    private fun loadConfig() {
        try {
            if (configFile.exists()) {
                val content = FileInputStream(configFile).use { it.readBytes().toString(Charsets.UTF_8) }
                val json = JSONObject(content)
                val configMap = parseJsonToMap(json)
                
                // 合并默认配置和用户配置
                val mergedConfig = mergeConfigs(DEFAULT_CONFIG, configMap)
                _config.value = mergedConfig
                
                Log.i(TAG, "Configuration loaded from file")
            } else {
                _config.value = DEFAULT_CONFIG
                saveConfig()
                Log.i(TAG, "Default configuration created")
            }
            
            _configLoaded.value = true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load configuration, using defaults", e)
            _config.value = DEFAULT_CONFIG
            _configLoaded.value = true
        }
    }
    
    private fun saveConfig() {
        try {
            val configJson = JSONObject(_config.value).toString(2)
            FileOutputStream(configFile).use { 
                it.write(configJson.toByteArray(Charsets.UTF_8))
            }
            Log.d(TAG, "Configuration saved to file")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save configuration", e)
        }
    }
    
    private fun parseJsonToMap(json: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        
        json.keys().forEach { key ->
            val value = json.get(key)
            map[key] = when (value) {
                is JSONObject -> parseJsonToMap(value)
                else -> value
            }
        }
        
        return map
    }
    
    private fun mergeConfigs(default: Map<String, Any>, user: Map<String, Any>): Map<String, Any> {
        val merged = default.toMutableMap()
        
        user.forEach { (key, value) ->
            val defaultValue = merged[key]
            merged[key] = when {
                defaultValue is Map<*, *> && value is Map<*, *> -> {
                    mergeConfigs(defaultValue as Map<String, Any>, value as Map<String, Any>)
                }
                else -> value
            }
        }
        
        return merged
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            // 简单的URL验证
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }
    
    private fun notifyConfigChanged(key: String, value: Any) {
        configListeners.forEach { listener ->
            try {
                listener.onConfigChanged(key, value)
            } catch (e: Exception) {
                Log.e(TAG, "Config listener failed", e)
            }
        }
    }
    
    private fun notifyConfigReset() {
        configListeners.forEach { listener ->
            try {
                listener.onConfigReset()
            } catch (e: Exception) {
                Log.e(TAG, "Config listener failed", e)
            }
        }
    }
    
    private fun notifyConfigImported() {
        configListeners.forEach { listener ->
            try {
                listener.onConfigImported()
            } catch (e: Exception) {
                Log.e(TAG, "Config listener failed", e)
            }
        }
    }
    
    // 配置数据类
    
    data class ServerConfig(
        val url: String,
        val timeout: Int,
        val retryCount: Int
    )
    
    data class SyncConfig(
        val autoSync: Boolean,
        val syncInterval: Int,
        val wifiOnly: Boolean,
        val compression: Boolean,
        val encryption: Boolean
    )
    
    data class CaptureConfig(
        val captureMode: String,
        val sessionTimeout: Long,
        val minSessionLength: Int,
        val captureSensitiveData: Boolean,
        val captureEmoji: Boolean,
        val captureGestures: Boolean
    )
    
    data class AnalysisConfig(
        val enablePatternAnalysis: Boolean,
        val enableImportanceAnalysis: Boolean,
        val enableCategoryAnalysis: Boolean,
        val minTextLength: Int,
        val maxTextLength: Int
    )
    
    data class StorageConfig(
        val maxSessions: Int,
        val maxSessionAge: Long,
        val cacheSize: Int,
        val backupEnabled: Boolean,
        val backupInterval: Long
    )
    
    data class PrivacyConfig(
        val anonymizeData: Boolean,
        val excludePasswords: Boolean,
        val excludeSensitiveInfo: Boolean,
        val dataRetentionDays: Int,
        val localEncryption: Boolean
    )
    
    data class UiConfig(
        val theme: String,
        val language: String,
        val notificationsEnabled: Boolean,
        val debugMode: Boolean
    )
    
    data class ConfigValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>
    )
    
    // 配置监听器接口
    interface ConfigListener {
        fun onConfigChanged(key: String, value: Any) {}
        fun onConfigReset() {}
        fun onConfigImported() {}
    }
}