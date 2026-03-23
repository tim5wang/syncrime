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
            // 使用大小写不敏感的匹配
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
                
                // 添加高亮文本，保留原文本的大小写
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
                
                // 添加普通文本
                if (foundIndex > currentIndex) {
                    append(text.substring(currentIndex, foundIndex))
                }
                
                // 添加高亮文本（带粗体）
                withStyle(style = SpanStyle(
                    background = highlightColor, 
                    fontWeight = FontWeight.Bold
                )) {
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
     * 高亮文本中的关键词（支持多个关键词）
     * @param text 原始文本
     * @param queries 搜索关键词列表
     * @param highlightColor 高亮颜色
     * @return 带有高亮样式的 AnnotatedString
     */
    fun highlightMultiple(
        text: String,
        queries: List<String>,
        highlightColor: Color = Color.Yellow.copy(alpha = 0.4f)
    ): AnnotatedString {
        if (queries.isEmpty() || queries.all { it.isBlank() }) return AnnotatedString(text)
        
        return buildAnnotatedString {
            val lowerText = text.lowercase()
            val sortedQueries = queries.filter { it.isNotBlank() }
                .sortedByDescending { it.length } // 按长度排序，优先匹配较长的关键词
            
            val highlights = mutableListOf<Pair<Int, Int>>() // 存储高亮的起始和结束位置
            
            // 为每个关键词寻找匹配位置
            for (query in sortedQueries) {
                val lowerQuery = query.lowercase()
                var searchStartIndex = 0
                
                while (searchStartIndex <= lowerText.length - lowerQuery.length) {
                    val foundIndex = lowerText.indexOf(lowerQuery, searchStartIndex)
                    if (foundIndex == -1) break
                    
                    // 检查是否与已有高亮重叠
                    var shouldAdd = true
                    for ((start, end) in highlights) {
                        if (foundIndex < end && foundIndex + query.length > start) {
                            shouldAdd = false
                            break
                        }
                    }
                    
                    if (shouldAdd) {
                        highlights.add(foundIndex to foundIndex + query.length)
                    }
                    
                    searchStartIndex = foundIndex + 1
                }
            }
            
            // 按位置排序高亮区域
            highlights.sortBy { it.first }
            
            var currentIndex = 0
            for ((start, end) in highlights) {
                // 添加普通文本
                if (start > currentIndex) {
                    append(text.substring(currentIndex, start))
                }
                
                // 添加高亮文本
                withStyle(style = SpanStyle(background = highlightColor)) {
                    append(text.substring(start, end))
                }
                
                currentIndex = end
            }
            
            // 添加剩余文本
            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }
    
    /**
     * 高亮文本中的关键词（支持正则表达式）
     * @param text 原始文本
     * @param regex 正则表达式
     * @param highlightColor 高亮颜色
     * @return 带有高亮样式的 AnnotatedString
     */
    fun highlightRegex(
        text: String,
        regex: Regex,
        highlightColor: Color = Color.Yellow.copy(alpha = 0.4f)
    ): AnnotatedString {
        val matches = regex.findAll(text)
        val highlights = matches.map { it.range.first to it.range.last + 1 }.toList()
        
        if (highlights.isEmpty()) return AnnotatedString(text)
        
        return buildAnnotatedString {
            var currentIndex = 0
            
            for ((start, end) in highlights) {
                // 添加普通文本
                if (start > currentIndex) {
                    append(text.substring(currentIndex, start))
                }
                
                // 添加高亮文本
                withStyle(style = SpanStyle(background = highlightColor)) {
                    append(text.substring(start, end))
                }
                
                currentIndex = end
            }
            
            // 添加剩余文本
            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }
}
