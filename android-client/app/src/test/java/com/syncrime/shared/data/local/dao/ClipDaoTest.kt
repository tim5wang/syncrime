package com.syncrime.shared.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.shared.model.SourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ClipDao 单元测试
 */
@RunWith(AndroidJUnit4::class)
class ClipDaoTest {
    
    private lateinit var db: AppDatabase
    private lateinit var clipDao: ClipDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        clipDao = db.clipDao()
    }
    
    @After
    fun closeDb() {
        db.close()
    }
    
    // ========== 插入测试 ==========
    
    @Test
    fun insertClip_returnsValidId() = runBlocking {
        val clip = createTestClip(title = "测试剪藏")
        val id = clipDao.insert(clip)
        assertTrue("Insert should return valid id", id > 0)
    }
    
    @Test
    fun insertAll_savesAllClips() = runBlocking {
        val clips = listOf(
            createTestClip(title = "剪藏1"),
            createTestClip(title = "剪藏2"),
            createTestClip(title = "剪藏3")
        )
        clipDao.insertAll(clips)
        
        val saved = clipDao.getAll(10, 0).first()
        assertEquals("Should save 3 clips", 3, saved.size)
    }
    
    // ========== 查询测试 ==========
    
    @Test
    fun getById_returnsCorrectClip() = runBlocking {
        val clip = createTestClip(title = "特定剪藏")
        val id = clipDao.insert(clip)
        
        val retrieved = clipDao.getById(id)
        assertNotNull("Should find clip", retrieved)
        assertEquals("Title should match", "特定剪藏", retrieved?.title)
    }
    
    @Test
    fun getById_returnsNullForNonExistent() = runBlocking {
        val retrieved = clipDao.getById(999999)
        assertNull("Should return null for non-existent id", retrieved)
    }
    
    @Test
    fun getAll_returnsOrderedByDateDesc() = runBlocking {
        val now = System.currentTimeMillis()
        clipDao.insert(createTestClip(title = "旧", createdAt = now - 2000))
        clipDao.insert(createTestClip(title = "新", createdAt = now))
        clipDao.insert(createTestClip(title = "中", createdAt = now - 1000))
        
        val clips = clipDao.getAll(10, 0).first()
        assertEquals("First should be newest", "新", clips[0].title)
        assertEquals("Last should be oldest", "旧", clips[2].title)
    }
    
    @Test
    fun getAll_respectsLimit() = runBlocking {
        repeat(20) {
            clipDao.insert(createTestClip(title = "剪藏$it"))
        }
        
        val clips = clipDao.getAll(5, 0).first()
        assertEquals("Should respect limit", 5, clips.size)
    }
    
    // ========== 按分类查询测试 ==========
    
    @Test
    fun getByCategory_filtersCorrectly() = runBlocking {
        clipDao.insert(createTestClip(title = "工作笔记", category = "work"))
        clipDao.insert(createTestClip(title = "个人笔记", category = "personal"))
        clipDao.insert(createTestClip(title = "工作笔记2", category = "work"))
        
        val workClips = clipDao.getByCategory("work").first()
        assertEquals("Should find 2 work clips", 2, workClips.size)
        workClips.forEach {
            assertEquals("All should be work category", "work", it.category)
        }
    }
    
    // ========== 按标签查询测试 ==========
    
    @Test
    fun getByTag_filtersCorrectly() = runBlocking {
        clipDao.insert(createTestClip(title = "笔记1", tags = listOf("技术", "Android")))
        clipDao.insert(createTestClip(title = "笔记2", tags = listOf("生活", "美食")))
        clipDao.insert(createTestClip(title = "笔记3", tags = listOf("技术", "Kotlin")))
        
        val techClips = clipDao.getByTag("技术").first()
        assertEquals("Should find 2 tech clips", 2, techClips.size)
    }
    
    // ========== 按来源类型查询测试 ==========
    
    @Test
    fun getBySourceType_filtersCorrectly() = runBlocking {
        clipDao.insert(createTestClip(title = "手动输入", sourceType = SourceType.MANUAL))
        clipDao.insert(createTestClip(title = "自动采集", sourceType = SourceType.AUTO_INPUT))
        clipDao.insert(createTestClip(title = "另一个手动", sourceType = SourceType.MANUAL))
        
        val manualClips = clipDao.getBySourceType(SourceType.MANUAL).first()
        assertEquals("Should find 2 manual clips", 2, manualClips.size)
    }
    
    // ========== 搜索测试 ==========
    
    @Test
    fun search_findsInTitle() = runBlocking {
        clipDao.insert(createTestClip(title = "Android开发笔记"))
        clipDao.insert(createTestClip(title = "iOS开发笔记"))
        
        val results = clipDao.search("Android").first()
        assertEquals("Should find Android clip", 1, results.size)
        assertTrue("Title should contain Android", results[0].title.contains("Android"))
    }
    
    @Test
    fun search_findsInContent() = runBlocking {
        clipDao.insert(createTestClip(title = "笔记", content = "这是一篇关于Kotlin的文章"))
        clipDao.insert(createTestClip(title = "另一篇", content = "这是一篇关于Java的文章"))
        
        val results = clipDao.search("Kotlin").first()
        assertEquals("Should find Kotlin content", 1, results.size)
    }
    
    // ========== 统计测试 ==========
    
    @Test
    fun getTotalCount_returnsCorrectCount() = runBlocking {
        repeat(5) {
            clipDao.insert(createTestClip())
        }
        
        val count = clipDao.getTotalCount()
        assertEquals("Total count should be 5", 5, count)
    }
    
    @Test
    fun getTodayCount_returnsCorrectCount() = runBlocking {
        val today = System.currentTimeMillis()
        val yesterday = today - 24 * 60 * 60 * 1000
        
        clipDao.insert(createTestClip(createdAt = today))
        clipDao.insert(createTestClip(createdAt = today))
        clipDao.insert(createTestClip(createdAt = yesterday))
        
        val count = clipDao.getTodayCount()
        assertEquals("Should count 2 today's clips", 2, count)
    }
    
    // ========== 更新测试 ==========
    
    @Test
    fun update_modifiesClip() = runBlocking {
        val clip = createTestClip(title = "原标题")
        val id = clipDao.insert(clip)
        
        val updated = clip.copy(id = id, title = "新标题")
        clipDao.update(updated)
        
        val retrieved = clipDao.getById(id)
        assertEquals("Title should be updated", "新标题", retrieved?.title)
    }
    
    @Test
    fun incrementViewCount_incrementsByOne() = runBlocking {
        val clip = createTestClip(viewCount = 0)
        val id = clipDao.insert(clip)
        
        clipDao.incrementViewCount(id)
        clipDao.incrementViewCount(id)
        
        val retrieved = clipDao.getById(id)
        assertEquals("View count should be 2", 2, retrieved?.viewCount)
    }
    
    // ========== 删除测试 ==========
    
    @Test
    fun delete_removesClip() = runBlocking {
        val clip = createTestClip(title = "待删除")
        val id = clipDao.insert(clip)
        
        clipDao.delete(clip.copy(id = id))
        
        val retrieved = clipDao.getById(id)
        assertNull("Clip should be deleted", retrieved)
    }
    
    @Test
    fun deleteBefore_removesOldClips() = runBlocking {
        val now = System.currentTimeMillis()
        val oldTime = now - 31 * 24 * 60 * 60 * 1000L // 31 天前
        
        clipDao.insert(createTestClip(createdAt = oldTime))
        clipDao.insert(createTestClip(createdAt = now))
        
        val deleted = clipDao.deleteBefore(now - 30 * 24 * 60 * 60 * 1000L)
        
        assertEquals("Should delete 1 old clip", 1, deleted)
        val remaining = clipDao.getTotalCount()
        assertEquals("Should have 1 remaining", 1, remaining)
    }
    
    // ========== 辅助方法 ==========
    
    private fun createTestClip(
        title: String = "测试剪藏",
        content: String = "测试内容",
        category: String? = null,
        tags: List<String> = emptyList(),
        sourceType: SourceType = SourceType.MANUAL,
        viewCount: Int = 0,
        favoriteCount: Int = 0,
        createdAt: Long = System.currentTimeMillis()
    ): KnowledgeClip {
        return KnowledgeClip(
            id = 0,
            title = title,
            content = content,
            summary = null,
            category = category,
            tags = tags,
            sourceType = sourceType,
            sourceUrl = null,
            visibility = com.syncrime.shared.model.Visibility.PUBLIC,
            viewCount = viewCount,
            favoriteCount = favoriteCount,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }
}