package com.syncrime.shared.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.model.InputRecord
import com.syncrime.shared.model.Visibility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * InputDao 单元测试
 */
@RunWith(AndroidJUnit4::class)
class InputDaoTest {
    
    private lateinit var db: AppDatabase
    private lateinit var inputDao: InputDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        inputDao = db.inputDao()
    }
    
    @After
    fun closeDb() {
        db.close()
    }
    
    // ========== 插入测试 ==========
    
    @Test
    fun insertRecord_returnsValidId() = runBlocking {
        val record = createTestRecord(content = "测试内容")
        val id = inputDao.insert(record)
        assertTrue("Insert should return valid id", id > 0)
    }
    
    @Test
    fun insertAll_savesAllRecords() = runBlocking {
        val records = listOf(
            createTestRecord(content = "内容1"),
            createTestRecord(content = "内容2"),
            createTestRecord(content = "内容3")
        )
        inputDao.insertAll(records)
        
        val saved = inputDao.getAll(10, 0).first()
        assertEquals("Should save 3 records", 3, saved.size)
    }
    
    // ========== 查询测试 ==========
    
    @Test
    fun getById_returnsCorrectRecord() = runBlocking {
        val record = createTestRecord(content = "特定内容")
        val id = inputDao.insert(record)
        
        val retrieved = inputDao.getById(id)
        assertNotNull("Should find record", retrieved)
        assertEquals("Content should match", "特定内容", retrieved?.content)
    }
    
    @Test
    fun getById_returnsNullForNonExistent() = runBlocking {
        val retrieved = inputDao.getById(999999)
        assertNull("Should return null for non-existent id", retrieved)
    }
    
    @Test
    fun getAll_returnsOrderedByDateDesc() = runBlocking {
        val now = System.currentTimeMillis()
        inputDao.insert(createTestRecord(content = "旧", createdAt = now - 2000))
        inputDao.insert(createTestRecord(content = "新", createdAt = now))
        inputDao.insert(createTestRecord(content = "中", createdAt = now - 1000))
        
        val records = inputDao.getAll(10, 0).first()
        assertEquals("First should be newest", "新", records[0].content)
        assertEquals("Last should be oldest", "旧", records[2].content)
    }
    
    @Test
    fun getAll_respectsLimit() = runBlocking {
        repeat(20) {
            inputDao.insert(createTestRecord(content = "内容$it"))
        }
        
        val records = inputDao.getAll(5, 0).first()
        assertEquals("Should respect limit", 5, records.size)
    }
    
    @Test
    fun getAll_respectsOffset() = runBlocking {
        repeat(10) {
            inputDao.insert(createTestRecord(content = "内容$it"))
        }
        
        val page1 = inputDao.getAll(5, 0).first()
        val page2 = inputDao.getAll(5, 5).first()
        
        assertNotEquals("Pages should be different", page1[0].content, page2[0].content)
    }
    
    // ========== 按应用查询测试 ==========
    
    @Test
    fun getByApplication_filtersCorrectly() = runBlocking {
        inputDao.insert(createTestRecord(content = "微信消息", application = "com.tencent.mm"))
        inputDao.insert(createTestRecord(content = "浏览器", application = "com.android.browser"))
        inputDao.insert(createTestRecord(content = "微信消息2", application = "com.tencent.mm"))
        
        val wechatRecords = inputDao.getByApplication("com.tencent.mm").first()
        assertEquals("Should find 2 WeChat records", 2, wechatRecords.size)
        wechatRecords.forEach {
            assertEquals("All should be from WeChat", "com.tencent.mm", it.application)
        }
    }
    
    // ========== 按分类查询测试 ==========
    
    @Test
    fun getByCategory_filtersCorrectly() = runBlocking {
        inputDao.insert(createTestRecord(content = "工作内容", category = "work"))
        inputDao.insert(createTestRecord(content = "个人内容", category = "personal"))
        inputDao.insert(createTestRecord(content = "工作内容2", category = "work"))
        
        val workRecords = inputDao.getByCategory("work").first()
        assertEquals("Should find 2 work records", 2, workRecords.size)
    }
    
    // ========== 搜索测试 ==========
    
    @Test
    fun search_findsMatchingContent() = runBlocking {
        inputDao.insert(createTestRecord(content = "今天是晴天"))
        inputDao.insert(createTestRecord(content = "明天会下雨"))
        inputDao.insert(createTestRecord(content = "后天也是晴天"))
        
        val results = inputDao.search("晴天").first()
        assertEquals("Should find 2 matching records", 2, results.size)
    }
    
    @Test
    fun search_isCaseInsensitive() = runBlocking {
        inputDao.insert(createTestRecord(content = "Hello World"))
        
        val results1 = inputDao.search("hello").first()
        val results2 = inputDao.search("HELLO").first()
        
        assertEquals("Should be case insensitive", results1.size, results2.size)
    }
    
    @Test
    fun search_returnsEmptyForNoMatch() = runBlocking {
        inputDao.insert(createTestRecord(content = "测试内容"))
        
        val results = inputDao.search("不存在的关键词").first()
        assertTrue("Should return empty for no match", results.isEmpty())
    }
    
    // ========== 统计测试 ==========
    
    @Test
    fun getTodayCount_returnsCorrectCount() = runBlocking {
        val today = System.currentTimeMillis()
        val yesterday = today - 24 * 60 * 60 * 1000
        
        inputDao.insert(createTestRecord(createdAt = today))
        inputDao.insert(createTestRecord(createdAt = today))
        inputDao.insert(createTestRecord(createdAt = yesterday))
        
        val count = inputDao.getTodayCount().first()
        assertEquals("Should count 2 today's records", 2, count)
    }
    
    @Test
    fun getTotalCount_returnsCorrectCount() = runBlocking {
        repeat(5) {
            inputDao.insert(createTestRecord())
        }
        
        val count = inputDao.getTotalCount().first()
        assertEquals("Total count should be 5", 5, count)
    }
    
    // ========== 删除测试 ==========
    
    @Test
    fun delete_removesRecord() = runBlocking {
        val record = createTestRecord(content = "待删除")
        val id = inputDao.insert(record)
        
        inputDao.delete(record.copy(id = id))
        
        val retrieved = inputDao.getById(id)
        assertNull("Record should be deleted", retrieved)
    }
    
    @Test
    fun deleteBefore_removesOldRecords() = runBlocking {
        val now = System.currentTimeMillis()
        val oldTime = now - 31 * 24 * 60 * 60 * 1000L // 31 天前
        
        inputDao.insert(createTestRecord(createdAt = oldTime))
        inputDao.insert(createTestRecord(createdAt = now))
        
        val deleted = inputDao.deleteBefore(now - 30 * 24 * 60 * 60 * 1000L)
        
        assertEquals("Should delete 1 old record", 1, deleted)
        val remaining = inputDao.getTotalCount().first()
        assertEquals("Should have 1 remaining", 1, remaining)
    }
    
    @Test
    fun deleteAll_clearsAllRecords() = runBlocking {
        repeat(10) {
            inputDao.insert(createTestRecord())
        }
        
        inputDao.deleteAll()
        
        val count = inputDao.getTotalCount().first()
        assertEquals("Should be empty after deleteAll", 0, count)
    }
    
    // ========== 辅助方法 ==========
    
    private fun createTestRecord(
        content: String = "测试内容",
        application: String = "com.test.app",
        category: String? = null,
        createdAt: Long = System.currentTimeMillis()
    ): InputRecord {
        return InputRecord(
            id = 0,
            content = content,
            application = application,
            category = category,
            visibility = Visibility.PUBLIC,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }
}