package com.syncrime.app.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextField
import com.syncrime.app.presentation.library.EnhancedLibraryScreen
import com.syncrime.app.presentation.viewmodel.HomeViewModel
import com.syncrime.app.presentation.viewmodel.LibraryViewModel
import com.syncrime.app.presentation.viewmodel.SearchViewModel
import com.syncrime.app.presentation.viewmodel.SettingsViewModel
import com.syncrime.android.debug.DebugViewModel
import com.syncrime.app.util.HighlightableText
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(homeViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(homeViewModel: HomeViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("SyncRime", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("首页") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Search, null) }, label = { Text("搜索") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("知识库") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("设置") })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Icon(Icons.Default.BugReport, null) }, label = { Text("调试") })
            }
        }
    ) { innerPadding -> Box(modifier = Modifier.padding(innerPadding)) {
        when (selectedTab) {
            0 -> HomeTab(homeViewModel)
            1 -> SearchTab()
            2 -> LibraryTab()
            3 -> SettingsTab()
            4 -> DebugTab()
        }
    }}
}

@Composable
fun HomeTab(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.checkAccessibilityStatus() }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("欢迎使用 SyncRime", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊 今日统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("输入记录：${uiState.todayInputCount} 条")
                Text("总记录：${uiState.totalInputCount} 条")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ℹ️ 服务状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.isAccessibilityEnabled) {
                    Text("无障碍服务：✅ 已开启", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("无障碍服务：❌ 未开启", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button({ context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("去开启") }
                }
            }
        }
    }
}

@Composable
fun SearchTab(viewModel: SearchViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val displayRecords = if (uiState.hasSearched) uiState.results else uiState.recentRecords
    
    // 消息提示
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(if (uiState.hasSearched) "🔍 搜索结果" else "📝 最近输入", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { 
                    viewModel.setQuery(it)
                    if (it.length >= 2) {
                        viewModel.searchWithDebounce(it)
                    } else if (it.isBlank()) {
                        viewModel.clearSearch()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索输入记录...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton({ viewModel.clearSearch() }) { Icon(Icons.Default.Clear, null) }
                    }
                },
                singleLine = true
            )
            
            // 搜索建议下拉列表
            if (uiState.showSuggestions && uiState.suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    LazyColumn {
                        items(uiState.suggestions) { suggestion ->
                            Text(
                                text = suggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .clickable {
                                        viewModel.setQuery(suggestion)
                                        viewModel.search(suggestion)
                                    },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
        
        // 消息提示
        uiState.message?.let {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(it, Modifier.padding(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when {
            uiState.isSearching -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            displayRecords.isEmpty() -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Edit, null, Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(if (uiState.hasSearched) "未找到匹配记录" else "暂无输入记录")
                        if (!uiState.hasSearched) {
                            Spacer(Modifier.height(8.dp))
                            Text("开启无障碍服务后，输入内容将显示在这里", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            else -> {
                Text("${if (uiState.hasSearched) "找到" else "共"} ${displayRecords.size} 条记录", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(displayRecords) { record ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(
                                Modifier.padding(12.dp)
                            ) {
                                val displayContent = if (record.content.length > 100) {
                    record.content.take(100) + "..."
                } else {
                    record.content
                }
                HighlightableText(
                    text = displayContent,
                    query = uiState.query,
                    modifier = Modifier.clickable { viewModel.selectRecord(record) }
                )
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text(record.application, style = MaterialTheme.typography.bodySmall)
                                    Row {
                                        Text(formatTime(record.createdAt), style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(8.dp))
                                        IconButton({ viewModel.copyRecord(record) }, Modifier.size(32.dp)) { 
                                            Icon(Icons.Default.ContentCopy, "复制", Modifier.size(18.dp)) 
                                        }
                                        IconButton({ viewModel.deleteRecord(record) }, Modifier.size(32.dp)) { 
                                            Icon(Icons.Default.Delete, "删除", Modifier.size(18.dp)) 
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 详情对话框
    uiState.selectedRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSelectedRecord() },
            title = { Text("输入记录详情") },
            text = {
                Column {
                    Text(record.content, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))
                    Text("应用: ${record.application}", style = MaterialTheme.typography.bodySmall)
                    Text("时间: ${formatTime(record.createdAt)}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button({ viewModel.copyRecord(record); viewModel.clearSelectedRecord() }) { Text("复制") }
            },
            dismissButton = {
                Row {
                    TextButton({ viewModel.deleteRecord(record) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    TextButton({ viewModel.clearSelectedRecord() }) { Text("关闭") }
                }
            }
        )
    }
}

@Composable
fun LibraryTab(viewModel: LibraryViewModel = viewModel()) {
    EnhancedLibraryScreen(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }
    
    // 登录成功后自动关闭对话框
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            showLoginDialog = false
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }
    
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonOutline, null, Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (uiState.isLoggedIn) "👤 ${uiState.userNickname}" else "未登录", fontWeight = FontWeight.Bold)
                        Text(if (uiState.isLoggedIn) uiState.userEmail ?: "" else "点击登录以同步数据", style = MaterialTheme.typography.bodySmall)
                    }
                    if (uiState.isLoggedIn) TextButton({ viewModel.logout() }) { Text("退出") }
                    else Button({ showLoginDialog = true }) { Text("登录") }
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("⚙️ 无障碍服务", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("需要开启无障碍服务才能采集输入内容")
                Spacer(Modifier.height(12.dp))
                Button({ context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.SettingsAccessibility, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("打开无障碍设置")
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("💾 数据设置", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("自动保存"); Switch(uiState.autoSave, { viewModel.setAutoSave(it) }) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("隐私过滤"); Switch(uiState.privacyFilter, { viewModel.setPrivacyFilter(it) }) }
            }
        }
        
        // 消息提示
        uiState.message?.let {
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
    
    // 登录对话框
    if (showLoginDialog) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var nickname by remember { mutableStateOf("") }
        var isRegister by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showLoginDialog = false; viewModel.clearMessage() },
            title = { Text(if (isRegister) "注册账号" else "登录") },
            text = {
                Column {
                    OutlinedTextField(email, { email = it }, label = { Text("邮箱") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                    )
                )
                    if (isRegister) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(nickname, { nickname = it }, label = { Text("昵称（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    uiState.loginError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (isRegister) viewModel.register(email, password, nickname.ifBlank { email.split("@").getOrNull(0) ?: "用户" })
                        else viewModel.login(email, password) 
                    },
                    enabled = !uiState.isLoggingIn && email.isNotBlank() && password.isNotBlank()
                ) {
                    if (uiState.isLoggingIn) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isRegister) "注册" else "登录")
                    }
                }
            },
            dismissButton = {
                Row {
                    TextButton({ isRegister = !isRegister }) { Text(if (isRegister) "已有账号" else "注册账号") }
                    TextButton({ showLoginDialog = false }) { Text("取消") }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugTab(viewModel: DebugViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.checkLoginStatus(); viewModel.checkAccessibilityStatus(context) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("🔧 调试面板", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("📊 系统状态", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("API 状态: ${state.apiStatus}")
                Text("登录状态: ${if (state.isLoggedIn) "✅ 已登录" else "❌ 未登录"}")
                if (state.isLoggedIn) Text("用户: ${state.userInfo}")
                Text("无障碍服务: ${state.accessibilityStatus}")
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            Button({ viewModel.checkApiStatus() }, Modifier.weight(1f)) { Text("测试API") }
            Button({ viewModel.checkLoginStatus() }, Modifier.weight(1f)) { Text("检查登录") }
            Button({ viewModel.checkAccessibilityStatus(context) }, Modifier.weight(1f)) { Text("检查无障碍") }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("📋 日志", fontWeight = FontWeight.Bold); TextButton({ viewModel.clearLogs() }) { Text("清除") } }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(Modifier.padding(8.dp)) {
                items(state.logs) { Text("[${it.time}] ${it.tag}: ${it.message}", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

fun formatTime(timestamp: Long): String = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))