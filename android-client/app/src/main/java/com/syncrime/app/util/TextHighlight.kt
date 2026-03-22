package com.syncrime.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * 文本高亮工具类
 */
object TextHighlight {
    
    /**
     * 高亮文本中的关键词
     * @param text 原始文本
     * @param query 搜索关键词
     * @param highlightColor 高亮颜色
     * @return 带有高亮样式的 AnnotatedString
     */
    fun highlightText(
        text: String,
        query: String,
        highlightColor: Color = Color.Yellow.copy(alpha = 0.4f)
    ): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(text)
        
        return buildAnnotatedString {
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            var currentIndex = 0
            
            var searchStartIndex = 0
            while (searchStartIndex <= lowerText.length - lowerQuery.length) {
                val foundIndex = lowerText.indexOf(lowerQuery, searchStartIndex)
                if (foundIndex == -1) break
                
                // 添加普通文本
                if (foundIndex > currentIndex) {
                    append(text.substring(currentIndex, foundIndex))
                }
                
                // 添加高亮文本
                withStyle(style = SpanStyle(background = highlightColor)) {
                    append(text.substring(foundIndex, foundIndex + query.length))
                }
                
                currentIndex = foundIndex + query.length
                searchStartIndex = foundIndex + 1
            }
            
            // 添加剩余文本
            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }
    
    /**
     * 高亮文本中的关键词（带粗体样式）
     * @param text 原始文本
     * @param query 搜索关键词
     * @param highlightColor 高亮颜色
     * @return 带有高亮和粗体样式的 AnnotatedString
     */
    fun highlightTextWithBold(
        text: String,
        query: String,
        highlightColor: Color = Color.Yellow.copy(alpha = 0.4f)
    ): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(text)
        
        return buildAnnotatedString {
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            var currentIndex = 0
            
            var searchStartIndex = 0
            while (searchStartIndex <= lowerText.length - lowerQuery.length) {
                val foundIndex = lowerText.indexOf(lowerQuery, searchStartIndex)
                if (foundIndex == -1) break
                
                // 添加普通文本（带粗体）
                if (foundIndex > currentIndex) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(currentIndex, foundIndex))
                    }
                }
                
                // 添加高亮文本（带粗体）
                withStyle(style = SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)) {
                    append(text.substring(foundIndex, foundIndex + query.length))
                }
                
                currentIndex = foundIndex + query.length
                searchStartIndex = foundIndex + 1
            }
            
            // 添加剩余文本（带粗体）
            if (currentIndex < text.length) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(currentIndex))
                }
            }
        }
    }
}
