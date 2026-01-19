package com.syncrime.android.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * SyncRime 应用主题配置
 * 
 * 提供浅色和深色主题，支持动态主题切换
 */
object SyncRimeTheme {
    
    // 主品牌色
    val SyncRimePrimary = Color(0xFF2196F3)      // 蓝色
    val SyncRimeSecondary = Color(0xFF03DAC6)    // 青色
    
    // 功能色
    val Success = Color(0xFF4CAF50)          // 绿色
    val Warning = Color(0xFFFF9800)          // 橙色
    val Error = Color(0xFFF44336)            // 红色
    val Info = Color(0xFF2196F3)              // 信息蓝
    
    // 中性色
    val BackgroundLight = Color(0xFFFAFAFA)
    val BackgroundDark = Color(0xFF121212)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF1E1E1E)
    
    // 文字色
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val OnPrimaryDark = Color(0xFF000000)
    val OnSurfaceLight = Color(0xFF000000)
    val OnSurfaceDark = Color(0xFFFFFFFF)
    
    // 智能化功能特定颜色
    val IntelligenceBlue = Color(0xFF1976D2)
    val IntelligenceGreen = Color(0xFF388E3C)
    val IntelligencePurple = Color(0xFF7B1FA2)
    val IntelligenceOrange = Color(0xFFFF6F00)
    
    // 状态颜色
    val Capturing = Color(0xFF4CAF50)
    val Syncing = Color(0xFF2196F3)
    val Idle = Color(0xFF9E9E9E)
    
    /**
     * 浅色主题
     */
    val LightColorScheme = lightColorScheme(
        primary = SyncRimePrimary,
        onPrimary = OnPrimaryLight,
        primaryContainer = Color(0xFFE3F2FD),
        onPrimaryContainer = Color(0xFF0D47A1),
        
        secondary = SyncRimeSecondary,
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFFB2EBF2),
        onSecondaryContainer = Color(0xFF004C8C),
        
        tertiary = Color(0xFF7E57C2),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFFFCE4EC),
        onTertiaryContainer = Color(0xFF492532),
        
        background = BackgroundLight,
        onBackground = Color(0xFF000000),
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        
        error = Error,
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFEBEE),
        onErrorContainer = Color(0xFFB71C1C),
        
        success = Success,
        onSuccess = Color(0xFFFFFFFF),
        successContainer = Color(0xFFE8F5E8),
        onSuccessContainer = Color(0xFF2E7D32),
        
        warning = Warning,
        onWarning = Color(0xFF000000),
        warningContainer = Color(0xFFFFF3E0),
        onWarningContainer = Color(0xFF65521C),
        
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        
        scrim = Color(0xFF000000),
        
        surfaceVariant = Color(0xFFF5F5F5),
        onSurfaceVariant = Color(0xFF49454F),
        
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
        inversePrimary = Color(0xFF90CAF9)
    )
    
    /**
     * 深色主题
     */
    val DarkColorScheme = darkColorScheme(
        primary = Color(0xFF90CAF9),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF00497A),
        onPrimaryContainer = Color(0xFFB3E5FC),
        
        secondary = Color(0xFF18FFFF),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF004C8C),
        onSecondaryContainer = Color(0xFFB2EBF2),
        
        tertiary = Color(0xFFCE93D8),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF4E2552),
        onTertiaryContainer = Color(0xFF8FD9E2),
        
        background = BackgroundDark,
        onBackground = Color(0xFFFFFFFF),
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        
        error = Color(0xFFCF6679),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFFB71C1C),
        onErrorContainer = Color(0xFFFFDAD6),
        
        success = Color(0xFF66BB6A),
        onSuccess = Color(0xFF000000),
        successContainer = Color(0xFF2E7D32),
        onSuccessContainer = Color(0xFFC8E6C9),
        
        warning = Color(0xFFFFB74D),
        onWarning = Color(0xFF000000),
        warningContainer = Color(0xFF65521C),
        onWarningContainer = Color(0xFFFFE0B2),
        
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        
        scrim = Color(0xFF000000),
        
        surfaceVariant = Color(0xFF1E1E1E),
        onSurfaceVariant = Color(0xFFCAC4D0),
        
        inverseSurface = Color(0xFFECE1F5),
        inverseOnSurface = Color(0xFF000000),
        inversePrimary = Color(0xFF2196F3)
    )
    
    /**
     * 动态主题（支持 Android 12+ Material You）
     */
    @Composable
    fun DynamicColorScheme(
        darkTheme: Boolean = isSystemInDarkTheme()
    ): ColorScheme {
        return if (darkTheme) DarkColorScheme else LightColorScheme
    }
    
    /**
     * 智能化功能专用颜色
     */
    object IntelligenceColors {
        val Recommendation = IntelligenceBlue
        val Context = IntelligenceGreen
        val Learning = IntelligencePurple
        val Analytics = IntelligenceOrange
        val Sync = Syncing
        val Capture = Capturing
        val Idle = Idle
        
        // 智能化功能渐变色
        val RecommendationGradient = listOf(
            Color(0xFF2196F3),
            Color(0xFF1976D2),
            Color(0xFF1565C0)
        )
        
        val ContextGradient = listOf(
            Color(0xFF4CAF50),
            Color(0xFF388E3C),
            Color(0xFF2E7D32)
        )
        
        val LearningGradient = listOf(
            Color(0xFF9C27B0),
            Color(0xFF7B1FA2),
            Color(0xFF673AB7)
        )
    }
    
    /**
     * 主题配置
     */
    data class ThemeConfig(
        val darkMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
        val useDynamicColors: Boolean = true,
        val customAccentColor: Color? = null
    ) {
        enum class ThemeMode {
            LIGHT,
            DARK,
            FOLLOW_SYSTEM
        }
    }
    
    /**
     * 应用 SyncRime 主题
     */
    @Composable
    fun SyncRimeTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        useDynamicColors: Boolean = true,
        config: ThemeConfig = ThemeConfig(),
        content: @Composable () -> Unit
    ) {
        val colorScheme = if (useDynamicColors) {
            DynamicColorScheme(darkTheme)
        } else {
            when (config.darkMode) {
                ThemeConfig.ThemeMode.DARK -> DarkColorScheme
                ThemeConfig.ThemeMode.LIGHT -> LightColorScheme
                else -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * 自定义形状
 */
object Shapes {
    val Small = RoundedCornerShape(4.dp)
    val Medium = RoundedCornerShape(8.dp)
    val Large = RoundedCornerShape(12.dp)
    val ExtraLarge = RoundedCornerShape(16.dp)
    
    // 智能化功能专用形状
    val IntelligenceCard = RoundedCornerShape(12.dp)
    val RecommendationItem = RoundedCornerShape(8.dp)
    val StatusIndicator = RoundedCornerShape(50)
    val FloatingActionButton = RoundedCornerShape(16.dp)
}

/**
 * 自定义排版
 */
object Typography {
    val Typography = androidx.compose.material3.Typography(
        displayLarge = androidx.compose.material3.Typography().displayLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        displayMedium = androidx.compose.material3.Typography().displayMedium.copy(
            fontWeight = FontWeight.Medium
        ),
        displaySmall = androidx.compose.material3.Typography().displaySmall.copy(
            fontWeight = FontWeight.Normal
        ),
        headlineLarge = androidx.compose.material3.Typography().headlineLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        headlineMedium = androidx.compose.material3.Typography().headlineMedium.copy(
            fontWeight = FontWeight.Medium
        ),
        headlineSmall = androidx.compose.material3.Typography().headlineSmall.copy(
            fontWeight = FontWeight.Medium
        ),
        titleLarge = androidx.compose.material3.Typography().titleLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        titleMedium = androidx.compose.material3.Typography().titleMedium.copy(
            fontWeight = FontWeight.Medium
        ),
        titleSmall = androidx.compose.material3.Typography().titleSmall.copy(
            fontWeight = FontWeight.Medium
        ),
        bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(
            fontWeight = FontWeight.Normal
        ),
        bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(
            fontWeight = FontWeight.Normal
        ),
        bodySmall = androidx.compose.material3.Typography().bodySmall.copy(
            fontWeight = FontWeight.Normal
        ),
        labelLarge = androidx.compose.material3.Typography().labelLarge.copy(
            fontWeight = FontWeight.Medium
        ),
        labelMedium = androidx.compose.material3.Typography().labelMedium.copy(
            fontWeight = FontWeight.Medium
        ),
        labelSmall = androidx.compose.material3.Typography().labelSmall.copy(
            fontWeight = FontWeight.Normal
        )
    )
}

/**
 * 颜色扩展函数
 */
fun ColorScheme.intelligenceColor(type: IntelligenceColorType): Color {
    return when (type) {
        IntelligenceColorType.RECOMMENDATION -> SyncRimeTheme.IntelligenceColors.Recommendation
        IntelligenceColorType.CONTEXT -> SyncRimeTheme.IntelligenceColors.Context
        IntelligenceColorType.LEARNING -> SyncRimeTheme.IntelligenceColors.Learning
        IntelligenceColorType.ANALYTICS -> SyncRimeTheme.IntelligenceColors.Analytics
        IntelligenceColorType.SYNC -> SyncRimeTheme.IntelligenceColors.Sync
        IntelligenceColorType.CAPTURE -> SyncRimeTheme.IntelligenceColors.Capture
        IntelligenceColorType.IDLE -> SyncRimeTheme.IntelligenceColors.Idle
    }
}

/**
 * 智能化颜色类型
 */
enum class IntelligenceColorType {
    RECOMMENDATION,
    CONTEXT,
    LEARNING,
    ANALYTICS,
    SYNC,
    CAPTURE,
    IDLE
}

/**
 * 主题扩展函数
 */
@Composable
fun ColorScheme.getSurfaceColorForIntelligence(type: IntelligenceColorType): Color {
    return when (type) {
        IntelligenceColorType.RECOMMENDATION -> primaryContainer
        IntelligenceColorType.CONTEXT -> secondaryContainer
        IntelligenceColorType.LEARNING -> tertiaryContainer
        IntelligenceColorType.ANALYTICS -> errorContainer
        IntelligenceColorType.SYNC -> primary
        IntelligenceColorType.CAPTURE -> success
        IntelligenceColorType.IDLE -> outline
    }
}

/**
 * 获取对应的状态颜色
 */
@Composable
fun ColorScheme.getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "capturing" -> SyncRimeTheme.IntelligenceColors.Capture
        "syncing" -> SyncRimeTheme.IntelligenceColors.Sync
        "success" -> SyncRimeTheme.Success
        "error", "failed" -> SyncRimeTheme.Error
        "warning" -> SyncRimeTheme.Warning
        else -> SyncRimeTheme.IntelligenceColors.Idle
    }
}

/**
 * 获取重要性对应的颜色
 */
@Composable
fun ColorScheme.getImportanceColor(importance: Int): Color {
    return when (importance) {
        1 -> outline // 低重要性
        2 -> outlineVariant // 普通重要性
        3 -> primary // 高重要性
        4 -> error // 关键重要性
        else -> outline
    }
}

/**
 * 主题预设
 */
object ThemePresets {
    val Professional = SyncRimeTheme.ThemeConfig(
        darkMode = SyncRimeTheme.ThemeConfig.ThemeMode.FOLLOW_SYSTEM,
        useDynamicColors = true
    )
    
    val HighContrast = SyncRimeTheme.ThemeConfig(
        darkMode = SyncRimeTheme.ThemeConfig.ThemeMode.FOLLOW_SYSTEM,
        useDynamicColors = false
    )
    
    val DarkMode = SyncRimeTheme.ThemeConfig(
        darkMode = SyncRimeTheme.ThemeConfig.ThemeMode.DARK,
        useDynamicColors = true
    )
    
    val LightMode = SyncRimeTheme.ThemeConfig(
        darkMode = SyncRimeTheme.ThemeConfig.ThemeMode.LIGHT,
        useDynamicColors = true
    )
    
    val Colorful = SyncRimeTheme.ThemeConfig(
        darkMode = SyncRimeTheme.ThemeConfig.ThemeMode.FOLLOW_SYSTEM,
        useDynamicColors = true,
        customAccentColor = SyncRimeTheme.IntelligenceOrange
    )
}