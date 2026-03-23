package com.syncrime.app.presentation.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.syncrime.app.data.DataRepository
import com.syncrime.shared.model.InputRecord
import com.syncrime.shared.model.Visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * SearchViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var application: Application
    
    @Mock
    private lateinit var clipboardManager: ClipboardManager
    
    @Mock
    private lateinit var repository: DataRepository
    
    private lateinit var searchViewModel: SearchViewModel
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        `when`(application.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(clipboardManager)
        `when`(repository.getRecentRecords()).thenReturn(MutableStateFlow(emptyList()))
        `when`(repository.searchRecords(anyString())).thenReturn(MutableStateFlow(emptyList()))
        
        searchViewModel = SearchViewModel(application)
        // 替换内部依赖为模拟对象
        searchViewModel.repository = repository
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ========== 初始状态测试 ==========
    
    @Test
    fun initialState_isCorrect() = runTest {
        val state = searchViewModel.uiState.value
        
        assertEquals("Initial query should be empty", "", state.query)
        assertFalse("Should not be searching initially", state.isSearching)
        assertTrue("Results should be empty", state.results.isEmpty())
        assertTrue("Recent records should be empty", state.recentRecords.isEmpty())
        assertTrue("Search history should be empty", state.searchHistory.isEmpty())
        assertFalse("Should not have searched", state.hasSearched)
        assertTrue("Should show history initially", state.showHistory)
        assertNull("No record should be selected", state.selectedRecord)
        assertNull("No message initially", state.message)
        assertNull("No error initially", state.error)
    }
    
    // ========== 加载最近记录测试 ==========
    
    @Test
    fun loadRecentRecords_updatesState() = runTest {
        val recentRecords = listOf(
            createTestRecord(1, "最近记录1", "内容1"),
            createTestRecord(2, "最近记录2", "内容2")
        )
        `when`(repository.getRecentRecords()).thenReturn(MutableStateFlow(recentRecords))
        
        // 重新初始化viewModel以触发init块
        searchViewModel = SearchViewModel(application)
        searchViewModel.repository = repository
        
        Thread.sleep(100) // 等待加载完成
        
        val state = searchViewModel.uiState.value
        assertEquals("Should load recent records", 2, state.recentRecords.size)
        assertTrue("Should have record1", state.recentRecords.any { it.content == "内容1" })
        assertTrue("Should have record2", state.recentRecords.any { it.content == "内容2" })
    }
    
    // ========== 设置查询测试 ==========
    
    @Test
    fun setQuery_updatesQueryState() = runTest {
        searchViewModel.setQuery("新查询")
        
        val state = searchViewModel.uiState.value
        assertEquals("Query should be updated", "新查询", state.query)
    }
    
    @Test
    fun setQuery_blankHidesHistory() = runTest {
        searchViewModel.search("测试")
        searchViewModel.setQuery("")
        
        val state = searchViewModel.uiState.value
        assertTrue("Should show history when query is blank", state.showHistory)
    }
    
    @Test
    fun setQuery_nonBlankHidesHistory() = runTest {
        searchViewModel.setQuery("非空查询")
        
        val state = searchViewModel.uiState.value
        assertFalse("Should hide history when query is not blank", state.showHistory)
    }
    
    @Test
    fun setQuery_clearsResultsWhenBlank() = runTest {
        // 先进行一次搜索
        val records = listOf(createTestRecord(1, "测试", "内容"))
        `when`(repository.searchRecords("测试")).thenReturn(MutableStateFlow(records))
        searchViewModel.search("测试")
        
        // 然后设置为空查询
        searchViewModel.setQuery("")
        
        val state = searchViewModel.uiState.value
        assertTrue("Results should be empty when query is blank", state.results.isEmpty())
        assertFalse("Should not have searched", state.hasSearched)
    }
    
    // ========== 搜索功能测试 ==========
    
    @Test
    fun search_withValidQuery_updatesState() = runTest {
        val records = listOf(createTestRecord(1, "搜索结果", "内容"))
        `when`(repository.searchRecords("测试查询")).thenReturn(MutableStateFlow(records))
        
        searchViewModel.search("测试查询")
        
        val state = searchViewModel.uiState.value
        assertEquals("Query should be updated", "测试查询", state.query)
        assertTrue("Should have searched", state.hasSearched)
        assertFalse("Should not show history after search", state.showHistory)
        assertFalse("Should not be searching anymore", state.isSearching)
        assertEquals("Should have search results", 1, state.results.size)
    }
    
    @Test
    fun search_withBlankQuery_doesNothing() = runTest {
        searchViewModel.search("")
        
        val state = searchViewModel.uiState.value
        assertFalse("Should not have searched", state.hasSearched)
        assertTrue("Should still show history", state.showHistory)
    }
    
    @Test
    fun search_setsSearchingState() = runTest {
        val records = listOf(createTestRecord(1, "结果", "内容"))
        val flow = MutableStateFlow(emptyList<InputRecord>()) // 初始为空
        `when`(repository.searchRecords("测试")).thenReturn(flow)
        
        // 启动搜索
        val searchJob = kotlinx.coroutines.launch { searchViewModel.search("测试") }
        
        // 立即检查状态（在搜索完成前）
        var state = searchViewModel.uiState.value
        assertTrue("Should be searching", state.isSearching)
        assertTrue("Should have searched", state.hasSearched)
        assertFalse("Should not show history while searching", state.showHistory)
        
        // 模拟搜索完成
        flow.value = records
        
        searchJob.join() // 等待搜索完成
        
        state = searchViewModel.uiState.value
        assertFalse("Should finish searching", state.isSearching)
        assertEquals("Should have results", 1, state.results.size)
    }
    
    @Test
    fun search_addsToHistory() = runTest {
        val records = listOf(createTestRecord(1, "结果", "内容"))
        `when`(repository.searchRecords("历史查询")).thenReturn(MutableStateFlow(records))
        
        searchViewModel.search("历史查询")
        
        val state = searchViewModel.uiState.value
        assertTrue("Should add to history", state.searchHistory.contains("历史查询"))
    }
    
    @Test
    fun search_preventsDuplicatesInHistory() = runTest {
        val records = listOf(createTestRecord(1, "结果", "内容"))
        `when`(repository.searchRecords(anyString())).thenReturn(MutableStateFlow(records))
        
        searchViewModel.search("重复查询")
        searchViewModel.search("其他查询")
        searchViewModel.search("重复查询") // 重复搜索
        
        val state = searchViewModel.uiState.value
        assertEquals("Should prevent duplicates in history", 2, state.searchHistory.size)
        assertEquals("Most recent should be '重复查询'", "重复查询", state.searchHistory[0])
        assertEquals("Previous should be '其他查询'", "其他查询", state.searchHistory[1])
    }
    
    @Test
    fun search_limitsHistorySize() = runTest {
        `when`(repository.searchRecords(anyString())).thenReturn(MutableStateFlow(listOf(createTestRecord(1, "结果", "内容"))))
        
        // 搜索15次不同的查询
        repeat(15) { i ->
            searchViewModel.search("查询$i")
        }
        
        val state = searchViewModel.uiState.value
        assertTrue("Should limit history to 10", state.searchHistory.size <= 10)
        assertEquals("Should have exactly 10 history items", 10, state.searchHistory.size)
    }
    
    // ========== 从历史搜索测试 ==========
    
    @Test
    fun searchFromHistory_setsQueryAndSearches() = runTest {
        val records = listOf(createTestRecord(1, "历史结果", "内容"))
        `when`(repository.searchRecords("历史查询")).thenReturn(MutableStateFlow(records))
        
        searchViewModel.searchFromHistory("历史查询")
        
        val state = searchViewModel.uiState.value
        assertEquals("Query should be set to history query", "历史查询", state.query)
        assertTrue("Should have searched", state.hasSearched)
        assertFalse("Should not show history", state.showHistory)
    }
    
    // ========== 清除历史测试 ==========
    
    @Test
    fun clearHistory_clearsSearchHistory() = runTest {
        // 先添加一些历史记录
        val records = listOf(createTestRecord(1, "结果", "内容"))
        `when`(repository.searchRecords("测试")).thenReturn(MutableStateFlow(records))
        
        searchViewModel.search("测试1")
        searchViewModel.search("测试2")
        
        var state = searchViewModel.uiState.value
        assertFalse("Should have search history", state.searchHistory.isEmpty())
        
        searchViewModel.clearHistory()
        
        state = searchViewModel.uiState.value
        assertTrue("Should clear search history", state.searchHistory.isEmpty())
    }
    
    // ========== 清除搜索测试 ==========
    
    @Test
    fun clearSearch_resetsState() = runTest {
        // 先进行搜索
        val records = listOf(createTestRecord(1, "结果", "内容"))
        `when`(repository.searchRecords("测试")).thenReturn(MutableStateFlow(records))
        searchViewModel.search("测试")
        
        var state = searchViewModel.uiState.value
        assertFalse("Query should not be empty", state.query.isEmpty())
        assertTrue("Should have searched", state.hasSearched)
        assertFalse("Should not show history", state.showHistory)
        
        searchViewModel.clearSearch()
        
        state = searchViewModel.uiState.value
        assertEquals("Query should be empty", "", state.query)
        assertFalse("Should not have searched", state.hasSearched)
        assertTrue("Should show history", state.showHistory)
        assertTrue("Results should be empty", state.results.isEmpty())
    }
    
    // ========== 选择记录测试 ==========
    
    @Test
    fun selectRecord_updatesSelectedRecord() = runTest {
        val record = createTestRecord(1, "选中的记录", "内容")
        searchViewModel.selectRecord(record)
        
        assertEquals("Should select record", record, searchViewModel.uiState.value.selectedRecord)
    }
    
    @Test
    fun clearSelectedRecord_clearsSelection() = runTest {
        val record = createTestRecord(1, "选中的记录", "内容")
        searchViewModel.selectRecord(record)
        searchViewModel.clearSelectedRecord()
        
        assertNull("Should clear selected record", searchViewModel.uiState.value.selectedRecord)
    }
    
    // ========== 复制记录测试 ==========
    
    @Test
    fun copyRecord_copiesToClipboard() = runTest {
        val record = createTestRecord(1, "测试应用", "要复制的内容")
        
        searchViewModel.copyRecord(record)
        
        verify(clipboardManager).setPrimaryClip(any(ClipData::class.java))
        assertEquals("Should show copy message", "已复制到剪贴板", searchViewModel.uiState.value.message)
    }
    
    // ========== 删除记录测试 ==========
    
    @Test
    fun deleteRecord_successfullyDeletes() = runTest {
        val record = createTestRecord(1, "测试应用", "内容")
        `when`(repository.deleteRecord(1L)).thenReturn(true)
        
        searchViewModel.deleteRecord(record)
        
        verify(repository).deleteRecord(1L)
        assertNull("Should clear selected record", searchViewModel.uiState.value.selectedRecord)
        assertEquals("Should show delete message", "已删除", searchViewModel.uiState.value.message)
    }
    
    @Test
    fun deleteRecord_handlesException() = runTest {
        val record = createTestRecord(1, "测试应用", "内容")
        `when`(repository.deleteRecord(1L)).thenThrow(RuntimeException("删除失败"))
        
        searchViewModel.deleteRecord(record)
        
        assertTrue("Should show error", searchViewModel.uiState.value.error?.contains("删除失败") == true)
    }
    
    // ========== 清除消息测试 ==========
    
    @Test
    fun clearMessage_clearsBothMessageAndError() = runTest {
        // 先设置一个消息和错误
        val record = createTestRecord(1, "测试", "内容")
        `when`(repository.deleteRecord(1L)).thenThrow(RuntimeException("删除失败"))
        searchViewModel.deleteRecord(record)
        
        assertNotNull("Should have error", searchViewModel.uiState.value.error)
        
        searchViewModel.clearMessage()
        
        assertNull("Should clear message", searchViewModel.uiState.value.message)
        assertNull("Should clear error", searchViewModel.uiState.value.error)
    }
    
    // ========== 错误处理测试 ==========
    
    @Test
    fun search_handlesRepositoryError() = runTest {
        `when`(repository.searchRecords("错误测试")).thenThrow(RuntimeException("搜索失败"))
        
        searchViewModel.search("错误测试")
        
        val state = searchViewModel.uiState.value
        assertTrue("Should have error", state.error?.contains("搜索失败") == true)
        assertFalse("Should finish searching", state.isSearching)
    }
    
    // ========== 显示记录逻辑测试 ==========
    
    @Test
    fun displayRecords_showsSearchResultsWhenSearched() = runTest {
        val searchResults = listOf(createTestRecord(1, "搜索结果", "内容"))
        val recentRecords = listOf(createTestRecord(2, "最近记录", "内容"))
        
        `when`(repository.searchRecords("测试")).thenReturn(MutableStateFlow(searchResults))
        `when`(repository.getRecentRecords()).thenReturn(MutableStateFlow(recentRecords))
        
        searchViewModel = SearchViewModel(application)
        searchViewModel.repository = repository
        
        Thread.sleep(100) // 等待初始加载完成
        
        searchViewModel.search("测试")
        
        val state = searchViewModel.uiState.value
        val displayRecords = if (state.hasSearched) state.results else state.recentRecords
        assertEquals("Should show search results", 1, displayRecords.size)
        assertEquals("Should have search result", "搜索结果", displayRecords[0].content)
    }
    
    @Test
    fun displayRecords_showsRecentRecordsWhenNotSearched() = runTest {
        val recentRecords = listOf(createTestRecord(1, "最近记录", "内容"))
        `when`(repository.getRecentRecords()).thenReturn(MutableStateFlow(recentRecords))
        
        searchViewModel = SearchViewModel(application)
        searchViewModel.repository = repository
        
        Thread.sleep(100) // 等待初始加载完成
        
        val state = searchViewModel.uiState.value
        val displayRecords = if (state.hasSearched) state.results else state.recentRecords
        assertEquals("Should show recent records", 1, displayRecords.size)
        assertEquals("Should have recent record", "最近记录", displayRecords[0].content)
    }
    
    // ========== 辅助方法 ==========
    
    private fun createTestRecord(
        id: Long = 0,
        application: String = "com.test.app",
        content: String = "测试内容",
        createdAt: Long = System.currentTimeMillis()
    ): InputRecord {
        return InputRecord(
            id = id,
            content = content,
            application = application,
            category = null,
            visibility = Visibility.PUBLIC,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }
}