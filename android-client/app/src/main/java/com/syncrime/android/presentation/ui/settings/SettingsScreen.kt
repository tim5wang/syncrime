package com.syncrime.android.presentation.ui.settings

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syncrime.android.permission.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToPermissionManagement: () -> Unit = {}
) {
    val context = LocalContext.current
    var captureEnabled by remember { mutableStateOf(true) }
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(PermissionManager.hasNotificationPermission(context)) }
    var privacyModeEnabled by remember { mutableStateOf(false) }
    var dataEncryptionEnabled by remember { mutableStateOf(true) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        notificationsEnabled = PermissionManager.hasNotificationPermission(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "采集设置") {
                    SwitchSettingItem(
                        icon = Icons.Default.RecordVoiceOver,
                        title = "启用输入采集",
                        subtitle = "采集输入内容用于智能推荐",
                        checked = captureEnabled,
                        onCheckedChange = { captureEnabled = it }
                    )
                }
            }
            
            item {
                SettingsSection(title = "同步设置") {
                    SwitchSettingItem(
                        icon = Icons.Default.Sync,
                        title = "自动同步",
                        subtitle = "自动同步输入数据到云端",
                        checked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it }
                    )
                }
            }
            
            item {
                SettingsSection(title = "通知设置") {
                    ClickableSettingItem(
                        icon = Icons.Default.Notifications,
                        title = "启用通知",
                        subtitle = if (notificationsEnabled) "接收同步状态和应用更新通知" else "点击开启通知权限",
                        onClick = {
                            if (!notificationsEnabled) {
                                if (PermissionManager.shouldShowNotificationRationale(context as android.app.Activity)) {
                                    showPermissionDialog = true
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    PermissionManager.requestNotificationPermission(context as android.app.Activity)
                                }
                            }
                        }
                    )
                    
                    if (notificationsEnabled) {
                        SwitchSettingItem(
                            icon = Icons.Default.NotificationsActive,
                            title = "同步通知",
                            subtitle = "同步完成后发送通知",
                            checked = true,
                            onCheckedChange = { }
                        )
                    }
                }
            }
            
            item {
                SettingsSection(title = "隐私与安全") {
                    SwitchSettingItem(
                        icon = Icons.Default.Lock,
                        title = "数据加密",
                        subtitle = "加密存储本地数据",
                        checked = dataEncryptionEnabled,
                        onCheckedChange = { dataEncryptionEnabled = it }
                    )
                    
                    SwitchSettingItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "隐私模式",
                        subtitle = "隐藏敏感输入内容",
                        checked = privacyModeEnabled,
                        onCheckedChange = { privacyModeEnabled = it }
                    )
                    
                    ClickableSettingItem(
                        icon = Icons.Default.Security,
                        title = "权限管理",
                        subtitle = "管理应用权限",
                        onClick = onNavigateToPermissionManagement
                    )
                }
            }
        }
    }
    
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("通知权限") },
            text = { Text("需要通知权限来接收同步状态和应用更新通知。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionManager.requestNotificationPermission(context as android.app.Activity)
                        }
                    }
                ) {
                    Text("授权")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun SwitchSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ClickableSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
