package com.syncrime.app.presentation.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.junit.Test
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule

/**
 * 知识库功能测试
 */
@RunWith(AndroidJUnit4::class)
class LibraryFeatureTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testEnhancedLibraryScreenDisplaysCorrectly() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    var searchQuery by remember { mutableStateOf("") }
                    EnhancedLibraryScreenContent(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        clips = listOf(
                            // 测试数据
                        ),
                        categories = listOf("工作", "学习", "生活"),
                        tags = listOf("重要", "待处理", "已完成"),
                        filterCategory = null,
                        filterTag = null,
                        onCategoryFilterChange = {},
                        onTagFilterChange = {},
                        onClearFilters = {},
                        onClipEdit = {},
                        onClipDelete = {},
                        onClipSelect = {}
                    )
                }
            }
        }
        
        // 验证界面元素存在
        composeTestRule.onNodeWithText("📚 知识库").assertExists()
        composeTestRule.onNodeWithText("🔍 过滤").assertExists()
        composeTestRule.onNodeWithText("所有分类").assertExists()
        composeTestRule.onNodeWithText("所有标签").assertExists()
    }
}

/**
 * 分离的可测试内容组件
 */
@Composable
fun EnhancedLibraryScreenContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    clips: List<com.syncrime.shared.model.KnowledgeClip>,
    categories: List<String>,
    tags: List<String>,
    filterCategory: String?,
    filterTag: String?,
    onCategoryFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onClipEdit: (com.syncrime.shared.model.KnowledgeClip) -> Unit,
    onClipDelete: (com.syncrime.shared.model.KnowledgeClip) -> Unit,
    onClipSelect: (com.syncrime.shared.model.KnowledgeClip) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📚 知识库",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 分类过滤
                var expandedCategory by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    TextField(
                        value = filterCategory ?: "所有分类",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            androidx.compose.material.icons.Icons.Filled.ArrowDropDown.let {
                                androidx.compose.material3.Icon(
                                    imageVector = if (expandedCategory) androidx.compose.material.icons.Icons.Filled.ArrowDropUp else it,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("所有分类") },
                            onClick = {
                                onCategoryFilterChange(null)
                                expandedCategory = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onCategoryFilterChange(category)
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
                        value = filterTag ?: "所有标签",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            androidx.compose.material.icons.Icons.Filled.ArrowDropDown.let {
                                androidx.compose.material3.Icon(
                                    imageVector = if (expandedTag) androidx.compose.material.icons.Icons.Filled.ArrowDropUp else it,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = expandedTag,
                        onDismissRequest = { expandedTag = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("所有标签") },
                            onClick = {
                                onTagFilterChange(null)
                                expandedTag = false
                            }
                        )
                        tags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = {
                                    onTagFilterChange(tag)
                                    expandedTag = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 清除过滤
                if (filterCategory != null || filterTag != null) {
                    Button(
                        onClick = onClearFilters,
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
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索知识库...") },
            leadingIcon = { androidx.compose.material.icons.Icons.Filled.Search.let { 
                androidx.compose.material3.Icon(it, contentDescription = null) 
            } },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) { 
                        androidx.compose.material.icons.Icons.Filled.Clear.let { 
                            androidx.compose.material3.Icon(it, contentDescription = null) 
                        } 
                    }
                }
            },
            singleLine = true
        )
    }
}