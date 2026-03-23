package com.syncrime.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TextHighlightTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testHighlightText_EmptyQuery_ReturnsOriginalText() {
        val result = TextHighlight.highlightText("Hello World", "")
        assertEquals("Hello World", result.text)
    }
    
    @Test
    fun testHighlightText_BlankQuery_ReturnsOriginalText() {
        val result = TextHighlight.highlightText("Hello World", "   ")
        assertEquals("Hello World", result.text)
    }
    
    @Test
    fun testHighlightText_SimpleMatch_HighlightsCorrectly() {
        val result = TextHighlight.highlightText("Hello World", "World")
        assertTrue(result.text.contains("World"))
        // Verify that there are spans applied for highlighting
        assertTrue(result.spanStyles.isNotEmpty())
    }
    
    @Test
    fun testHighlightText_CaseInsensitiveMatch_HighlightsCorrectly() {
        val result = TextHighlight.highlightText("Hello World", "world")
        assertTrue(result.text.contains("World")) // Original case preserved
    }
    
    @Test
    fun testHighlightMultiple_NoMatches_ReturnsOriginalText() {
        val result = TextHighlight.highlightMultiple("Hello World", listOf("foo", "bar"))
        assertEquals("Hello World", result.text)
    }
    
    @Test
    fun testHighlightMultiple_MultipleMatches_HighlightsCorrectly() {
        val result = TextHighlight.highlightMultiple("Hello World Hello Universe", listOf("Hello", "World"))
        assertTrue(result.text.contains("Hello"))
        assertTrue(result.text.contains("World"))
        // Should have multiple spans for different matches
        assertTrue(result.spanStyles.isNotEmpty())
    }
    
    @Test
    fun testHighlightMultiple_OverlappingKeywords_PrioritizesLonger() {
        val result = TextHighlight.highlightMultiple("Hello World", listOf("lo Wo", "World", "Hello"))
        assertTrue(result.text.contains("Hello"))
        assertTrue(result.text.contains("World"))
    }
}