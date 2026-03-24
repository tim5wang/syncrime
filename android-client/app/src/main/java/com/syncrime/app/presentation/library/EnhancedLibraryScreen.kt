package com.syncrime.app.presentation.library

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.syncrime.app.presentation.formatTime
import com.syncrime.app.presentation.viewmodel.LibraryViewModel
import com.syncrime.shared.model.KnowledgeClip

/**
 * 增强版知识库界面
 * 包含分类、标签、过滤和搜索功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedLibraryScreen(
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 刷新剪贴板
    LaunchedEffect(Unit) { viewModel.loadClipboardHistory() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📚 知识库",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // 过滤器区域
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔍 过滤",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 分类过滤
                var expandedCategory by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    TextField(
                        value = uiState.filterCategory ?: "所有分类",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                imageVector = if (expandedCategory) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("所有分类") },
                            onClick = {
                                viewModel.setFilterCategory(null)
                                expandedCategory = false
                            }
                        )
                        uiState.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    viewModel.setFilterCategory(category)
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 标签过滤
                var expandedTag by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedTag,
                    onExpandedChange = { expandedTag = !expandedTag }
                ) {
                    TextField(
                        value = uiState.filterTag ?: "所有标签",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                imageVector = if (expandedTag) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = expandedTag,
                        onDismissRequest = { expandedTag = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("所有标签") },
                            onClick = {
                                viewModel.setFilterTag(null)
                                expandedTag = false
                            }
                        )
                        uiState.tags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = {
                                    viewModel.setFilterTag(tag)
                                    expandedTag = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 清除过滤
                if (uiState.filterCategory != null || uiState.filterTag != null) {
                    Button(
                        onClick = { viewModel.clearFilters() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text("清除过滤")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 搜索框
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { 
                viewModel.setSearchQuery(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索知识库...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton({ 
                        viewModel.setSearchQuery("")
                    }) { 
                        Icon(Icons.Default.Clear, null) 
                    }
                }
            },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 剪贴板内容
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 剪贴板",
                        fontWeight = FontWeight.Bold
                    )
                    IconButton({ viewModel.loadClipboardHistory() }) { 
                        Icon(Icons.Default.Refresh, contentDescription = "刷新") 
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.recentClipboard.isEmpty()) {
                    Text(
                        text = "复制内容后将显示在这里",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(150.dp)
                    ) {
                        items(uiState.recentClipboard) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.text.take(30) + if (item.text.length > 30) "..." else "",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton({ 
                                    viewModel.addToClip(item.text) 
                                }) { 
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "添加到剪藏",
                                        modifier = Modifier.size(20.dp)
                                    ) 
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 已剪藏内容
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📌 我的剪藏 (${uiState.filteredClips.size})",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        if (uiState.filteredClips.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("暂无剪藏内容")
                    Text(
                        "从上方剪贴板添加",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn {
                items(uiState.filteredClips) { clip ->
                    ClipItemCard(
                        clip = clip,
                        onEdit = { viewModel.startEdit(it) },
                        onDelete = { viewModel.deleteClip(it.id) },
                        onSelectCategory = { viewModel.setFilterCategory(it) },
                        onSelectTag = { viewModel.setFilterTag(it) }
                    )
                }
            }
        }
        
        // 消息提示
        uiState.message?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp)
                )
            }
            LaunchedEffect(message) { 
                kotlinx.coroutines.delay(2000)
                viewModel.clearMessage() 
            }
        }
    }
    
    // 编辑对话框
    uiState.editingClip?.let { clip ->
        EditClipDialog(
            clip = clip,
            onDismiss = { viewModel.cancelEdit() },
            onSave = { updatedClip ->
                viewModel.updateClip(updatedClip)
            },
            categories = uiState.categories
        )
    }
}

/**
 * 剪藏项目卡片
 */
@Composable
fun ClipItemCard(
    clip: KnowledgeClip,
    onEdit: (KnowledgeClip) -> Unit,
    onDelete: (KnowledgeClip) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectTag: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = clip.title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(
                        onClick = { onEdit(clip) }
                    ) { 
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier.size(18.dp)
                        ) 
                    }
                    IconButton(
                        onClick = { onDelete(clip) }
                    ) { 
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(18.dp)
                        ) 
                    }
                }
            }
            
            // 显示分类
            clip.category?.let { category ->
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = { onSelectCategory(category) },
                    label = { Text(category) },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null
                        ) 
                    }
                )
            }
            
            // 显示标签
            if (clip.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    clip.tags.take(3).forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { onSelectTag(tag) },
                            label = { Text(tag) },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Label,
                                    contentDescription = null
                                ) 
                            }
                        )
                    }
                    if (clip.tags.size > 3) {
                        Text(
                            text = "+${clip.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = clip.content.take(100) + if (clip.content.length > 100) "..." else "",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTime(clip.createdAt),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 编辑剪藏对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClipDialog(
    clip: KnowledgeClip,
    onDismiss: () -> Unit,
    onSave: (KnowledgeClip) -> Unit,
    categories: List<String>
) {
    var editTitle by remember { mutableStateOf(clip.title) }
    var editContent by remember { mutableStateOf(clip.content) }
    var editCategory by remember { mutableStateOf(clip.category ?: "") }
    var editTags by remember { mutableStateOf(clip.tags.joinToString(", ")) }
    
    var expandedCategory by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑剪藏") },
        text = {
            Column {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    label = { Text("内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 分类选择框
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    TextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("分类（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = if (expandedCategory) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("无分类") },
                            onClick = {
                                editCategory = ""
                                expandedCategory = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    editCategory = category
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editTags,
                    onValueChange = { editTags = it },
                    label = { Text("标签（逗号分隔）") },
                    modifier = Modifier.fillMaxWidth(),
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
                        title = editTitle,
                        content = editContent,
                        category = if (editCategory.isBlank()) null else editCategory,
                        tags = editTags.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() },
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(updatedClip)
                },
                enabled = editTitle.isNotBlank() && editContent.isNotBlank()
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