package com.syncrime.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.syncrime.app.ai.model.ChatMessage
import com.syncrime.app.ai.viewmodel.AIViewModel

/**
 * AI 功能标签页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITab(aiViewModel: AIViewModel) {
    val uiState by aiViewModel.uiState.collectAsState()
    var selectedMode by remember { mutableStateOf(AIMode.CONTINUE) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "✨ AI 助手",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 配置状态提示
        if (!uiState.isConfigured) {
            AISettingsCard(aiViewModel)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 模式选择
        AIModeSelector(selectedMode) { selectedMode = it }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 根据模式显示不同内容
        when (selectedMode) {
            AIMode.CONTINUE -> ContinueWritingPanel(aiViewModel, uiState)
            AIMode.SUMMARIZE -> SummarizePanel(aiViewModel, uiState)
            AIMode.CHAT -> ChatPanel(aiViewModel, uiState)
            AIMode.POLISH -> PolishPanel(aiViewModel, uiState)
        }
    }
}

/**
 * AI 功能模式
 */
enum class AIMode {
    CONTINUE,    // 智能续写
    SUMMARIZE,   // 智能摘要
    CHAT,        // AI 对话
    POLISH       // 文本润色
}

/**
 * AI 模式选择器
 */
@Composable
fun AIModeSelector(
    selectedMode: AIMode,
    onModeSelected: (AIMode) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedMode.ordinal,
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AIMode.values().forEach { mode ->
            Tab(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                text = { Text(mode.displayName) }
            )
        }
    }
}

val AIMode.displayName: String
    get() = when (this) {
        AIMode.CONTINUE -> "✍️ 续写"
        AIMode.SUMMARIZE -> "📝 摘要"
        AIMode.CHAT -> "💬 对话"
        AIMode.POLISH -> "🎨 润色"
    }

/**
 * AI 设置卡片（未配置时显示）
 */
@Composable
fun AISettingsCard(aiViewModel: AIViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚙️ 配置 AI 服务",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "需要阿里云百炼 API Key 才能使用 AI 功能",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Button(onClick = { showDialog = true }) {
                    Text("配置")
                }
            }
        }
    }
    
    if (showDialog) {
        AIConfigDialog(
            onConfirm = { key ->
                aiViewModel.configureApiKey(key)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * AI 配置对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfigDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置 AI 服务") },
        text = {
            Column {
                Text("请输入阿里云百炼 API Key：")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "可在阿里云百炼控制台获取：https://bailian.console.aliyun.com/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 智能续写面板
 */
@Composable
fun ContinueWritingPanel(aiViewModel: AIViewModel, uiState: AIViewModel.AIUiState) {
    var context by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = context,
            onValueChange = { context = it },
            label = { Text("上下文（可选）") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            placeholder = { Text("输入相关上下文，帮助 AI 更好地理解场景") }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = prefix,
            onValueChange = { prefix = it },
            label = { Text("已输入内容") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            placeholder = { Text("输入你想续写的内容开头") }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { aiViewModel.continueWriting(context, prefix) },
            modifier = Modifier.fillMaxWidth(),
            enabled = prefix.isNotBlank() && !uiState.isProcessing
        ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 创作中...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("智能续写")
            }
        }
        
        // 显示结果
        if (uiState.currentResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            AIResultCard(
                result = uiState.currentResult,
                onCopy = { /* TODO: 复制到剪贴板 */ },
                onClear = { aiViewModel.clearResult() }
            )
        }
    }
}

/**
 * 智能摘要面板
 */
@Composable
fun SummarizePanel(aiViewModel: AIViewModel, uiState: AIViewModel.AIUiState) {
    var content by remember { mutableStateOf("") }
    var summaryLength by remember { mutableStateOf("100") }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("原文内容") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("输入需要摘要的长文本") },
            maxLines = 10
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("摘要长度：")
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = summaryLength,
                onValueChange = { summaryLength = it.filter { char -> char.isDigit() } },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
            )
            Text(" 字", modifier = Modifier.padding(start = 4.dp))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = {
                aiViewModel.summarize(content, summaryLength.toIntOrNull() ?: 100)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = content.isNotBlank() && !uiState.isProcessing
        ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 摘要中...")
            } else {
                Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("智能摘要")
            }
        }
        
        // 显示结果
        if (uiState.currentResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            AIResultCard(
                result = uiState.currentResult,
                onCopy = { /* TODO: 复制到剪贴板 */ },
                onClear = { aiViewModel.clearResult() }
            )
        }
    }
}

/**
 * AI 对话面板
 */
@Composable
fun ChatPanel(aiViewModel: AIViewModel, uiState: AIViewModel.AIUiState) {
    val chatMessages = uiState.chatMessages
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 对话历史
        if (chatMessages.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatMessages) { message ->
                    ChatMessageBubble(message)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "开始与 AI 对话吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 输入框
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = uiState.chatInput,
                onValueChange = { aiViewModel.setChatInput(it) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 120.dp),
                placeholder = { Text("输入消息...") },
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (uiState.chatInput.isNotBlank()) {
                            aiViewModel.sendChatMessage(uiState.chatInput)
                        }
                    }
                ),
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (uiState.chatInput.isNotBlank()) {
                        aiViewModel.sendChatMessage(uiState.chatInput)
                    }
                },
                enabled = uiState.chatInput.isNotBlank() && !uiState.isProcessing
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 对话消息气泡
 */
@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatMessage.Role.USER
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = if (isUser) "你" else "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }
    }
}

/**
 * 文本润色面板
 */
@Composable
fun PolishPanel(aiViewModel: AIViewModel, uiState: AIViewModel.AIUiState) {
    var content by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("professional") }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("需要润色的文本") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("输入想要润色的内容") },
            maxLines = 10
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 风格选择
        Text("选择风格：", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStyle == "professional",
                onClick = { selectedStyle = "professional" },
                label = { Text("💼 专业") }
            )
            FilterChip(
                selected = selectedStyle == "formal",
                onClick = { selectedStyle = "formal" },
                label = { Text("🎩 正式") }
            )
            FilterChip(
                selected = selectedStyle == "casual",
                onClick = { selectedStyle = "casual" },
                label = { Text("😊 轻松") }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { aiViewModel.polish(content, selectedStyle) },
            modifier = Modifier.fillMaxWidth(),
            enabled = content.isNotBlank() && !uiState.isProcessing
        ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 润色中...")
            } else {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("文本润色")
            }
        }
        
        // 显示结果
        if (uiState.currentResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            AIResultCard(
                result = uiState.currentResult,
                onCopy = { /* TODO: 复制到剪贴板 */ },
                onClear = { aiViewModel.clearResult() }
            )
        }
    }
}

/**
 * AI 结果卡片
 */
@Composable
fun AIResultCard(
    result: String,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✨ AI 结果",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Row {
                    IconButton(onClick = onCopy) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "清除",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            )
        }
    }
}
