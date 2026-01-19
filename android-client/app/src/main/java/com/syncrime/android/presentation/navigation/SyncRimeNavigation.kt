package com.syncrime.android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.syncrime.android.presentation.ui.main.MainScreen
import com.syncrime.android.presentation.ui.main.MainViewModel
import com.syncrime.android.presentation.ui.settings.SettingsScreen
import com.syncrime.android.presentation.ui.settings.SettingsViewModel
import com.syncrime.android.presentation.ui.statistics.StatisticsScreen
import com.syncrime.android.presentation.ui.statistics.StatisticsViewModel
import com.syncrime.android.presentation.ui.profile.ProfileScreen
import com.syncrime.android.presentation.ui.profile.ProfileViewModel
import com.syncrime.android.presentation.ui.intelligence.IntelligenceSettingsScreen
import com.syncrime.android.presentation.ui.intelligence.IntelligenceSettingsViewModel

/**
 * SyncRime 导航组件
 */
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
        addMainGraph(
            navController = navController,
            onEvent = onEvent,
            isDarkTheme = isDarkTheme
        )
        addSettingsGraph(navController)
        addStatisticsGraph(navController)
        addProfileGraph(navController)
        addIntelligenceGraph(navController)
    }
}

/**
 * 主导航图
 */
private fun NavGraphBuilder.addMainGraph(
    navController: NavHostController,
    onEvent: (MainUiEvent) -> Unit,
    isDarkTheme: Boolean
) {
    composable(Screen.Main.route) {
        MainScreen(
            onNavigateToSettings = {
                navController.navigate(Screen.Settings.route)
            },
            onNavigateToStatistics = {
                navController.navigate(Screen.Statistics.route)
            },
            onNavigateToProfile = {
                navController.navigate(Screen.Profile.route)
            }
        )
    }
}

/**
 * 设置导航图
 */
private fun NavGraphBuilder.addSettingsGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Settings.route,
        route = "settings"
    ) {
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToIntelligence = {
                    navController.navigate(Screen.IntelligenceSettings.route)
                }
            )
        }
        
        composable(Screen.IntelligenceSettings.route) {
            IntelligenceSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * 统计导航图
 */
private fun NavGraphBuilder.addStatisticsGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Statistics.route,
        route = "statistics"
    ) {
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * 个人资料导航图
 */
private fun NavGraphBuilder.addProfileGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Profile.route,
        route = "profile"
    ) {
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.popBackStack()
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
    }
}

/**
 * 智能化设置导航图
 */
private fun NavGraphBuilder.addIntelligenceGraph(navController: NavHostController) {
    // 智能化相关页面的导航逻辑
    // 这里可以添加更多智能化相关的页面
}

/**
 * 导航路由定义
 */
sealed class Screen(val route: String) {
    // 主要页面
    object Main : Screen("main")
    object Settings : Screen("settings")
    object Statistics : Screen("statistics")
    object Profile : Screen("profile")
    
    // 子页面
    object IntelligenceSettings : Screen("intelligence_settings")
    
    // 可选页面
    object Help : Screen("help")
    object About : Screen("about")
    object Privacy : Screen("privacy")
    
    // 创建导航参数
    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}

/**
 * 导航工具类
 */
object NavigationUtils {
    
    /**
     * 导航到主页面
     */
    fun navigateToMain(navController: NavHostController) {
        navController.navigate(Screen.Main.route) {
            popUpTo(Screen.Main.route) {
                inclusive = true
            }
        }
    }
    
    /**
     * 导航到设置页面
     */
    fun navigateToSettings(navController: NavHostController) {
        navController.navigate(Screen.Settings.route)
    }
    
    /**
     * 导航到统计页面
     */
    fun navigateToStatistics(navController: NavHostController) {
        navController.navigate(Screen.Statistics.route)
    }
    
    /**
     * 导航到个人资料页面
     */
    fun navigateToProfile(navController: NavHostController) {
        navController.navigate(Screen.Profile.route)
    }
    
    /**
     * 导航到智能化设置页面
     */
    fun navigateToIntelligenceSettings(navController: NavHostController) {
        navController.navigate(Screen.IntelligenceSettings.route)
    }
    
    /**
     * 返回上一页
     */
    fun navigateBack(navController: NavHostController) {
        navController.popBackStack()
    }
    
    /**
     * 检查是否可以返回
     */
    fun canNavigateBack(navController: NavHostController): Boolean {
        return navController.previousBackStackEntry != null
    }
}

/**
 * 深度链接处理
 */
object DeepLinkHandler {
    
    /**
     * 处理深度链接
     */
    fun handleDeepLink(navController: NavHostController, deepLink: String): Boolean {
        return when {
            deepLink.startsWith("syncrime://app/main") -> {
                NavigationUtils.navigateToMain(navController)
                true
            }
            deepLink.startsWith("syncrime://app/settings") -> {
                NavigationUtils.navigateToSettings(navController)
                true
            }
            deepLink.startsWith("syncrime://app/statistics") -> {
                NavigationUtils.navigateToStatistics(navController)
                true
            }
            deepLink.startsWith("syncrime://app/profile") -> {
                NavigationUtils.navigateToProfile(navController)
                true
            }
            else -> false
        }
    }
    
    /**
     * 解析参数
     */
    fun parseParameters(deepLink: String): Map<String, String> {
        val parameters = mutableMapOf<String, String>()
        val uri = android.net.Uri.parse(deepLink)
        
        uri?.queryParameterNames?.forEach { key ->
            uri.getQueryParameter(key)?.let { value ->
                parameters[key] = value
            }
        }
        
        return parameters
    }
}

/**
 * 导航状态管理
 */
@Composable
fun rememberNavigationState(navController: NavHostController) = remember(navController) {
    NavigationState(navController)
}

/**
 * 导航状态类
 */
class NavigationState(
    private val navController: NavHostController
) {
    var currentRoute: String
        get() = navController.currentBackStackEntry?.destination?.route ?: ""
        private set(_) {}
    
    val canGoBack: Boolean
        get() = NavigationUtils.canNavigateBack(navController)
    
    fun goBack() {
        NavigationUtils.navigateBack(navController)
    }
    
    fun navigateToMain() {
        NavigationUtils.navigateToMain(navController)
    }
    
    fun navigateToSettings() {
        NavigationUtils.navigateToSettings(navController)
    }
    
    fun navigateToStatistics() {
        NavigationUtils.navigateToStatistics(navController)
    }
    
    fun navigateToProfile() {
        NavigationUtils.navigateToProfile(navController)
    }
    
    fun navigateToIntelligenceSettings() {
        NavigationUtils.navigateToIntelligenceSettings(navController)
    }
}