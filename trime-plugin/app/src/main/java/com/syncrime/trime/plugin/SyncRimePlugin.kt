package com.syncrime.trime.plugin

import android.content.Context
import android.util.Log
import com.osfans.trime.core.Trime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * SyncRime Trime 插件主类
 * 
 * 负责协调输入内容采集、数据同步和配置管理等功能。
 * 提供统一的插件接口供 Trime 输入法调用。
 */
class SyncRimePlugin private constructor() {
    
    companion object {
        private const val TAG = "SyncRimePlugin"
        
        @Volatile
        private var INSTANCE: SyncRimePlugin? = null
        
        fun getInstance(): SyncRimePlugin {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncRimePlugin().also { INSTANCE = it }
            }
        }
        
        // 加载原生库
        init {
            System.loadLibrary("syncrime_plugin")
        }
    }
    
    // 原生实例指针
    private var nativeInstance: Long = 0
    
    // 插件状态
    enum class PluginState(val value: Int) {
        UNINITIALIZED(0),
        INITIALIZING(1),
        READY(2),
        CAPTURING(3),
        SYNCING(4),
        ERROR(5);
        
        companion object {
            fun fromValue(value: Int): PluginState {
                return values().find { it.value == value } ?: UNINITIALIZED
            }
        }
    }
    
    // 错误代码
    enum class ErrorCode(val value: Int) {
        SUCCESS(0),
        INITIALIZATION_FAILED(1),
        ALREADY_INITIALIZED(2),
        NOT_INITIALIZED(3),
        CAPTURE_ALREADY_STARTED(4),
        CAPTURE_NOT_STARTED(5),
        SYNC_IN_PROGRESS(6),
        CONFIG_LOAD_FAILED(7),
        CONFIG_SAVE_FAILED(8),
        NETWORK_ERROR(9),
        PERMISSION_DENIED(10),
        UNKNOWN_ERROR(99);
        
        companion object {
            fun fromValue(value: Int): ErrorCode {
                return values().find { it.value == value } ?: UNKNOWN_ERROR
            }
        }
    }
    
    // 状态流
    private val _pluginState = MutableStateFlow(PluginState.UNINITIALIZED)
    val pluginState: StateFlow<PluginState> = _pluginState.asStateFlow()
    
    private val _lastError = MutableStateFlow(ErrorCode.SUCCESS)
    val lastError: StateFlow<ErrorCode> = _lastError.asStateFlow()
    
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()
    
    // 统计信息
    data class Statistics(
        val totalInputs: Long = 0,
        val totalSyncs: Long = 0,
        val successfulSyncs: Long = 0,
        val failedSyncs: Long = 0,
        val lastSyncTime: Long = 0,
        val averageSyncTime: Double = 0.0
    )
    
    // 回调接口
    interface PluginCallbacks {
        fun onInputCaptured(text: String) {}
        fun onSyncStarted() {}
        fun onSyncCompleted(success: Boolean) {}
        fun onError(error: ErrorCode, message: String) {}
    }
    
    private var callbacks: PluginCallbacks? = null
    
    // 协程作用域
    private val pluginScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 输入会话管理
    private val inputSessionManager = InputSessionManager()
    
    // 同步管理器
    private val syncManager = SyncManager()
    
    // 配置管理器
    private val configManager = PluginConfigManager()
    
    /**
     * 初始化插件
     */
    fun initialize(context: Context): Boolean {
        return try {
            if (nativeInstance != 0L) {
                Log.w(TAG, "Plugin already initialized")
                return true
            }
            
            _pluginState.value = PluginState.INITIALIZING
            
            // 初始化原生层
            nativeInstance = nativeInitialize(context)
            
            if (nativeInstance == 0L) {
                Log.e(TAG, "Failed to initialize native plugin")
                _pluginState.value = PluginState.ERROR
                return false
            }
            
            // 设置回调
            setJniCallbacks()
            
            // 初始化组件
            initializeComponents(context)
            
            _pluginState.value = PluginState.READY
            Log.i(TAG, "Plugin initialized successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize plugin", e)
            _pluginState.value = PluginState.ERROR
            _lastError.value = ErrorCode.INITIALIZATION_FAILED
            false
        }
    }
    
    /**
     * 清理插件
     */
    fun cleanup() {
        try {
            pluginScope.cancel()
            
            if (nativeInstance != 0L) {
                nativeCleanup(nativeInstance)
                nativeInstance = 0L
            }
            
            _pluginState.value = PluginState.UNINITIALIZED
            Log.i(TAG, "Plugin cleaned up")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup plugin", e)
        }
    }
    
    /**
     * 开始采集输入
     */
    fun startCapture(): Boolean {
        return if (nativeInstance != 0L) {
            val result = nativeStartCapture(nativeInstance)
            if (result) {
                _isCapturing.value = true
                _pluginState.value = PluginState.CAPTURING
                inputSessionManager.startSession()
            }
            result
        } else {
            Log.e(TAG, "Plugin not initialized")
            false
        }
    }
    
    /**
     * 停止采集输入
     */
    fun stopCapture(): Boolean {
        return if (nativeInstance != 0L) {
            val result = nativeStopCapture(nativeInstance)
            if (result) {
                _isCapturing.value = false
                _pluginState.value = PluginState.READY
                inputSessionManager.endSession()
            }
            result
        } else {
            Log.e(TAG, "Plugin not initialized")
            false
        }
    }
    
    /**
     * 同步数据
     */
    fun syncData(): Boolean {
        return if (nativeInstance != 0L) {
            _pluginState.value = PluginState.SYNCING
            val result = nativeSyncData(nativeInstance)
            _pluginState.value = PluginState.READY
            result
        } else {
            Log.e(TAG, "Plugin not initialized")
            false
        }
    }
    
    /**
     * 采集输入文本
     */
    fun captureInput(text: String, metadata: String = ""): Boolean {
        return if (nativeInstance != 0L && _isCapturing.value) {
            // 记录到输入会话
            inputSessionManager.addInput(text, metadata)
            
            // 调用原生层
            nativeCaptureInput(nativeInstance, text, metadata)
        } else {
            false
        }
    }
    
    /**
     * 采集按键事件
     */
    fun captureKeyEvent(keycode: Int, action: Int): Boolean {
        return if (nativeInstance != 0L && _isCapturing.value) {
            nativeCaptureKeyEvent(nativeInstance, keycode, action)
        } else {
            false
        }
    }
    
    /**
     * 采集手势事件
     */
    fun captureGesture(gestureType: Int, points: FloatArray): Boolean {
        return if (nativeInstance != 0L && _isCapturing.value) {
            nativeCaptureGesture(nativeInstance, gestureType, points)
        } else {
            false
        }
    }
    
    /**
     * 启用自动同步
     */
    fun enableAutoSync(enable: Boolean): Boolean {
        return if (nativeInstance != 0L) {
            nativeEnableAutoSync(nativeInstance, enable)
        } else {
            false
        }
    }
    
    /**
     * 设置同步间隔
     */
    fun setSyncInterval(seconds: Int): Boolean {
        return if (nativeInstance != 0L) {
            nativeSetSyncInterval(nativeInstance, seconds)
        } else {
            false
        }
    }
    
    /**
     * 获取同步间隔
     */
    fun getSyncInterval(): Int {
        return if (nativeInstance != 0L) {
            nativeGetSyncInterval(nativeInstance)
        } else {
            -1
        }
    }
    
    /**
     * 强制同步
     */
    fun forceSync(): Boolean {
        return syncData()
    }
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): Statistics {
        return if (nativeInstance != 0L) {
            nativeGetStatistics(nativeInstance)
        } else {
            Statistics()
        }
    }
    
    /**
     * 重置统计信息
     */
    fun resetStatistics() {
        if (nativeInstance != 0L) {
            nativeResetStatistics(nativeInstance)
        }
    }
    
    /**
     * 加载配置
     */
    fun loadConfig(configPath: String): Boolean {
        return if (nativeInstance != 0L) {
            nativeLoadConfig(nativeInstance, configPath)
        } else {
            false
        }
    }
    
    /**
     * 保存配置
     */
    fun saveConfig(configPath: String): Boolean {
        return if (nativeInstance != 0L) {
            nativeSaveConfig(nativeInstance, configPath)
        } else {
            false
        }
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(key: String, value: String) {
        if (nativeInstance != 0L) {
            nativeUpdateConfig(nativeInstance, key, value)
        }
    }
    
    /**
     * 设置回调
     */
    fun setCallbacks(callbacks: PluginCallbacks) {
        this.callbacks = callbacks
    }
    
    /**
     * 获取插件状态
     */
    fun getPluginState(): PluginState {
        return if (nativeInstance != 0L) {
            PluginState.fromValue(nativeGetState(nativeInstance))
        } else {
            PluginState.UNINITIALIZED
        }
    }
    
    /**
     * 获取最后的错误
     */
    fun getLastError(): ErrorCode {
        return if (nativeInstance != 0L) {
            ErrorCode.fromValue(nativeGetLastError(nativeInstance))
        } else {
            ErrorCode.NOT_INITIALIZED
        }
    }
    
    // 私有方法
    
    private fun initializeComponents(context: Context) {
        try {
            // 初始化配置管理器
            configManager.initialize(context)
            
            // 初始化同步管理器
            syncManager.initialize(context)
            
            // 初始化输入会话管理器
            inputSessionManager.initialize(context)
            
            Log.i(TAG, "Components initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize components", e)
            throw e
        }
    }
    
    private fun setJniCallbacks() {
        if (nativeInstance != 0L) {
            nativeSetJniCallbacks(nativeInstance, JniCallbacks())
        }
    }
    
    // JNI 回调实现
    private inner class JniCallbacks : PluginCallbacks {
        override fun onInputCaptured(text: String) {
            pluginScope.launch {
                callbacks?.onInputCaptured(text)
                Log.d(TAG, "Input captured: $text")
            }
        }
        
        override fun onSyncStarted() {
            pluginScope.launch {
                callbacks?.onSyncStarted()
                Log.d(TAG, "Sync started")
            }
        }
        
        override fun onSyncCompleted(success: Boolean) {
            pluginScope.launch {
                callbacks?.onSyncCompleted(success)
                Log.d(TAG, "Sync completed: $success")
            }
        }
        
        override fun onError(error: ErrorCode, message: String) {
            pluginScope.launch {
                _lastError.value = error
                callbacks?.onError(error, message)
                Log.e(TAG, "Error: $error - $message")
            }
        }
    }
    
    // 原生方法声明
    private external fun nativeInitialize(context: Context): Long
    private external fun nativeCleanup(instance: Long)
    private external fun nativeStartCapture(instance: Long): Boolean
    private external fun nativeStopCapture(instance: Long): Boolean
    private external fun nativeSyncData(instance: Long): Boolean
    private external fun nativeGetState(instance: Long): Int
    private external fun nativeGetLastError(instance: Long): Int
    private external fun nativeLoadConfig(instance: Long, configPath: String): Boolean
    private external fun nativeSaveConfig(instance: Long, configPath: String): Boolean
    private external fun nativeUpdateConfig(instance: Long, key: String, value: String)
    private external fun nativeCaptureInput(instance: Long, text: String, metadata: String): Boolean
    private external fun nativeCaptureKeyEvent(instance: Long, keycode: Int, action: Int): Boolean
    private external fun nativeCaptureGesture(instance: Long, gestureType: Int, points: FloatArray): Boolean
    private external fun nativeEnableAutoSync(instance: Long, enable: Boolean): Boolean
    private external fun nativeSetSyncInterval(instance: Long, seconds: Int): Boolean
    private external fun nativeGetSyncInterval(instance: Long): Int
    private external fun nativeForceSync(instance: Long): Boolean
    private external fun nativeGetStatistics(instance: Long): Statistics
    private external fun nativeResetStatistics(instance: Long)
    private external fun nativeSetJniCallbacks(instance: Long, callbacks: PluginCallbacks)
}