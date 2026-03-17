package com.syncrime.android.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 智能推荐卡片
 * 显示实时输入推荐
 */
@Composable
fun SmartRecommendationCard(
    recommendations: List<RecommendationUiModel>,
    onRecommendationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible && recommendations.isNotEmpty(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "智能推荐",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recommendations) { recommendation ->
                        RecommendationChip(
                            text = recommendation.text,
                            confidence = recommendation.confidence,
                            onClick = { onRecommendationSelected(recommendation.text) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 推荐标签
 */
@Composable
private fun RecommendationChip(
    text: String,
    confidence: Float,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            
            if (confidence > 0.8f) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 推荐数据模型
 */
data class RecommendationUiModel(
    val text: String,
    val confidence: Float,
    val type: String = "general"
)

/**
 * 输入状态指示器
 */
@Composable
fun InputStatusIndicator(
    isCapturing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isCapturing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            ) {
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = if (isCapturing) "采集中" else "未采集",
            style = MaterialTheme.typography.bodySmall,
            color = if (isCapturing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            fontStyle = if (isCapturing) FontStyle.Normal else FontStyle.Italic
        )
    }
}

/**
 * 智能分析提示
 */
@Composable
fun IntelligenceHint(
    analysisResult: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = analysisResult.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = analysisResult,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
