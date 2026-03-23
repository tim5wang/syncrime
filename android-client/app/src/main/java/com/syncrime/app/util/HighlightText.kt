package com.syncrime.app.util

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

/**
 * 高亮显示文本中的关键词
 * @param text 原始文本
 * @param query 搜索关键词
 * @param modifier 修饰符
 * @param textStyle 文本样式
 * @param highlightColor 高亮背景色
 * @param onClick 点击回调
 */
@Composable
fun HighlightableText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    highlightColor: Color = androidx.compose.ui.graphics.Color.Yellow.copy(alpha = 0.4f),
    onClick: ((AnnotatedString.Range<String>) -> Unit)? = null
) {
    val annotatedString = if (query.isNotEmpty()) {
        TextHighlight.highlightText(text, query, highlightColor)
    } else {
        AnnotatedString(text)
    }
    
    if (onClick != null) {
        ClickableText(
            text = annotatedString,
            modifier = modifier,
            style = textStyle,
            onClick = { offset ->
                annotatedString.getStringAnnotations(start = offset, end = offset).firstOrNull()?.let {
                    onClick(it)
                }
            }
        )
    } else {
        Text(
            text = annotatedString,
            modifier = modifier,
            style = textStyle
        )
    }
}

/**
 * 高亮显示文本中的多个关键词
 * @param text 原始文本
 * @param queries 搜索关键词列表
 * @param modifier 修饰符
 * @param textStyle 文本样式
 * @param highlightColor 高亮背景色
 * @param onClick 点击回调
 */
@Composable
fun HighlightableMultipleText(
    text: String,
    queries: List<String>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    highlightColor: Color = androidx.compose.ui.graphics.Color.Yellow.copy(alpha = 0.4f),
    onClick: ((AnnotatedString.Range<String>) -> Unit)? = null
) {
    val filteredQueries = queries.filter { it.isNotEmpty() }
    val annotatedString = if (filteredQueries.isNotEmpty()) {
        TextHighlight.highlightMultiple(text, filteredQueries, highlightColor)
    } else {
        AnnotatedString(text)
    }
    
    if (onClick != null) {
        ClickableText(
            text = annotatedString,
            modifier = modifier,
            style = textStyle,
            onClick = { offset ->
                annotatedString.getStringAnnotations(start = offset, end = offset).firstOrNull()?.let {
                    onClick(it)
                }
            }
        )
    } else {
        Text(
            text = annotatedString,
            modifier = modifier,
            style = textStyle
        )
    }
}