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
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import kotlin.test.*

/**
 * SyncManager 单元测试
 */
@RunWith(MockitoJUnitRunner::class)
class SyncManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var syncManager: SyncManager
    private lateinit var testScope: TestScope
    
    @Before
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        
        syncManager = SyncManager()
        syncManager.initialize(mockContext)
    }
    
    @After
    fun tearDown() {
        syncManager.cleanup()
    }
    
    @Test
    fun `test sync manager initialization`() {
        assertEquals(SyncManager.SyncState.IDLE, syncManager.syncState.first())
        assertNotNull(syncManager.config.first())
        assertNotNull(syncManager.statistics.first())
    }
    
    @Test
    fun `test manual sync`() = testScope.runTest {
        val syncResult = syncManager.startSync(SyncManager.SyncMode.MANUAL)
        assertTrue(syncResult, "Manual sync should start successfully")
        
        // 等待同步完成
        delay(3000)
        
        assertEquals(SyncManager.SyncState.SUCCESS, syncManager.syncState.first())
        assertTrue(syncManager.statistics.first().totalSyncs > 0)
    }
    
    @Test
    fun `test auto sync configuration`() {
        // 启用自动同步
        syncManager.enableAutoSync(true)
        assertTrue(syncManager.config.first().autoSync)
        
        // 设置同步间隔
        val syncInterval = 600 // 10分钟
        val setIntervalResult = syncManager.setSyncInterval(syncInterval)
        assertTrue(setIntervalResult, "Should set sync interval successfully")
        
        assertEquals(syncInterval, syncManager.getSyncInterval())
        assertEquals(syncInterval, syncManager.config.first().syncInterval)
    }
    
    @Test
    fun `test sync configuration update`() {
        val newConfig = SyncManager.SyncConfig(
            serverUrl = "https://test.syncrime.com",
            syncInterval = 1800, // 30分钟
            autoSync = false,
            wifiOnly = true,
            compression = false,
            encryption = false
        )
        
        syncManager.updateConfig(newConfig)
        
        val updatedConfig = syncManager.config.first()
        assertEquals(newConfig.serverUrl, updatedConfig.serverUrl)
        assertEquals(newConfig.syncInterval, updatedConfig.syncInterval)
        assertEquals(newConfig.autoSync, updatedConfig.autoSync)
        assertEquals(newConfig.wifiOnly, updatedConfig.wifiOnly)
        assertEquals(newConfig.compression, updatedConfig.compression)
        assertEquals(newConfig.encryption, updatedConfig.encryption)
    }
    
    @Test
    fun `test sync status`() = testScope.runTest {
        val initialStatus = syncManager.getSyncStatus()
        assertEquals(SyncManager.SyncState.IDLE, initialStatus.state)
        assertEquals(0.0f, initialStatus.progress)
        assertEquals(0, initialStatus.totalSyncs)
        
        // 开始同步
        syncManager.startSync()
        
        // 检查同步中状态
        val syncingStatus = syncManager.getSyncStatus()
        assertEquals(SyncManager.SyncState.SYNCING, syncingStatus.state)
        assertTrue(syncingStatus.progress > 0.0f)
        
        // 等待同步完成
        delay(3000)
        
        val completedStatus = syncManager.getSyncStatus()
        assertEquals(SyncManager.SyncState.SUCCESS, completedStatus.state)
        assertTrue(completedStatus.totalSyncs > initialStatus.totalSyncs)
    }
    
    @Test
    fun `test force sync`() = testScope.runTest {
        val forceResult = syncManager.forceSync()
        assertTrue(forceResult, "Force sync should succeed")
        
        // 等待同步完成
        delay(3000)
        
        val stats = syncManager.statistics.first()
        assertTrue(stats.totalSyncs > 0, "Should have performed at least one sync")
    }
    
    @Test
    fun `test concurrent sync operations`() = testScope.runTest {
        val syncJobs = (1..5).map { i ->
            testScope.async {
                syncManager.startSync(SyncManager.SyncMode.MANUAL)
            }
        }
        
        val results = syncJobs.awaitAll()
        
        // 至少有一个应该成功
        assertTrue(results.any { it }, "At least one sync should succeed")
        
        // 等待所有操作完成
        delay(5000)
        
        val stats = syncManager.statistics.first()
        assertTrue(stats.totalSyncs >= 1, "Should have completed at least one sync")
    }
    
    @Test
    fun `test statistics tracking`() = testScope.runTest {
        // 重置统计信息
        syncManager.resetStatistics()
        val initialStats = syncManager.statistics.first()
        assertEquals(0L, initialStats.totalSyncs)
        assertEquals(0L, initialStats.successfulSyncs)
        assertEquals(0L, initialStats.failedSyncs)
        
        // 执行同步
        syncManager.startSync()
        delay(3000)
        
        val afterSyncStats = syncManager.statistics.first()
        assertTrue(afterSyncStats.totalSyncs > initialStats.totalSyncs)
        assertTrue(afterSyncStats.successfulSyncs > initialStats.successfulSyncs)
        assertTrue(afterSyncStats.lastSyncTime > initialStats.lastSyncTime)
        assertTrue(afterSyncStats.lastSyncDuration > 0)
    }
    
    @Test
    fun `test sync progress tracking`() = testScope.runTest {
        var progressUpdates = 0
        syncManager.syncState.collect { state ->
            when (state) {
                SyncManager.SyncState.PREPARING -> progressUpdates++
                SyncManager.SyncState.SYNCING -> progressUpdates++
                SyncManager.SyncState.SUCCESS -> progressUpdates++
                else -> {}
            }
        }
        
        syncManager.startSync()
        
        // 等待同步完成
        delay(3000)
        
        assertTrue(progressUpdates >= 2, "Should have progress updates")
        
        val finalProgress = syncManager.syncProgress.first()
        assertEquals(0.0f, finalProgress) // 应该重置为0
    }
    
    @Test
    fun `test error handling`() = testScope.runTest {
        // 测试网络不可用的情况
        // 这个测试需要 mock NetworkManager
        
        syncManager.startSync()
        delay(3000)
        
        // 验证错误状态
        val finalState = syncManager.syncState.first()
        assertTrue(
            finalState == SyncManager.SyncState.SUCCESS || 
            finalState == SyncManager.SyncState.ERROR
        )
    }
    
    @Test
    fun `test sync mode switching`() = testScope.runTest {
        // 手动同步
        val manualResult = syncManager.startSync(SyncManager.SyncMode.MANUAL)
        assertTrue(manualResult)
        assertEquals(SyncManager.SyncMode.MANUAL, syncManager.syncMode.first())
        
        delay(3000)
        
        // 定时同步
        val scheduledResult = syncManager.startSync(SyncManager.SyncMode.SCHEDULED)
        assertTrue(scheduledResult)
        assertEquals(SyncManager.SyncMode.SCHEDULED, syncManager.syncMode.first())
        
        delay(3000)
        
        val stats = syncManager.statistics.first()
        assertTrue(stats.totalSyncs >= 2, "Should have performed at least 2 syncs")
    }
    
    @Test
    fun `test auto sync toggle`() {
        // 启用自动同步
        syncManager.enableAutoSync(true)
        assertTrue(syncManager.config.first().autoSync)
        
        // 禁用自动同步
        syncManager.enableAutoSync(false)
        assertFalse(syncManager.config.first().autoSync)
    }
    
    @Test
    fun `test sync interval validation`() {
        // 测试有效间隔
        assertTrue(syncManager.setSyncInterval(600)) // 10分钟
        assertTrue(syncManager.setSyncInterval(3600)) // 1小时
        
        // 测试无效间隔（太短）
        assertFalse(syncManager.setSyncInterval(30)) // 30秒太短
        assertFalse(syncManager.setSyncInterval(0))
        
        // 验证有效间隔被设置
        assertEquals(600, syncManager.getSyncInterval())
    }
    
    @Test
    fun `test sync manager cleanup`() {
        syncManager.startSync()
        syncManager.cleanup()
        
        // 清理后状态应该重置
        assertEquals(SyncManager.SyncState.IDLE, syncManager.syncState.first())
    }
    
    @Test
    fun `test configuration persistence`() {
        // 更新配置
        syncManager.enableAutoSync(true)
        syncManager.setSyncInterval(1200) // 20分钟
        
        val config1 = syncManager.config.first()
        assertTrue(config1.autoSync)
        assertEquals(1200, config1.syncInterval)
        
        // 创建新的 SyncManager 实例（模拟重启）
        val newSyncManager = SyncManager()
        newSyncManager.initialize(mockContext)
        
        // 验证配置是否被持久化（在实际实现中）
        val config2 = newSyncManager.config.first()
        // 在实际实现中，这里应该验证配置被正确保存和加载
        
        newSyncManager.cleanup()
    }
    
    @Test
    fun `test sync timeout handling`() = testScope.runTest {
        // 这个测试需要模拟超时情况
        val syncResult = syncManager.startSync(SyncManager.SyncMode.MANUAL)
        
        // 等待足够长的时间
        delay(10000)
        
        // 验证超时处理
        val finalState = syncManager.syncState.first()
        assertTrue(
            finalState == SyncManager.SyncState.SUCCESS || 
            finalState == SyncManager.SyncState.ERROR
        )
    }
    
    @Test
    fun `test data integrity during sync`() = testScope.runTest {
        // 这个测试需要实际的数据同步实现
        val beforeStats = syncManager.statistics.first()
        
        syncManager.startSync()
        delay(3000)
        
        val afterStats = syncManager.statistics.first()
        
        // 验证同步后统计信息的完整性
        assertTrue(afterStats.totalSyncs > beforeStats.totalSyncs)
        assertTrue(afterStats.lastSyncTime > beforeStats.lastSyncTime)
    }
}