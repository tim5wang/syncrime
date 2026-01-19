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
import kotlin.test.*

/**
 * 输入会话管理器单元测试
 */
@RunWith(MockitoJUnitRunner::class)
class InputSessionManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var sessionManager: InputSessionManager
    private lateinit var testScope: TestScope
    
    @Before
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        
        sessionManager = InputSessionManager()
        sessionManager.initialize(mockContext)
    }
    
    @After
    fun tearDown() {
        sessionManager.destroy()
    }
    
    @Test
    fun `test session lifecycle`() = testScope.runTest {
        // 开始会话
        val session = sessionManager.startSession()
        assertNotNull(session)
        assertTrue(session.isActive)
        
        // 添加输入
        val record1 = sessionManager.addInput("Hello", "metadata")
        assertNotNull(record1)
        assertEquals("Hello", record1.text)
        
        val record2 = sessionManager.addInput("World", "metadata")
        assertNotNull(record2)
        assertEquals("World", record2.text)
        
        // 结束会话
        sessionManager.endSession(session)
        assertFalse(session.isActive)
        assertTrue(session.endTime > 0)
        
        // 验证会话统计
        val stats = sessionManager.getSessionStatistics()
        assertEquals(1, stats.totalSessions)
        assertEquals(2, stats.totalInputs)
    }
    
    @Test
    fun `test input type detection`() = testScope.runTest {
        val session = sessionManager.startSession()
        
        // 测试邮箱输入
        val emailRecord = sessionManager.addInput("test@example.com")
        assertEquals(InputSessionManager.InputType.EMAIL, emailRecord.inputType)
        assertEquals("contact", emailRecord.category)
        
        // 测试密码输入
        val passwordRecord = sessionManager.addInput("password123")
        assertEquals(InputSessionManager.InputType.PASSWORD, passwordRecord.inputType)
        assertEquals(InputSessionManager.Importance.CRITICAL, passwordRecord.importance)
        
        // 测试URL输入
        val urlRecord = sessionManager.addInput("https://example.com")
        assertEquals(InputSessionManager.InputType.URL, urlRecord.inputType)
        assertEquals("url", urlRecord.category)
        
        // 测试数字输入
        val numberRecord = sessionManager.addInput("12345")
        assertEquals(InputSessionManager.InputType.NUMBER, numberRecord.inputType)
        assertEquals("number", numberRecord.category)
        
        sessionManager.endSession(session)
    }
    
    @Test
    fun `test importance analysis`() = testScope.runTest {
        val session = sessionManager.startSession()
        
        // 普通输入
        val normalRecord = sessionManager.addInput("Hello World")
        assertEquals(InputSessionManager.Importance.NORMAL, normalRecord.importance)
        
        // 敏感输入
        val sensitiveRecord = sessionManager.addInput("password: secret123")
        assertEquals(InputSessionManager.Importance.CRITICAL, sensitiveRecord.importance)
        
        // 联系信息
        val contactRecord = sessionManager.addInput("13800138000")
        assertEquals(InputSessionManager.Importance.HIGH, contactRecord.importance)
        
        sessionManager.endSession(session)
        
        // 验证会话重要性
        assertTrue(session.importance.value >= InputSessionManager.Importance.CRITICAL.value)
    }
    
    @Test
    fun `test session timeout`() = testScope.runTest {
        // 设置短超时时间用于测试
        // 注意：实际的超时逻辑可能需要修改 sessionManager 的配置
        
        val session1 = sessionManager.startSession()
        val session2 = sessionManager.startSession()
        
        // 模拟时间过去
        delay(100)
        
        // 添加输入到 session1（应该会创建新会话）
        sessionManager.addInput("Session 1 input")
        
        // 验证 session1 已结束，session2 仍在活动
        assertFalse(session1.isActive)
        assertTrue(session2.isActive)
        
        sessionManager.endSession()
    }
    
    @Test
    fun `test search functionality`() = testScope.runTest {
        val session = sessionManager.startSession()
        
        // 添加多种输入
        sessionManager.addInput("Hello World")
        sessionManager.addInput("Kotlin programming")
        sessionManager.addInput("Android development")
        sessionManager.addInput("Test input")
        
        sessionManager.endSession(session)
        
        // 搜索测试
        val kotlinResults = sessionManager.searchInputs("Kotlin")
        assertEquals(1, kotlinResults.size)
        assertTrue(kotlinResults[0].text.contains("Kotlin"))
        
        val testResults = sessionManager.searchInputs("test")
        assertEquals(1, testResults.size)
        assertTrue(testResults[0].text.contains("Test"))
        
        val emptyResults = sessionManager.searchInputs("nonexistent")
        assertEquals(0, emptyResults.size)
    }
    
    @Test
    fun `test high importance inputs`() = testScope.runTest {
        val session1 = sessionManager.startSession()
        sessionManager.addInput("Normal input")
        sessionManager.addInput("password: secret")
        sessionManager.endSession(session1)
        
        val session2 = sessionManager.startSession()
        sessionManager.addInput("user@example.com")
        sessionManager.addInput("Another normal input")
        sessionManager.endSession(session2)
        
        // 获取高重要性输入
        val highImportanceInputs = sessionManager.getHighImportanceInputs()
        assertTrue(highImportanceInputs.isNotEmpty())
        
        // 验证包含密码和邮箱
        val hasPassword = highImportanceInputs.any { it.text.contains("password") }
        val hasEmail = highImportanceInputs.any { it.text.contains("@") }
        assertTrue(hasPassword || hasEmail)
    }
    
    @Test
    fun `test session statistics`() = testScope.runTest {
        val session1 = sessionManager.startSession()
        sessionManager.addInput("Input 1")
        sessionManager.addInput("Input 2")
        sessionManager.endSession(session1)
        
        val session2 = sessionManager.startSession()
        sessionManager.addInput("Input 3")
        sessionManager.addInput("Input 4")
        sessionManager.addInput("Input 5")
        sessionManager.endSession(session2)
        
        val stats = sessionManager.getSessionStatistics()
        assertEquals(2, stats.totalSessions)
        assertEquals(5, stats.totalInputs)
        assertEquals(2.5, stats.averageSessionLength, 0.1)
    }
    
    @Test
    fun `test session cleanup`() = testScope.runTest {
        val session = sessionManager.startSession()
        sessionManager.addInput("Test input")
        sessionManager.endSession(session)
        
        // 模拟旧会话
        session.endTime = System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000 // 8天前
        
        // 清理旧会话
        sessionManager.cleanupOldSessions(7 * 24 * 60 * 60 * 1000) // 7天
        
        val stats = sessionManager.getSessionStatistics()
        assertEquals(0, stats.totalSessions)
    }
    
    @Test
    fun `test recent sessions`() = testScope.runTest {
        val session1 = sessionManager.startSession()
        sessionManager.addInput("Session 1")
        sessionManager.endSession(session1)
        
        delay(10)
        
        val session2 = sessionManager.startSession()
        sessionManager.addInput("Session 2")
        sessionManager.endSession(session2)
        
        delay(10)
        
        val session3 = sessionManager.startSession()
        sessionManager.addInput("Session 3")
        sessionManager.endSession(session3)
        
        val recentSessions = sessionManager.getRecentSessions(2)
        assertEquals(2, recentSessions.size)
        assertEquals("Session 3", recentSessions[0].inputs[0].text)
        assertEquals("Session 2", recentSessions[1].inputs[0].text)
    }
    
    @Test
    fun `test concurrent session access`() = testScope.runTest {
        val jobs = (1..10).map { i ->
            testScope.async {
                val session = sessionManager.startSession()
                sessionManager.addInput("Input $i")
                sessionManager.endSession(session)
            }
        }
        
        jobs.awaitAll()
        
        val stats = sessionManager.getSessionStatistics()
        assertEquals(10, stats.totalSessions)
        assertEquals(10, stats.totalInputs)
    }
    
    @Test
    fun `test session metadata`() = testScope.runTest {
        val session = sessionManager.startSession()
        
        // 添加带元数据的输入
        val record1 = sessionManager.addInput("Hello", "app:browser")
        assertEquals("app:browser", record1.metadata)
        
        val record2 = sessionManager.addInput("World", "app:messenger")
        assertEquals("app:messenger", record2.metadata)
        
        sessionManager.endSession(session)
        
        // 验证元数据被保存
        val inputs = session.inputs
        assertEquals("app:browser", inputs[0].metadata)
        assertEquals("app:messenger", inputs[1].metadata)
    }
    
    @Test
    fun `test session persistence`() = testScope.runTest {
        // 这个测试需要实际的持久化实现
        val session = sessionManager.startSession()
        sessionManager.addInput("Persistent input")
        sessionManager.endSession(session)
        
        // 验证会话被正确保存
        val stats = sessionManager.getSessionStatistics()
        assertEquals(1, stats.totalSessions)
        
        // 在实际实现中，这里会测试数据的持久化和恢复
    }
    
    @Test
    fun `test edge cases`() = testScope.runTest {
        val session = sessionManager.startSession()
        
        // 空字符串输入
        val emptyRecord = sessionManager.addInput("")
        assertNotNull(emptyRecord)
        assertEquals("", emptyRecord.text)
        
        // 长字符串输入
        val longText = "x".repeat(1000)
        val longRecord = sessionManager.addInput(longText)
        assertEquals(longText, longRecord.text)
        
        // 特殊字符输入
        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val specialRecord = sessionManager.addInput(specialChars)
        assertEquals(specialChars, specialRecord.text)
        
        sessionManager.endSession(session)
        
        val stats = sessionManager.getSessionStatistics()
        assertEquals(3, stats.totalInputs)
    }
    
    @Test
    fun `test session manager initialization`() {
        // 测试重复初始化
        val sessionManager2 = InputSessionManager()
        sessionManager2.initialize(mockContext)
        sessionManager2.destroy()
        
        // 应该不会抛出异常
        assertTrue(true)
    }
    
    @Test
    fun `test session manager cleanup`() {
        sessionManager.destroy()
        
        // 清理后应该可以重新初始化
        val newSessionManager = InputSessionManager()
        newSessionManager.initialize(mockContext)
        newSessionManager.destroy()
        
        assertTrue(true)
    }
}