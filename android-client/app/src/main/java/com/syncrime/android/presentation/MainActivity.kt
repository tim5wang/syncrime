package com.syncrime.android.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.syncrime.android.presentation.navigation.SyncRimeNavigation
import com.syncrime.android.presentation.theme.SyncRimeTheme
import com.syncrime.android.presentation.ui.main.MainUiState

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SyncRimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var uiState by remember { mutableStateOf(MainUiState()) }
                    
                    SyncRimeNavigation(
                        navController = navController,
                        uiState = uiState,
                        onEvent = { /* Handle events */ },
                        isDarkTheme = false
                    )
                }
            }
        }
    }
}
