package com.syncrime.app.presentation

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日志查看器（Debug 功能）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogViewer() {
    var logs by remember { mutableStateOf(emptyList<LogEntry>()) }
    var showDialog by remember { mutableStateOf(false) }
    
    // 模拟日志数据
    LaunchedEffect(Unit) {
        logs = listOf(
            LogEntry("InputCaptureService", "无障碍服务已连接", Log.INFO),
            LogEntry("InputCaptureService", "收到事件：TYPE_VIEW_TEXT_CHANGED, 包名：com.android.browser", Log.DEBUG),
            LogEntry("InputCaptureService", "保存输入记录：123 字符", Log.INFO),
            LogEntry("HomeViewModel", "加载统计数据", Log.DEBUG),
            LogEntry("AIService", "AI 服务已配置，模型：qwen-plus", Log.INFO)
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🔍 日志查看器 (Debug)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ℹ️ 使用说明",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "如果遇到无障碍服务无法开启的问题，请：\n\n" +
                           "1. 截图此页面的日志\n" +
                           "2. 发送到技术支持\n" +
                           "3. 我们会根据日志分析问题",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查看完整日志")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 最近日志预览
        Text(
            text = "最近日志（预览）",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs.take(10)) { log ->
                LogItem(log)
            }
        }
    }
    
    if (showDialog) {
        FullLogDialog(logs = logs, onDismiss = { showDialog = false })
    }
}

data class LogEntry(
    val tag: String,
    val message: String,
    val level: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun LogItem(log: LogEntry) {
    val bgColor = when (log.level) {
        Log.ERROR -> MaterialTheme.colorScheme.errorContainer
        Log.WARN -> MaterialTheme.colorScheme.tertiaryContainer
        Log.INFO -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val levelText = when (log.level) {
        Log.ERROR -> "❌ ERROR"
        Log.WARN -> "⚠️ WARN"
        Log.INFO -> "ℹ️ INFO"
        Log.DEBUG -> "📝 DEBUG"
        else -> "   "
    }
    
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = timeFormat.format(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = levelText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = log.tag,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(150.dp)
            )
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FullLogDialog(logs: List<LogEntry>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("完整日志") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    LogItem(log)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
