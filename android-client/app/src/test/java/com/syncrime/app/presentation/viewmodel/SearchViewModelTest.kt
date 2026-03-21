package com.syncrime.app.presentation.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.syncrime.inputmethod.repository.ClipRepository
import com.syncrime.inputmethod.repository.InputRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * SearchViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var inputRepository: InputRepository
    
    @Mock
    private lateinit var clipRepository: ClipRepository
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var sharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var editor: SharedPreferences.Editor
    
    private lateinit var searchViewModel: SearchViewModel
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.getStringSet(anyString(), any())).thenReturn(emptySet())
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putStringSet(anyString(), any())).thenReturn(editor)
        
        `when`(inputRepository.searchRecords(anyString())).thenReturn(flowOf(emptyList()))
        `when`(clipRepository.searchClips(anyString())).thenReturn(flowOf(emptyList()))
        
        searchViewModel = SearchViewModel(inputRepository, clipRepository, context)
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
        assertTrue("Input results should be empty", state.inputResults.isEmpty())
        assertTrue("Clip results should be empty", state.clipResults.isEmpty())
        assertFalse("Should not have searched", state.hasSearched)
        assertTrue("Should show history initially", state.showHistory)
    }
    
    // ========== 搜索测试 ==========
    
    @Test
    fun search_withValidQuery_updatesState() = runTest {
        searchViewModel.search("测试查询")
        
        val state = searchViewModel.uiState.value
        assertEquals("Query should be updated", "测试查询", state.query)
        assertTrue("Should have searched", state.hasSearched)
        assertFalse("Should not show history after search", state.showHistory)
    }
    
    @Test
    fun search_withEmptyQuery_clearsResults() = runTest {
        searchViewModel.search("测试")
        searchViewModel.search("")
        
        val state = searchViewModel.uiState.value
        assertFalse("Should not have searched", state.hasSearched)
        assertTrue("Results should be empty", state.inputResults.isEmpty())
        assertTrue("Should show history", state.showHistory)
    }
    
    // ========== 搜索历史测试 ==========
    
    @Test
    fun search_savesToHistory() = runTest {
        searchViewModel.search("历史记录测试")
        verify(editor).putStringSet(anyString(), any())
    }
    
    @Test
    fun clearSearchHistory_clearsHistory() = runTest {
        searchViewModel.clearSearchHistory()
        verify(editor).remove(anyString())
        assertTrue("History should be empty", searchViewModel.uiState.value.searchHistory.isEmpty())
    }
    
    // ========== 清除搜索测试 ==========
    
    @Test
    fun clearSearch_resetsState() = runTest {
        searchViewModel.search("测试")
        searchViewModel.clearSearch()
        
        val state = searchViewModel.uiState.value
        assertEquals("Query should be empty", "", state.query)
        assertFalse("Should not have searched", state.hasSearched)
        assertTrue("Should show history", state.showHistory)
    }
    
    // ========== 设置查询测试 ==========
    
    @Test
    fun setQuery_updatesQueryState() = runTest {
        searchViewModel.setQuery("新查询")
        assertEquals("Query should be updated", "新查询", searchViewModel.uiState.value.query)
    }
    
    @Test
    fun setQuery_emptyShowsHistory() = runTest {
        searchViewModel.search("测试")
        searchViewModel.setQuery("")
        assertTrue("Should show history when query is empty", searchViewModel.uiState.value.showHistory)
    }
}