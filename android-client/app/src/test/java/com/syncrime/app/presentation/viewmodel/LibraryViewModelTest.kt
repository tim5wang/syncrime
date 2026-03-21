package com.syncrime.app.presentation.viewmodel

import com.syncrime.inputmethod.repository.ClipRepository
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.shared.model.SourceType
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
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * LibraryViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var clipRepository: ClipRepository
    
    private lateinit var libraryViewModel: LibraryViewModel
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        `when`(clipRepository.getAllClips(anyInt(), anyInt())).thenReturn(flowOf(emptyList()))
        `when`(clipRepository.getClipsByCategory(anyString())).thenReturn(flowOf(emptyList()))
        
        libraryViewModel = LibraryViewModel(clipRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun initialState_isCorrect() = runTest {
        val state = libraryViewModel.uiState.value
        assertTrue("Initial clips should be empty", state.clips.isEmpty())
        assertNull("No category should be selected", state.selectedCategory)
    }
    
    @Test
    fun loadClips_callsRepository() = runTest {
        verify(clipRepository).getAllClips(anyInt(), anyInt())
    }
    
    @Test
    fun filterByCategory_updatesState() = runTest {
        libraryViewModel.filterByCategory("personal")
        val state = libraryViewModel.uiState.value
        assertEquals("Category should be updated", "personal", state.selectedCategory)
    }
    
    @Test
    fun deleteClip_callsRepository() = runTest {
        val clip = createTestClip(id = 1, title = "测试")
        libraryViewModel.deleteClip(clip)
        verify(clipRepository).deleteClip(clip)
    }
    
    @Test
    fun toggleFavorite_callsRepository() = runTest {
        val clip = createTestClip(id = 1, title = "测试")
        libraryViewModel.toggleFavorite(clip)
        verify(clipRepository).incrementFavoriteCount(1L)
    }
    
    private fun createTestClip(
        id: Long = 0,
        title: String = "测试剪藏",
        content: String = "测试内容"
    ): KnowledgeClip {
        return KnowledgeClip(
            id = id,
            title = title,
            content = content,
            summary = null,
            category = null,
            tags = emptyList(),
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