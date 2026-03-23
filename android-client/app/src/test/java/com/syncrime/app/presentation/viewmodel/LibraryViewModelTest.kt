package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.shared.data.local.dao.ClipDao
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.shared.model.SourceType
import com.syncrime.android.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * LibraryViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var application: Application
    
    @Mock
    private lateinit var clipboardManager: ClipboardManager
    
    @Mock
    private lateinit var appDatabase: AppDatabase
    
    @Mock
    private lateinit var clipDao: ClipDao
    
    @Mock
    private lateinit var syncManager: SyncManager
    
    private lateinit var libraryViewModel: LibraryViewModel
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        `when`(application.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(clipboardManager)
        `when`(appDatabase.clipDao()).thenReturn(clipDao)
        `when`(clipDao.getAll()).thenReturn(MutableStateFlow(emptyList()))
        
        libraryViewModel = LibraryViewModel(application)
        // 替换内部依赖为模拟对象
        libraryViewModel.database = appDatabase
        libraryViewModel.syncManager = syncManager
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ========== 初始状态测试 ==========
    
    @Test
    fun initialState_isCorrect() = runTest {
        val state = libraryViewModel.uiState.value
        
        assertTrue("Initial recent clipboard should be empty", state.recentClipboard.isEmpty())
        assertTrue("Initial clips should be empty", state.clips.isEmpty())
        assertNull("No clip should be selected", state.selectedClip)
        assertNull("No clip should be editing", state.editingClip)
        assertFalse("Should not be loading initially", state.isLoading)
        assertEquals("Search query should be empty", "", state.searchQuery)
        assertTrue("Filtered clips should be empty", state.filteredClips.isEmpty())
        assertNull("No message initially", state.message)
    }
    
    // ========== 加载剪藏测试 ==========
    
    @Test
    fun loadClips_setsLoadingState() = runTest {
        val clips = listOf(createTestClip(1, "测试剪藏"))
        `when`(clipDao.getAll()).thenReturn(MutableStateFlow(clips))
        
        // 重新初始化viewModel以触发init块
        libraryViewModel = LibraryViewModel(application)
        libraryViewModel.database = appDatabase
        libraryViewModel.syncManager = syncManager
        
        // 等待加载完成
        Thread.sleep(100)
        
        val state = libraryViewModel.uiState.value
        assertFalse("Should finish loading", state.isLoading)
        assertEquals("Should load clips", clips.size, state.clips.size)
    }
    
    @Test
    fun loadClips_updatesClips() = runTest {
        val clips = listOf(
            createTestClip(1, "工作笔记", "工作相关内容"),
            createTestClip(2, "个人笔记", "个人相关内容")
        )
        `when`(clipDao.getAll()).thenReturn(MutableStateFlow(clips))
        
        // 重新初始化viewModel以触发init块
        libraryViewModel = LibraryViewModel(application)
        libraryViewModel.database = appDatabase
        libraryViewModel.syncManager = syncManager
        
        Thread.sleep(100)
        
        val state = libraryViewModel.uiState.value
        assertEquals("Should load all clips", 2, state.clips.size)
        assertTrue("Should have work clip", state.clips.any { it.title == "工作笔记" })
        assertTrue("Should have personal clip", state.clips.any { it.title == "个人笔记" })
    }
    
    // ========== 搜索功能测试 ==========
    
    @Test
    fun setSearchQuery_updatesQueryAndFiltersClips() = runTest {
        val clips = listOf(
            createTestClip(1, "Android开发", "关于Android的笔记"),
            createTestClip(2, "iOS开发", "关于iOS的笔记"),
            createTestClip(3, "Web开发", "关于Web的笔记")
        )
        `when`(clipDao.getAll()).thenReturn(MutableStateFlow(clips))
        
        libraryViewModel = LibraryViewModel(application)
        libraryViewModel.database = appDatabase
        libraryViewModel.syncManager = syncManager
        
        Thread.sleep(100) // 等待初始加载完成
        
        libraryViewModel.setSearchQuery("Android")
        
        val state = libraryViewModel.uiState.value
        assertEquals("Query should be updated", "Android", state.searchQuery)
        assertEquals("Should filter to 1 clip", 1, state.filteredClips.size)
        assertEquals("Should find Android clip", "Android开发", state.filteredClips[0].title)
    }
    
    @Test
    fun setSearchQuery_emptyShowsAll() = runTest {
        val clips = listOf(
            createTestClip(1, "Android开发", "关于Android的笔记"),
            createTestClip(2, "iOS开发", "关于iOS的笔记")
        )
        `when`(clipDao.getAll()).thenReturn(MutableStateFlow(clips))
        
        libraryViewModel = LibraryViewModel(application)
        libraryViewModel.database = appDatabase
        libraryViewModel.syncManager = syncManager
        
        Thread.sleep(100) // 等待初始加载完成
        
        libraryViewModel.setSearchQuery("Android") // 先搜索
        libraryViewModel.setSearchQuery("") // 再清空搜索
        
        val state = libraryViewModel.uiState.value
        assertEquals("Should show all clips when query is empty", 2, state.filteredClips.size)
    }
    
    @Test
    fun setSearchQuery_caseInsensitive() = runTest {
        val clips = listOf(
            createTestClip(1, "ANDROID开发", "关于Android的笔记")
        )
        `when`(clipDao.getAll()).thenReturn(MutableStateFlow(clips))
        
        libraryViewModel = LibraryViewModel(application)
        libraryViewModel.database = appDatabase
        libraryViewModel.syncManager = syncManager
        
        Thread.sleep(100) // 等待初始加载完成
        
        libraryViewModel.setSearchQuery("android")
        
        val state = libraryViewModel.uiState.value
        assertEquals("Should find case-insensitive match", 1, state.filteredClips.size)
        assertEquals("Should find Android clip", "ANDROID开发", state.filteredClips[0].title)
    }
    
    @Test
    fun setSearchQuery_searchesInContent() = runTest {
        val clips = listOf(
            createTestClip(1, "标题1", "这是一篇关于Android开发的文章"),
            createTestClip(2, "标题2", "这是一篇关于iOS开发的文章")
        )
        `when`(clipDao.getAll()).thenReturn(MutableStateFlow(clips))
        
        libraryViewModel = LibraryViewModel(application)
        libraryViewModel.database = appDatabase
        libraryViewModel.syncManager = syncManager
        
        Thread.sleep(100) // 等待初始加载完成
        
        libraryViewModel.setSearchQuery("Android")
        
        val state = libraryViewModel.uiState.value
        assertEquals("Should find clip by content", 1, state.filteredClips.size)
        assertEquals("Should find Android content clip", "标题1", state.filteredClips[0].title)
    }
    
    @Test
    fun setSearchQuery_searchesInCategory() = runTest {
        val clips = listOf(
            createTestClip(1, "笔记1", "内容1", category = "技术"),
            createTestClip(2, "笔记2", "内容2", category = "生活")
        )
        `when`(clipDao.getAll()).thenReturn(MutableStateFlow(clips))
        
        libraryViewModel = LibraryViewModel(application)
        libraryViewModel.database = appDatabase
        libraryViewModel.syncManager = syncManager
        
        Thread.sleep(100) // 等待初始加载完成
        
        libraryViewModel.setSearchQuery("技术")
        
        val state = libraryViewModel.uiState.value
        assertEquals("Should find clip by category", 1, state.filteredClips.size)
        assertEquals("Should find tech clip", "笔记1", state.filteredClips[0].title)
    }
    
    @Test
    fun setSearchQuery_searchesInTags() = runTest {
        val clips = listOf(
            createTestClip(1, "笔记1", "内容1", tags = listOf("Android", "Mobile")),
            createTestClip(2, "笔记2", "内容2", tags = listOf("iOS", "Mobile"))
        )
        `when`(clipDao.getAll()).thenReturn(MutableStateFlow(clips))
        
        libraryViewModel = LibraryViewModel(application)
        libraryViewModel.database = appDatabase
        libraryViewModel.syncManager = syncManager
        
        Thread.sleep(100) // 等待初始加载完成
        
        libraryViewModel.setSearchQuery("Android")
        
        val state = libraryViewModel.uiState.value
        assertEquals("Should find clip by tag", 1, state.filteredClips.size)
        assertEquals("Should find Android tag clip", "笔记1", state.filteredClips[0].title)
    }
    
    // ========== 剪贴板历史测试 ==========
    
    @Test
    fun loadClipboardHistory_addsNewClip() = runTest {
        val clipData = android.content.ClipData.newPlainText("测试", "剪贴板内容")
        `when`(clipboardManager.primaryClip).thenReturn(clipData)
        
        libraryViewModel.loadClipboardHistory()
        
        val state = libraryViewModel.uiState.value
        assertEquals("Should add clipboard content", 1, state.recentClipboard.size)
        assertEquals("Should have correct text", "剪贴板内容", state.recentClipboard[0].text)
    }
    
    @Test
    fun loadClipboardHistory_ignoresShortText() = runTest {
        val clipData = android.content.ClipData.newPlainText("测试", "短")
        `when`(clipboardManager.primaryClip).thenReturn(clipData)
        
        libraryViewModel.loadClipboardHistory()
        
        val state = libraryViewModel.uiState.value
        assertTrue("Should ignore short text", state.recentClipboard.isEmpty())
    }
    
    @Test
    fun loadClipboardHistory_ignoresDuplicate() = runTest {
        val clipData = android.content.ClipData.newPlainText("测试", "相同内容")
        `when`(clipboardManager.primaryClip).thenReturn(clipData)
        
        libraryViewModel.loadClipboardHistory()
        libraryViewModel.loadClipboardHistory() // 再次加载相同内容
        
        val state = libraryViewModel.uiState.value
        assertEquals("Should not add duplicate", 1, state.recentClipboard.size)
    }
    
    @Test
    fun loadClipboardHistory_maintainsLimit() = runTest {
        repeat(15) { i ->
            val clipData = android.content.ClipData.newPlainText("测试", "内容$i")
            `when`(clipboardManager.primaryClip).thenReturn(clipData)
            libraryViewModel.loadClipboardHistory()
        }
        
        val state = libraryViewModel.uiState.value
        assertEquals("Should maintain limit of 10", 10, state.recentClipboard.size)
        assertEquals("Should have latest content first", "内容14", state.recentClipboard[0].text)
        assertEquals("Should have older content last", "内容5", state.recentClipboard[9].text)
    }
    
    // ========== 添加剪藏测试 ==========
    
    @Test
    fun addToClip_successfullyAdds() = runTest {
        val clip = createTestClip(1, "测试", "测试内容")
        `when`(clipDao.insert(any(KnowledgeClip::class.java))).thenReturn(1L)
        
        libraryViewModel.addToClip("测试内容", "测试标题")
        
        verify(clipDao).insert(any(KnowledgeClip::class.java))
        verify(syncManager).syncClip(anyLong(), eq("create"))
        assertEquals("Should show success message", "已添加到剪藏", libraryViewModel.uiState.value.message)
    }
    
    @Test
    fun addToClip_generatesTitleFromContent() = runTest {
        val content = "这是很长的测试内容，用来验证标题生成"
        `when`(clipDao.insert(any(KnowledgeClip::class.java))).thenReturn(1L)
        
        libraryViewModel.addToClip(content)
        
        verify(clipDao).insert(argThat { 
            title == content.take(50) // 验证标题是从内容生成的
        })
    }
    
    @Test
    fun addToClip_handlesException() = runTest {
        `when`(clipDao.insert(any(KnowledgeClip::class.java))).thenThrow(RuntimeException("数据库错误"))
        
        libraryViewModel.addToClip("测试内容")
        
        assertEquals("Should show error message", "添加失败", libraryViewModel.uiState.value.message)
    }
    
    // ========== 选择剪藏测试 ==========
    
    @Test
    fun selectClip_updatesSelectedClip() = runTest {
        val clip = createTestClip(1, "选中的剪藏", "内容")
        libraryViewModel.selectClip(clip)
        
        assertEquals("Should select clip", clip, libraryViewModel.uiState.value.selectedClip)
    }
    
    @Test
    fun clearSelectedClip_clearsSelection() = runTest {
        val clip = createTestClip(1, "选中的剪藏", "内容")
        libraryViewModel.selectClip(clip)
        libraryViewModel.clearSelectedClip()
        
        assertNull("Should clear selection", libraryViewModel.uiState.value.selectedClip)
    }
    
    // ========== 编辑剪藏测试 ==========
    
    @Test
    fun startEdit_setsEditingClip() = runTest {
        val clip = createTestClip(1, "编辑的剪藏", "内容")
        libraryViewModel.startEdit(clip)
        
        assertEquals("Should set editing clip", clip, libraryViewModel.uiState.value.editingClip)
    }
    
    @Test
    fun cancelEdit_clearsEditingClip() = runTest {
        val clip = createTestClip(1, "编辑的剪藏", "内容")
        libraryViewModel.startEdit(clip)
        libraryViewModel.cancelEdit()
        
        assertNull("Should clear editing clip", libraryViewModel.uiState.value.editingClip)
    }
    
    @Test
    fun updateClip_successfullyUpdates() = runTest {
        val originalClip = createTestClip(1, "原标题", "原内容")
        val updatedClip = originalClip.copy(title = "新标题", content = "新内容")
        
        libraryViewModel.updateClip(updatedClip)
        
        verify(clipDao).update(updatedClip)
        verify(syncManager).syncClip(1L, "update")
        assertNull("Should clear editing clip", libraryViewModel.uiState.value.editingClip)
        assertNull("Should clear selected clip", libraryViewModel.uiState.value.selectedClip)
        assertEquals("Should show success message", "已更新", libraryViewModel.uiState.value.message)
    }
    
    @Test
    fun updateClip_handlesException() = runTest {
        val clip = createTestClip(1, "测试", "内容")
        `when`(clipDao.update(any(KnowledgeClip::class.java))).thenThrow(RuntimeException("更新失败"))
        
        libraryViewModel.updateClip(clip)
        
        assertEquals("Should show error message", "更新失败", libraryViewModel.uiState.value.message)
    }
    
    // ========== 删除剪藏测试 ==========
    
    @Test
    fun deleteClip_successfullyDeletes() = runTest {
        val clip = createTestClip(1, "测试", "内容")
        `when`(clipDao.delete(any(KnowledgeClip::class.java))).thenReturn(Unit)
        
        libraryViewModel.deleteClip(1L)
        
        verify(clipDao).delete(clip)
        verify(syncManager).syncClip(1L, "delete")
        assertNull("Should clear selected clip", libraryViewModel.uiState.value.selectedClip)
        assertEquals("Should show success message", "已删除", libraryViewModel.uiState.value.message)
    }
    
    @Test
    fun deleteClip_handlesException() = runTest {
        `when`(clipDao.delete(any(KnowledgeClip::class.java))).thenThrow(RuntimeException("删除失败"))
        
        libraryViewModel.deleteClip(1L)
        
        assertEquals("Should show error message", "删除失败", libraryViewModel.uiState.value.message)
    }
    
    // ========== 更新查看次数测试 ==========
    
    @Test
    fun incrementViewCount_successfullyIncrements() = runTest {
        libraryViewModel.incrementViewCount(1L)
        
        verify(clipDao).incrementViewCount(1L)
    }
    
    @Test
    fun incrementViewCount_handlesExceptionSilently() = runTest {
        `when`(clipDao.incrementViewCount(anyLong())).thenThrow(RuntimeException("更新失败"))
        
        // 应该不会抛出异常
        libraryViewModel.incrementViewCount(1L)
        
        // 验证调用了方法但没有影响UI状态
        verify(clipDao).incrementViewCount(1L)
    }
    
    // ========== 清除消息测试 ==========
    
    @Test
    fun clearMessage_clearsMessage() = runTest {
        // 先设置一个消息
        libraryViewModel.addToClip("测试")
        assertNotNull("Should have message", libraryViewModel.uiState.value.message)
        
        libraryViewModel.clearMessage()
        
        assertNull("Should clear message", libraryViewModel.uiState.value.message)
    }
    
    // ========== 辅助方法 ==========
    
    private fun createTestClip(
        id: Long = 0,
        title: String = "测试剪藏",
        content: String = "测试内容",
        category: String? = null,
        tags: List<String> = emptyList()
    ): KnowledgeClip {
        return KnowledgeClip(
            id = id,
            title = title,
            content = content,
            summary = null,
            category = category,
            tags = tags,
            sourceType = SourceType.MANUAL,
            sourceUrl = null,
            visibility = com.syncrime.shared.model.Visibility.PUBLIC,
            viewCount = 0,
            favoriteCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}