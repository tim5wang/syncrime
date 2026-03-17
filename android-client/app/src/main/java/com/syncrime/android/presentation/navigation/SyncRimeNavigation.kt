package com.syncrime.android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.syncrime.android.presentation.ui.main.MainScreen
import com.syncrime.android.presentation.ui.settings.SettingsScreen
import com.syncrime.android.presentation.ui.settings.PermissionManagementScreen
import com.syncrime.android.presentation.ui.settings.AccessibilitySettingsScreen
import com.syncrime.android.presentation.ui.statistics.StatisticsScreen
import com.syncrime.android.presentation.ui.profile.ProfileScreen
import com.syncrime.android.presentation.ui.main.MainUiState
import com.syncrime.android.presentation.ui.main.MainUiEvent

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
    object PermissionManagement : Screen("permission_management")
    object AccessibilitySettings : Screen("accessibility_settings")
    object Statistics : Screen("statistics")
    object Profile : Screen("profile")
}

@Composable
fun SyncRimeNavigation(
    navController: NavHostController = rememberNavController(),
    uiState: MainUiState,
    onEvent: (MainUiEvent) -> Unit,
    isDarkTheme: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPermissionManagement = { navController.navigate(Screen.PermissionManagement.route) }
            )
        }
        
        composable(Screen.PermissionManagement.route) {
            PermissionManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAccessibilitySettings = { navController.navigate(Screen.AccessibilitySettings.route) }
            )
        }
        
        composable(Screen.AccessibilitySettings.route) {
            AccessibilitySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { }
            )
        }
    }
}
