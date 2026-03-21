package com.syncrime.app.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Debug 悬浮按钮组件
 */
@Composable
fun DebugFloatingButton(
    onClearCache: () -> Unit = {}
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showErrorLogDialog by remember { mutableStateOf(false) }
    var currentLog by remember { mutableStateOf("") }
    var logTitle by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 悬浮按钮
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .shadow(8.dp, CircleShape),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Icon(
                if (expanded) Icons.Default.Close else Icons.Default.BugReport,
                contentDescription = "Debug Menu"
            )
        }
        
        // 展开的菜单
        if (expanded) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
                    .width(200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 抓日志
                    DebugMenuItem(
                        icon = Icons.Default.Article,
                        text = "抓日志",
                        onClick = {
                            expanded = false
                            currentLog = captureLogs(context, filterError = false)
                            logTitle = "系统日志"
                            showLogDialog = true
                        }
                    )
                    
                    // 抓错误日志
                    DebugMenuItem(
                        icon = Icons.Default.Error,
                        text = "抓错误日志",
                        onClick = {
                            expanded = false
                            currentLog = captureLogs(context, filterError = true)
                            logTitle = "错误日志"
                            showErrorLogDialog = true
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // 清缓存
                    DebugMenuItem(
                        icon = Icons.Default.DeleteSweep,
                        text = "清缓存",
                        onClick = {
                            expanded = false
                            onClearCache()
                            clearAppCache(context)
                            Toast.makeText(context, "缓存已清理", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
    
    // 日志查看对话框
    if (showLogDialog || showErrorLogDialog) {
        LogViewDialog(
            title = logTitle,
            logContent = currentLog,
            onDismiss = {
                showLogDialog = false
                showErrorLogDialog = false
            }
        )
    }
}

/**
 * Debug 菜单项
 */
@Composable
private fun DebugMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 日志查看对话框
 */
@Composable
private fun LogViewDialog(
    title: String,
    logContent: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 $title",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                
                Divider()
                
                // 日志内容
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if (logContent.isBlank()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无日志",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = logContent,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
                
                Divider()
                
                // 操作按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 复制到剪贴板
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("SyncRime Log", logContent)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = logContent.isNotBlank()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("复制")
                    }
                    
                    // 关闭
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

/**
 * 抓取日志
 */
private fun captureLogs(context: Context, filterError: Boolean): String {
    return try {
        val process = Runtime.getRuntime().exec("logcat -d -v time")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?
        
        val filterTags = listOf(
            "SyncRime",
            "InputCaptureService",
            "InputProcessor",
            "InputRepository",
            "AI",
            "SyncRimeDatabase",
            "AndroidRuntime"
        )
        
        while (reader.readLine().also { line = it } != null) {
            val logLine = line ?: continue
            
            // 过滤相关标签
            val isRelevant = filterTags.any { tag -> 
                logLine.contains(tag, ignoreCase = true) 
            }
            
            // 如果是错误日志，只显示错误和警告
            val isErrorLog = if (filterError) {
                logLine.contains(" E/") || logLine.contains(" W/") || logLine.contains("Error") || logLine.contains("Exception")
            } else {
                true
            }
            
            if (isRelevant && isErrorLog) {
                output.append(logLine).append("\n")
            }
        }
        
        reader.close()
        
        val result = output.toString()
        if (result.isBlank()) {
            if (filterError) "暂无错误日志" else "暂无日志"
        } else {
            // 截取最近 500 行
            val lines = result.lines().takeLast(500)
            lines.joinToString("\n")
        }
    } catch (e: Exception) {
        "抓取日志失败: ${e.message}"
    }
}

/**
 * 清理应用缓存
 */
private fun clearAppCache(context: Context) {
    try {
        // 清理内部缓存
        val cacheDir = context.cacheDir
        if (cacheDir.exists()) {
            deleteDir(cacheDir)
        }
        
        // 清理外部缓存
        val externalCacheDir = context.externalCacheDir
        if (externalCacheDir != null && externalCacheDir.exists()) {
            deleteDir(externalCacheDir)
        }
        
        // 清理代码缓存
        val codeCacheDir = context.codeCacheDir
        if (codeCacheDir.exists()) {
            deleteDir(codeCacheDir)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 递归删除目录
 */
private fun deleteDir(dir: File): Boolean {
    if (dir.isDirectory) {
        dir.listFiles()?.forEach { file ->
            deleteDir(file)
        }
    }
    return dir.delete()
}