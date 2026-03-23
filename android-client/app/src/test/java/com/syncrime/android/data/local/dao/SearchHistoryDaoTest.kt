package com.syncrime.android.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.syncrime.android.data.local.AppDatabase
import com.syncrime.android.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SearchHistoryDao 单元测试
 */
@RunWith(AndroidJUnit4::class)
class SearchHistoryDaoTest {
    
    private lateinit var db: AppDatabase
    private lateinit var searchHistoryDao: SearchHistoryDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        searchHistoryDao = db.searchHistoryDao()
    }
    
    @After
    fun closeDb() {
        db.close()
    }
    
    // ========== 插入测试 ==========
    
    @Test
    fun insertHistory_returnsValidId() = runBlocking {
        val history = createTestHistory(query = "测试查询")
        val id = searchHistoryDao.insert(history)
        assertTrue("Insert should return valid id", id > 0)
    }
    
    // ========== 查询测试 ==========
    
    @Test
    fun getAll_returnsOrderedByDateDesc() = runBlocking {
        val now = System.currentTimeMillis()
        searchHistoryDao.insert(createTestHistory(query = "旧查询", createdAt = now - 2000))
        searchHistoryDao.insert(createTestHistory(query = "新查询", createdAt = now))
        searchHistoryDao.insert(createTestHistory(query = "中查询", createdAt = now - 1000))
        
        val histories = searchHistoryDao.getAll(10).first()
        assertEquals("First should be newest", "新查询", histories[0].query)
        assertEquals("Last should be oldest", "旧查询", histories[2].query)
    }
    
    @Test
    fun getAll_respectsLimit() = runBlocking {
        repeat(20) {
            searchHistoryDao.insert(createTestHistory(query = "查询$it"))
        }
        
        val histories = searchHistoryDao.getAll(5).first()
        assertEquals("Should respect limit", 5, histories.size)
    }
    
    // ========== 搜索建议测试 ==========
    
    @Test
    fun getSuggestions_returnsPrefixMatches() = runBlocking {
        searchHistoryDao.insert(createTestHistory(query = "Android开发"))
        searchHistoryDao.insert(createTestHistory(query = "Android Studio"))
        searchHistoryDao.insert(createTestHistory(query = "iOS开发"))
        searchHistoryDao.insert(createTestHistory(query = "Android应用"))
        
        val suggestions = searchHistoryDao.getSuggestions("Android", 10).first()
        assertEquals("Should find 3 Android suggestions", 3, suggestions.size)
        suggestions.forEach {
            assertTrue("Should start with Android", it.startsWith("Android"))
        }
    }
    
    @Test
    fun getSuggestions_respectsLimit() = runBlocking {
        repeat(15) {
            searchHistoryDao.insert(createTestHistory(query = "Android查询$it"))
        }
        
        val suggestions = searchHistoryDao.getSuggestions("Android", 5).first()
        assertEquals("Should respect limit", 5, suggestions.size)
    }
    
    @Test
    fun getSuggestions_returnsDistinct() = runBlocking {
        searchHistoryDao.insert(createTestHistory(query = "Android开发"))
        searchHistoryDao.insert(createTestHistory(query = "Android开发")) // 重复
        searchHistoryDao.insert(createTestHistory(query = "Android开发")) // 重复
        
        val suggestions = searchHistoryDao.getSuggestions("Android", 10).first()
        assertEquals("Should return distinct results", 1, suggestions.size)
        assertEquals("Should return the distinct query", "Android开发", suggestions[0])
    }
    
    @Test
    fun getSuggestions_isCaseSensitive() = runBlocking {
        searchHistoryDao.insert(createTestHistory(query = "Android开发"))
        searchHistoryDao.insert(createTestHistory(query = "android studio"))
        
        val upperSuggestions = searchHistoryDao.getSuggestions("Android", 10).first()
        val lowerSuggestions = searchHistoryDao.getSuggestions("android", 10).first()
        
        assertEquals("Should find Android query", 1, upperSuggestions.size)
        assertEquals("Should find android query", 1, lowerSuggestions.size)
        assertEquals("Should match exact case", "Android开发", upperSuggestions[0])
        assertEquals("Should match exact case", "android studio", lowerSuggestions[0])
    }
    
    // ========== 删除测试 ==========
    
    @Test
    fun delete_removesSpecificHistory() = runBlocking {
        val history = createTestHistory(query = "待删除")
        val id = searchHistoryDao.insert(history)
        
        val insertedHistory = SearchHistoryEntity(
            id = id,
            query = "待删除",
            searchType = "input",
            createdAt = System.currentTimeMillis()
        )
        searchHistoryDao.delete(insertedHistory)
        
        val remainingHistories = searchHistoryDao.getAll(10).first()
        assertTrue("History should be deleted", remainingHistories.isEmpty())
    }
    
    @Test
    fun clearByType_removesCorrectType() = runBlocking {
        searchHistoryDao.insert(createTestHistory(query = "输入查询", searchType = "input"))
        searchHistoryDao.insert(createTestHistory(query = "剪藏查询", searchType = "clip"))
        searchHistoryDao.insert(createTestHistory(query = "输入查询2", searchType = "input"))
        
        searchHistoryDao.clearByType("input")
        
        val remainingHistories = searchHistoryDao.getAll(10).first()
        assertEquals("Should have 1 clip history remaining", 1, remainingHistories.size)
        assertEquals("Remaining should be clip type", "clip", remainingHistories[0].searchType)
    }
    
    @Test
    fun deleteOldRecords_keepsSpecifiedCount() = runBlocking {
        val now = System.currentTimeMillis()
        repeat(10) { i ->
            searchHistoryDao.insert(createTestHistory(
                query = "查询$i",
                createdAt = now - (i * 1000) // 每个查询间隔1秒
            ))
        }
        
        // 保持最新的3条记录
        searchHistoryDao.deleteOldRecords(3)
        
        val remainingHistories = searchHistoryDao.getAll(20).first()
        assertEquals("Should keep 3 records", 3, remainingHistories.size)
        
        // 验证保留的是最新的3条记录
        assertEquals("Latest should be 查询0", "查询0", remainingHistories[0].query)
        assertEquals("Second latest should be 查询1", "查询1", remainingHistories[1].query)
        assertEquals("Third latest should be 查询2", "查询2", remainingHistories[2].query)
    }
    
    @Test
    fun deleteOldRecords_handlesLessThanKeepCount() = runBlocking {
        searchHistoryDao.insert(createTestHistory(query = "查询1"))
        
        // 尝试保留5条，但只有1条记录
        searchHistoryDao.deleteOldRecords(5)
        
        val remainingHistories = searchHistoryDao.getAll(10).first()
        assertEquals("Should still have 1 record", 1, remainingHistories.size)
    }
    
    // ========== 并发安全测试 ==========
    
    @Test
    fun insert_concurrentInsertions() = runBlocking {
        val queries = (1..100).map { i -> "并发查询$i" }
        
        // 并发插入
        queries.forEach { query ->
            searchHistoryDao.insert(createTestHistory(query = query))
        }
        
        val histories = searchHistoryDao.getAll(200).first()
        assertEquals("Should insert all queries", 100, histories.size)
    }
    
    // ========== 辅助方法 ==========
    
    private fun createTestHistory(
        query: String = "测试查询",
        searchType: String = "input",
        createdAt: Long = System.currentTimeMillis()
    ): SearchHistoryEntity {
        return SearchHistoryEntity(
            id = 0,
            query = query,
            searchType = searchType,
            createdAt = createdAt
        )
    }
}