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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syncrime.android.permission.PermissionManager
import com.syncrime.android.permission.PermissionManager.PermissionType

data class PermissionUiState(
    val type: PermissionType,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagementScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var permissions by remember { 
        mutableStateOf(loadPermissions(context)) 
    }
    var showRationaleDialog by remember { mutableStateOf<PermissionType?>(null) }
    
    LaunchedEffect(Unit) {
        permissions = loadPermissions(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("权限管理", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "应用权限",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(permissions.size) { index ->
                val permission = permissions[index]
                PermissionCard(
                    permission = permission,
                    onRequestPermission = {
                        handlePermissionRequest(context, permission.type)
                    },
                    onShowRationale = {
                        showRationaleDialog = permission.type
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "权限说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• 通知权限：用于接收同步状态和输入推荐提醒\n" +
                                   "• 存储权限：用于备份和恢复输入数据\n" +
                                   "• 位置权限：用于智能场景识别（如位置相关的输入建议）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    showRationaleDialog?.let { type ->
        PermissionRationaleDialog(
            permissionType = type,
            onDismiss = { showRationaleDialog = null },
            onConfirm = {
                handlePermissionRequest(context, type)
                showRationaleDialog = null
            }
        )
    }
}

@Composable
private fun PermissionCard(
    permission: PermissionUiState,
    onRequestPermission: () -> Unit,
    onShowRationale: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!permission.isGranted) {
                    if (permission.shouldShowRationale) {
                        onShowRationale()
                    } else {
                        onRequestPermission()
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = permission.icon,
                contentDescription = null,
                tint = if (permission.isGranted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permission.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (permission.isGranted) "已授权" else "未授权",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (permission.isGranted) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
            if (permission.isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已授权",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "需要授权",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PermissionRationaleDialog(
    permissionType: PermissionType,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val (title, message) = when (permissionType) {
        PermissionType.NOTIFICATIONS -> "通知权限" to "SyncRime需要通知权限来提醒您同步状态和输入推荐。是否授权？"
        PermissionType.STORAGE -> "存储权限" to "SyncRime需要存储权限来备份和恢复您的输入数据。是否授权？"
        PermissionType.LOCATION -> "位置权限" to "SyncRime需要位置权限来提供基于位置的智能输入建议。是否授权？"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("授权")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun loadPermissions(context: android.content.Context): List<PermissionUiState> {
    return listOf(
        PermissionUiState(
            type = PermissionType.NOTIFICATIONS,
            title = "通知权限",
            description = "接收同步状态和应用更新通知",
            icon = Icons.Default.Notifications,
            isGranted = PermissionManager.hasNotificationPermission(context),
            shouldShowRationale = (context as? android.app.Activity)?.let {
                PermissionManager.shouldShowNotificationRationale(it)
            } ?: false
        ),
        PermissionUiState(
            type = PermissionType.STORAGE,
            title = "存储权限",
            description = "备份和恢复输入数据",
            icon = Icons.Default.Storage,
            isGranted = PermissionManager.hasStoragePermission(context),
            shouldShowRationale = (context as? android.app.Activity)?.let {
                PermissionManager.shouldShowStorageRationale(it)
            } ?: false
        ),
        PermissionUiState(
            type = PermissionType.LOCATION,
            title = "位置权限",
            description = "基于位置的智能输入建议",
            icon = Icons.Default.LocationOn,
            isGranted = PermissionManager.hasLocationPermission(context),
            shouldShowRationale = (context as? android.app.Activity)?.let {
                PermissionManager.shouldShowLocationRationale(it)
            } ?: false
        )
    )
}

private fun handlePermissionRequest(
    context: android.content.Context,
    type: PermissionType
) {
    val activity = context as? android.app.Activity ?: return
    
    when (type) {
        PermissionType.NOTIFICATIONS -> PermissionManager.requestNotificationPermission(activity)
        PermissionType.STORAGE -> PermissionManager.requestStoragePermission(activity)
        PermissionType.LOCATION -> PermissionManager.requestLocationPermission(activity)
    }
}
