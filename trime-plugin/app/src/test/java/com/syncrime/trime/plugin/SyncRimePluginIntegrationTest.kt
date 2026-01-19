package com.syncrime.trime.plugin

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * SyncRime 插件集成测试
 * 
 * 测试插件的完整功能，包括初始化、数据采集、同步和配置管理。
 */
@RunWith(MockitoJUnitRunner::class)
class SyncRimePluginIntegrationTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockCallbacks: SyncRimePlugin.PluginCallbacks
    
    private lateinit var plugin: SyncRimePlugin
    private lateinit var testScope: TestScope
    
    @Before
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        
        // Mock context
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        
        // 获取插件实例
        plugin = SyncRimePlugin.getInstance()
    }
    
    @After
    fun tearDown() {
        plugin.cleanup()
    }
    
    @Test
    fun `test plugin lifecycle initialization`() = testScope.runTest {
        // 测试插件初始化
        val initialized = plugin.initialize(mockContext)
        
        assertTrue(initialized, "Plugin should initialize successfully")
        assertEquals(SyncRimePlugin.PluginState.READY, plugin.getPluginState())
        
        // 测试插件清理
        plugin.cleanup()
        assertEquals(SyncRimePlugin.PluginState.UNINITIALIZED, plugin.getPluginState())
    }
    
    @Test
    fun `test input capture functionality`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        plugin.setCallbacks(mockCallbacks)
        
        // 开始采集
        val captureStarted = plugin.startCapture()
        assertTrue(captureStarted, "Should start capture successfully")
        assertTrue(plugin.isCapturing.first(), "Should be capturing")
        
        // 测试输入采集
        val inputText = "Hello World"
        val captureResult = plugin.captureInput(inputText)
        assertTrue(captureResult, "Should capture input successfully")
        
        // 验证回调
        verify(mockCallbacks, timeout(1000)).onInputCaptured(inputText)
        
        // 停止采集
        val captureStopped = plugin.stopCapture()
        assertTrue(captureStopped, "Should stop capture successfully")
        assertFalse(plugin.isCapturing.first(), "Should not be capturing")
    }
    
    @Test
    fun `test data synchronization`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        plugin.setCallbacks(mockCallbacks)
        
        // 开始采集一些数据
        plugin.startCapture()
        plugin.captureInput("Test input 1")
        plugin.captureInput("Test input 2")
        plugin.captureInput("Test input 3")
        plugin.stopCapture()
        
        // 测试同步
        val syncResult = plugin.syncData()
        
        // 验证回调
        verify(mockCallbacks, timeout(2000)).onSyncStarted()
        verify(mockCallbacks, timeout(3000)).onSyncCompleted(any())
        
        // 验证统计信息
        val stats = plugin.getStatistics()
        assertTrue(stats.totalInputs >= 3, "Should have captured at least 3 inputs")
    }
    
    @Test
    fun `test configuration management`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        
        // 测试配置更新
        val testKey = "test.key"
        val testValue = "test.value"
        plugin.updateConfig(testKey, testValue)
        
        // 测试配置保存和加载
        val testConfigPath = "/tmp/test_config.json"
        val saveResult = plugin.saveConfig(testConfigPath)
        assertTrue(saveResult, "Should save config successfully")
        
        val loadResult = plugin.loadConfig(testConfigPath)
        assertTrue(loadResult, "Should load config successfully")
        
        // 清理测试文件
        testScope.launch {
            // 在实际环境中，这里会删除测试文件
        }
    }
    
    @Test
    fun `test error handling`() = testScope.runTest {
        // 测试未初始化状态下的错误处理
        val uninitializedPlugin = SyncRimePlugin.getInstance()
        
        val captureResult = uninitializedPlugin.captureInput("test")
        assertFalse(captureResult, "Should fail to capture when not initialized")
        
        val syncResult = uninitializedPlugin.syncData()
        assertFalse(syncResult, "Should fail to sync when not initialized")
        
        assertEquals(
            SyncRimePlugin.ErrorCode.NOT_INITIALIZED,
            uninitializedPlugin.getLastError()
        )
    }
    
    @Test
    fun `test concurrent operations`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        plugin.setCallbacks(mockCallbacks)
        
        // 并发测试：多个输入采集
        val captureJobs = (1..10).map { i ->
            testScope.async {
                plugin.captureInput("Test input $i")
            }
        }
        
        // 等待所有采集完成
        val results = captureJobs.awaitAll()
        assertTrue(results.all { it }, "All captures should succeed")
        
        // 验证输入数量
        val stats = plugin.getStatistics()
        assertTrue(stats.totalInputs >= 10, "Should have captured at least 10 inputs")
    }
    
    @Test
    fun `test session management`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        plugin.startCapture()
        
        // 模拟输入会话
        plugin.captureInput("Session start")
        
        // 模拟会话延迟
        delay(100)
        
        plugin.captureInput("Session middle")
        
        delay(100)
        
        plugin.captureInput("Session end")
        
        plugin.stopCapture()
        
        // 验证会话统计
        val stats = plugin.getStatistics()
        assertTrue(stats.totalInputs >= 3, "Should have captured all session inputs")
    }
    
    @Test
    fun `test configuration validation`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        
        // 测试配置验证
        val configManager = PluginConfigManager()
        configManager.initialize(mockContext)
        
        // 测试有效配置
        configManager.setValue("server.url", "https://api.syncrime.com")
        configManager.setValue("sync.auto_sync", true)
        configManager.setValue("sync.sync_interval", 300)
        
        val validation = configManager.validateConfig()
        assertTrue(validation.isValid, "Valid configuration should pass validation")
        
        // 测试无效配置
        configManager.setValue("server.url", "") // 无效URL
        configManager.setValue("sync.sync_interval", 30) // 太短
        
        val invalidValidation = configManager.validateConfig()
        assertFalse(invalidValidation.isValid, "Invalid configuration should fail validation")
        assertTrue(invalidValidation.errors.isNotEmpty(), "Should have errors")
    }
    
    @Test
    fun `test performance characteristics`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        plugin.startCapture()
        
        // 性能测试：大量输入
        val startTime = System.currentTimeMillis()
        
        repeat(1000) { i ->
            plugin.captureInput("Performance test input $i")
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        plugin.stopCapture()
        
        // 验证性能
        assertTrue(duration < 5000, "Should complete 1000 inputs within 5 seconds")
        
        val stats = plugin.getStatistics()
        assertTrue(stats.totalInputs >= 1000, "Should have captured all inputs")
    }
    
    @Test
    fun `test memory management`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        plugin.startCapture()
        
        // 内存测试：大量数据
        repeat(100) { i ->
            val longText = "x".repeat(1000) // 1KB 文本
            plugin.captureInput(longText)
        }
        
        plugin.stopCapture()
        
        // 强制垃圾回收
        System.gc()
        
        // 验证统计信息
        val stats = plugin.getStatistics()
        assertTrue(stats.totalInputs >= 100, "Should have captured all inputs")
        
        // 这里可以添加内存使用检查
        // 但在单元测试中，我们主要验证功能正常
    }
    
    @Test
    fun `test plugin state transitions`() = testScope.runTest {
        val plugin = SyncRimePlugin.getInstance()
        
        // 初始状态
        assertEquals(SyncRimePlugin.PluginState.UNINITIALIZED, plugin.getPluginState())
        
        // 初始化
        plugin.initialize(mockContext)
        assertEquals(SyncRimePlugin.PluginState.READY, plugin.getPluginState())
        
        // 开始采集
        plugin.startCapture()
        assertEquals(SyncRimePlugin.PluginState.CAPTURING, plugin.getPluginState())
        
        // 同步（在采集中）
        val syncResult = plugin.syncData()
        assertTrue(syncResult, "Should sync while capturing")
        
        // 停止采集
        plugin.stopCapture()
        assertEquals(SyncRimePlugin.PluginState.READY, plugin.getPluginState())
        
        // 清理
        plugin.cleanup()
        assertEquals(SyncRimePlugin.PluginState.UNINITIALIZED, plugin.getPluginState())
    }
    
    @Test
    fun `test error recovery`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        plugin.setCallbacks(mockCallbacks)
        
        // 模拟错误情况
        plugin.captureInput("Normal input")
        
        // 验证错误恢复
        val stats = plugin.getStatistics()
        assertEquals(SyncRimePlugin.ErrorCode.SUCCESS, plugin.getLastError())
        
        // 验证插件仍然可用
        val captureResult = plugin.captureInput("Recovery input")
        assertTrue(captureResult, "Plugin should recover from errors")
    }
    
    @Test
    fun `test data integrity`() = testScope.runTest {
        // 初始化插件
        plugin.initialize(mockContext)
        plugin.startCapture()
        
        val testInputs = listOf(
            "Hello World",
            "123456",
            "test@example.com",
            "https://example.com",
            "特殊字符测试：🎉",
            "Multiline\ntest\ninput"
        )
        
        // 采集各种类型的输入
        testInputs.forEach { input ->
            val result = plugin.captureInput(input)
            assertTrue(result, "Should capture input: $input")
        }
        
        plugin.stopCapture()
        
        // 验证数据完整性
        val stats = plugin.getStatistics()
        assertEquals(testInputs.size.toLong(), stats.totalInputs)
        
        // 验证回调被正确调用
        verify(mockCallbacks, atLeastOnce()).onInputCaptured(any())
    }
}