package com.syncrime.android.presentation.ui.settings

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syncrime.android.accessibility.InputCaptureService

data class AccessibilityServiceState(
    val isEnabled: Boolean,
    val isRunning: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var serviceState by remember {
        mutableStateOf(getAccessibilityServiceState(context))
    }
    
    // 定时刷新状态
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            serviceState = getAccessibilityServiceState(context)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("无障碍设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatusCard(serviceState = serviceState)
            }
            
            item {
                InfoCard()
            }
            
            item {
                ActionCard(
                    isEnabled = serviceState.isEnabled,
                    isRunning = serviceState.isRunning,
                    onOpenSettings = { openAccessibilitySettings(context) }
                )
            }
            
            item {
                PrivacyInfoCard()
            }
        }
    }
}

@Composable
private fun StatusCard(serviceState: AccessibilityServiceState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (serviceState.isRunning) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (serviceState.isRunning) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (serviceState.isRunning) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (serviceState.isRunning) "服务运行中" else "服务未运行",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (serviceState.isEnabled) "已开启无障碍权限" else "未开启无障碍权限",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("什么是无障碍服务？", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "无障碍服务是 Android 系统提供的一项功能，帮助残障人士使用设备。SyncRime 使用此服务来监听您的输入，提供智能推荐和同步功能。",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ActionCard(
    isEnabled: Boolean,
    isRunning: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("操作", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!isEnabled || !isRunning) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开启无障碍服务")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "服务已开启",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { /* TODO: 显示详细说明 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("查看详情说明")
            }
        }
    }
}

@Composable
private fun PrivacyInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("隐私保护", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PrivacyFeatureItem(
                icon = Icons.Default.FilterList,
                text = "自动过滤密码、银行卡号等敏感信息"
            )
            PrivacyFeatureItem(
                icon = Icons.Default.Lock,
                text = "本地加密存储，保护数据安全"
            )
            PrivacyFeatureItem(
                icon = Icons.Default.VisibilityOff,
                text = "不会记录敏感应用的输入"
            )
            PrivacyFeatureItem(
                icon = Icons.Default.ToggleOff,
                text = "您可随时关闭此功能"
            )
        }
    }
}

@Composable
private fun PrivacyFeatureItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 获取无障碍服务状态
 */
private fun getAccessibilityServiceState(context: Context): AccessibilityServiceState {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    
    val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_GENERIC
    )
    
    val isEnabled = enabledServices.any {
        it.resolveInfo.serviceInfo.packageName == context.packageName
    }
    
    val isRunning = InputCaptureService.isRunning()
    
    return AccessibilityServiceState(
        isEnabled = isEnabled,
        isRunning = isRunning
    )
}

/**
 * 打开无障碍设置
 */
private fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        // 如果无法打开设置，显示提示
        e.printStackTrace()
    }
}
