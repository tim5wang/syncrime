package com.syncrime.app.presentation.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.shared.model.SourceType
import java.text.SimpleDateFormat
import java.util.*

/**
 * 知识剪藏详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipDetailScreen(
    clip: KnowledgeClip,
    onBack: () -> Unit,
    onEdit: (KnowledgeClip) -> Unit,
    onDelete: (Long) -> Unit,
    onIncrementView: (Long) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(clip.id) {
        onIncrementView(clip.id)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("剪藏详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = clip.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text(getSourceTypeLabel(clip.sourceType)) },
                            leadingIcon = {
                                Icon(
                                    getSourceTypeIcon(clip.sourceType),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        clip.category?.let { category ->
                            AssistChip(
                                onClick = {},
                                label = { Text(category) }
                            )
                        }
                    }
                }
            }
            
            // 内容卡片
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "内容",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = clip.content,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            // AI摘要卡片
            clip.summary?.let { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI 摘要",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // 标签卡片
            if (clip.tags.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "标签",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 使用 Row 代替 FlowRow 避免实验性 API
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            clip.tags.take(5).forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            }
            
            // 统计信息
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "统计",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.Visibility,
                            label = "查看",
                            value = clip.viewCount.toString()
                        )
                        StatItem(
                            icon = Icons.Default.Favorite,
                            label = "收藏",
                            value = clip.favoriteCount.toString()
                        )
                    }
                }
            }
            
            // 时间信息
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "时间信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeInfoRow("创建时间", clip.createdAt)
                    if (clip.updatedAt != clip.createdAt) {
                        TimeInfoRow("更新时间", clip.updatedAt)
                    }
                }
            }
            
            // 底部操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("编辑")
                }
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除")
                }
            }
        }
    }
    
    // 编辑对话框
    if (showEditDialog) {
        EditClipDialog(
            clip = clip,
            onDismiss = { showEditDialog = false },
            onSave = { updatedClip ->
                onEdit(updatedClip)
                showEditDialog = false
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${clip.title}」吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(clip.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 编辑剪藏对话框
 */
@Composable
fun EditClipDialog(
    clip: KnowledgeClip,
    onDismiss: () -> Unit,
    onSave: (KnowledgeClip) -> Unit
) {
    var title by remember { mutableStateOf(clip.title) }
    var content by remember { mutableStateOf(clip.content) }
    var category by remember { mutableStateOf(clip.category ?: "") }
    var tags by remember { mutableStateOf(clip.tags.joinToString(", ")) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑剪藏") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 6
                )
                
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("标签（逗号分隔）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Label, contentDescription = null)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedClip = clip.copy(
                        title = title,
                        content = content,
                        category = category.ifBlank { null },
                        tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(updatedClip)
                },
                enabled = title.isNotBlank() && content.isNotBlank()
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
 * 统计项
 */
@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 时间信息行
 */
@Composable
fun TimeInfoRow(label: String, timestamp: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatDateTime(timestamp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 获取来源类型标签
 */
fun getSourceTypeLabel(type: SourceType): String = when (type) {
    SourceType.INPUT -> "输入"
    SourceType.CLIP -> "剪藏"
    SourceType.SHARE -> "分享"
    SourceType.IMPORT -> "导入"
}

/**
 * 获取来源类型图标
 */
@Composable
fun getSourceTypeIcon(type: SourceType) = when (type) {
    SourceType.INPUT -> Icons.Default.Edit
    SourceType.CLIP -> Icons.Default.ContentCopy
    SourceType.SHARE -> Icons.Default.Share
    SourceType.IMPORT -> Icons.Default.Download
}

/**
 * 格式化日期时间
 */
fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
