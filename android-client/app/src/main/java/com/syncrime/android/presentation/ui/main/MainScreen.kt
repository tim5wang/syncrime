package com.syncrime.android.presentation.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.syncrime.android.presentation.ui.components.*
import com.syncrime.android.presentation.theme.*

/**
 * 主界面组合函数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            MainTopBar(
                syncStatus = syncStatus,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        bottomBar = {
            MainBottomBar(
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToStatistics = onNavigateToStatistics
            )
        },
        floatingActionButton = {
            if (uiState.isCapturing) {
                StopCaptureButton(
                    onClick = { viewModel.stopCapture() }
                )
            } else {
                StartCaptureButton(
                    onClick = { viewModel.showApplicationSelector() }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 欢迎卡片
                WelcomeCard(
                    userName = uiState.userName,
                    isCapturing = uiState.isCapturing,
                    currentSession = uiState.currentSession
                )
            }
            
            // 统计信息卡片
            if (statistics != null) {
                item {
                    StatisticsCard(
                        statistics = statistics!!,
                        onNavigateToStatistics = onNavigateToStatistics
                    )
                }
            }
            
            // 同步状态卡片
            item {
                SyncStatusCard(
                    syncStatus = syncStatus,
                    onSyncNow = { viewModel.syncData() }
                )
            }
            
            // 当前会话状态
            if (uiState.hasActiveSession) {
                item {
                    ActiveSessionCard(
                        session = uiState.currentSession!!,
                        onStopSession = { viewModel.stopCapture() }
                    )
                }
            } else {
                item {
                    StartSessionCard(
                        onShowApplicationSelector = { viewModel.showApplicationSelector() }
                    )
                }
            }
            
            // 智能推荐
            if (recommendations.isNotEmpty()) {
                item {
                    RecommendationsCard(
                        recommendations = recommendations,
                        onRecommendationSelected = { recommendation ->
                            viewModel.selectRecommendation(recommendation)
                        }
                    )
                }
            }
            
            // 快捷操作
            item {
                QuickActionsCard(
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToStatistics = onNavigateToStatistics,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
            
            // 最近输入
            if (uiState.recentInputs.isNotEmpty()) {
                item {
                    RecentInputsCard(
                        recentInputs = uiState.recentInputs,
                        onInputSelected = { input ->
                            viewModel.selectInput(input)
                        }
                    )
                }
            }
        }
    }
    
    // 应用选择对话框
    if (uiState.showApplicationSelector) {
        ApplicationSelectorDialog(
            applications = uiState.availableApplications,
            onApplicationSelected = { application ->
                viewModel.startInputSession(application.packageName, application.name)
            },
            onDismiss = { viewModel.hideApplicationSelector() }
        )
    }
    
    // 错误提示
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // 显示错误提示
        }
    }
}

/**
 * 主界面的顶部应用栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    syncStatus: SyncStatus,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "SyncRime",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onNavigateToProfile) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "个人资料"
                )
            }
            
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * 主界面的底部导航栏
 */
@Composable
private fun MainBottomBar(
    onNavigateToSettings: () -> Unit,
    onNavigateToStatistics: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
            label = { Text("首页") },
            selected = true,
            onClick = { }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "统计") },
            label = { Text("统计") },
            selected = false,
            onClick = onNavigateToStatistics
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
            label = { Text("设置") },
            selected = false,
            onClick = onNavigateToSettings
        )
    }
}

/**
 * 开始采集浮动按钮
 */
@Composable
private fun StartCaptureButton(
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "开始采集"
        )
    }
}

/**
 * 停止采集浮动按钮
 */
@Composable
private fun StopCaptureButton(
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = "停止采集"
        )
    }
}

/**
 * 欢迎卡片
 */
@Composable
private fun WelcomeCard(
    userName: String,
    isCapturing: Boolean,
    currentSession: InputSession?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "欢迎回来，$userName！",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isCapturing) {
                    if (currentSession != null) {
                        "正在采集 ${currentSession.application} 的输入..."
                    } else {
                        "正在采集输入数据..."
                    }
                } else {
                    "点击下方按钮开始采集输入数据"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 统计信息卡片
 */
@Composable
private fun StatisticsCard(
    statistics: Statistics,
    onNavigateToStatistics: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToStatistics() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onNavigateToStatistics) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "查看详细统计"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "总输入",
                    value = statistics.totalInputsToday.toString(),
                    icon = Icons.Default.Keyboard
                )
                
                StatisticItem(
                    label = "会话数",
                    value = statistics.totalSessionsToday.toString(),
                    icon = Icons.Default.History
                )
                
                StatisticItem(
                    label = "同步次数",
                    value = statistics.syncCountToday.toString(),
                    icon = Icons.Default.Sync
                )
            }
        }
    }
}

/**
 * 统计项目组件
 */
@Composable
private fun StatisticItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 同步状态卡片
 */
@Composable
private fun SyncStatusCard(
    syncStatus: SyncStatus,
    onSyncNow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (syncStatus.status) {
                        SyncStatus.Status.SYNCING -> Icons.Default.Sync
                        SyncStatus.Status.SUCCESS -> Icons.Default.CheckCircle
                        SyncStatus.Status.ERROR -> Icons.Default.Error
                        else -> Icons.Default.SyncProblem
                    },
                    contentDescription = null,
                    tint = when (syncStatus.status) {
                        SyncStatus.Status.SYNCING -> MaterialTheme.colorScheme.primary
                        SyncStatus.Status.SUCCESS -> Color(0xFF4CAF50)
                        SyncStatus.Status.ERROR -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "数据同步",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = when (syncStatus.status) {
                            SyncStatus.Status.SYNCING -> "正在同步..."
                            SyncStatus.Status.SUCCESS -> "同步成功"
                            SyncStatus.Status.ERROR -> "同步失败"
                            else -> "待同步"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (syncStatus.lastSyncTime > 0) {
                        Text(
                            text = "上次同步: ${formatTime(syncStatus.lastSyncTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Button(
                onClick = onSyncNow,
                enabled = syncStatus.status != SyncStatus.Status.SYNCING
            ) {
                Text("立即同步")
            }
        }
    }
}

/**
 * 活跃会话卡片
 */
@Composable
private fun ActiveSessionCard(
    session: InputSession,
    onStopSession: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "当前会话",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = session.application,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "输入数: ${session.inputCount} | 时长: ${formatDuration(session.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                OutlinedButton(
                    onClick = onStopSession,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("结束")
                }
            }
        }
    }
}

/**
 * 开始会话卡片
 */
@Composable
private fun StartSessionCard(
    onShowApplicationSelector: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowApplicationSelector() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "开始采集",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "选择应用开始输入采集",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 推荐卡片
 */
@Composable
private fun RecommendationsCard(
    recommendations: List<Recommendation>,
    onRecommendationSelected: (Recommendation) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "智能推荐",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recommendations.take(5).forEach { recommendation ->
                RecommendationItem(
                    recommendation = recommendation,
                    onClick = { onRecommendationSelected(recommendation) }
                )
                
                if (recommendation != recommendations.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 推荐项组件
 */
@Composable
private fun RecommendationItem(
    recommendation: Recommendation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (recommendation.type) {
                Recommendation.TYPE_WORD -> Icons.Default.Keyboard
                Recommendation.TYPE_PHRASE -> Icons.Default.TextFields
                Recommendation.TYPE_SENTENCE -> Icons.Default.Article
                Recommendation.TYPE_EMOJI -> Icons.Default.EmojiEmotions
                else -> Icons.Default.SmartButton
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recommendation.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = recommendation.type,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LinearProgressIndicator(
            progress = recommendation.confidence,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(50)),
            color = when {
                recommendation.confidence >= 0.8f -> Color(0xFF4CAF50)
                recommendation.confidence >= 0.6f -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
        )
    }
}

/**
 * 快捷操作卡片
 */
@Composable
private fun QuickActionsCard(
    onNavigateToSettings: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "快捷操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionItem(
                    icon = Icons.Default.Settings,
                    label = "设置",
                    onClick = onNavigateToSettings
                )
                
                QuickActionItem(
                    icon = Icons.Default.BarChart,
                    label = "统计",
                    onClick = onNavigateToStatistics
                )
                
                QuickActionItem(
                    icon = Icons.Default.AccountCircle,
                    label = "个人",
                    onClick = onNavigateToProfile
                )
                
                QuickActionItem(
                    icon = Icons.Default.Sync,
                    label = "同步",
                    onClick = { /* 触发同步 */ }
                )
            }
        }
    }
}

/**
 * 快捷操作项组件
 */
@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 最近输入卡片
 */
@Composable
private fun RecentInputsCard(
    recentInputs: List<String>,
    onInputSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "最近输入",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recentInputs.take(5).forEach { input ->
                Text(
                    text = input,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInputSelected(input) }
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}