package com.syncrime.android.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.syncrime.android.presentation.navigation.SyncRimeNavigation
import com.syncrime.android.presentation.theme.SyncRimeTheme
import com.syncrime.android.presentation.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * SyncRime 主活动
 * 
 * 应用的入口点，负责初始化应用和设置导航
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            SyncRimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SyncRimeApp()
                }
            }
        }
    }
}

/**
 * SyncRime 应用组合函数
 */
@Composable
fun SyncRimeApp() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme = isSystemInDarkTheme()
    
    // 初始化应用
    LaunchedEffect(Unit) {
        mainViewModel.initialize()
    }
    
    // 设置导航
    SyncRimeNavigation(
        navController = navController,
        uiState = uiState,
        onEvent = mainViewModel::handleEvent,
        isDarkTheme = isDarkTheme
    )
}

@Preview(showBackground = true)
@Composable
fun SyncRimeAppPreview() {
    SyncRimeTheme {
        SyncRimeApp()
    }
}