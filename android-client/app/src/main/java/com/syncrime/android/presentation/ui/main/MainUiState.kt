package com.syncrime.android.presentation.ui.main

import com.syncrime.android.domain.model.*

/**
 * 主界面状态
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val isCapturing: Boolean = false,
    val currentSession: InputSession? = null,
    val showApplicationSelector: Boolean = false,
    val availableApplications: List<ApplicationInfo> = emptyList(),
    val userName: String = "用户",
    val userAvatar: String? = null,
    val recentInputs: List<String> = emptyList(),
    val error: String? = null
) {
    val hasActiveSession: Boolean
        get() = currentSession != null && isCapturing
}

/**
 * 主界面事件
 */
sealed class MainUiEvent {
    object Refresh : MainUiEvent()
    object ClearError : MainUiEvent()
    object SyncData : MainUiEvent()
    data class ShowError(val message: String) : MainUiEvent()
}

/**
 * 输入会话状态
 */
data class InputSession(
    val id: String,
    val application: String,
    val startTime: Long,
    val endTime: Long = 0,
    val inputCount: Int = 0,
    val lastInputTime: Long = 0,
    val metadata: Map<String, Any> = emptyMap()
) {
    val duration: Long
        get() = if (endTime > 0) endTime - startTime else System.currentTimeMillis() - startTime
    
    val isActive: Boolean
        get() = endTime == 0
}

/**
 * 应用信息
 */
data class ApplicationInfo(
    val packageName: String,
    val name: String,
    val icon: String? = null,
    val category: String = "general",
    val lastUsed: Long = 0,
    val usageCount: Long = 0
)

/**
 * 统计信息
 */
data class Statistics(
    val totalInputsToday: Long = 0,
    val totalInputsWeek: Long = 0,
    val totalInputsMonth: Long = 0,
    val totalSessionsToday: Int = 0,
    val totalSessionsWeek: Int = 0,
    val totalSessionsMonth: Int = 0,
    val syncCountToday: Int = 0,
    val syncCountWeek: Int = 0,
    val syncCountMonth: Int = 0,
    val averageSessionLength: Double = 0.0,
    val mostUsedApplication: String? = null,
    val topApplications: Map<String, Long> = emptyMap(),
    val inputTypes: Map<String, Long> = emptyMap(),
    val timeDistribution: Map<String, Long> = emptyMap()
) {
    val efficiency: Float
        get() = if (totalSessionsToday > 0) {
            (totalInputsToday.toFloat() / totalSessionsToday).coerceAtMost(100f)
        } else 0f
}

/**
 * 推荐信息
 */
data class Recommendation(
    val text: String,
    val type: String,
    val confidence: Float,
    val source: String,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        const val TYPE_WORD = "word"
        const val TYPE_PHRASE = "phrase"
        const val TYPE_SENTENCE = "sentence"
        const val TYPE_EMOJI = "emoji"
        const val TYPE_CONTACT = "contact"
        const val TYPE_URL = "url"
        const val TYPE_CONTEXTUAL = "contextual"
    }
}

/**
 * 同步状态
 */
data class SyncStatus(
    val status: Status = Status.IDLE,
    val lastSyncTime: Long = 0,
    val syncProgress: Float = 0f,
    val errorMessage: String? = null,
    val isAutomatic: Boolean = true
) {
    enum class Status {
        IDLE,
        SYNCING,
        SUCCESS,
        ERROR,
        PAUSED
    }
    
    val statusText: String
        get() = when (status) {
            IDLE -> "待同步"
            SYNCING -> "同步中..."
            SUCCESS -> "同步成功"
            ERROR -> "同步失败"
            PAUSED -> "同步暂停"
        }
    
    val isSuccess: Boolean
        get() = status == SUCCESS
}

/**
 * 用户资料
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val joinDate: Long,
    val lastActiveTime: Long,
    val preferences: UserPreferences = UserPreferences(),
    val statistics: UserStatistics = UserStatistics()
)

/**
 * 用户偏好设置
 */
data class UserPreferences(
    val language: String = "zh-CN",
    val theme: String = "auto",
    val autoSync: Boolean = true,
    val syncInterval: Long = 900000L, // 15分钟
    val notificationsEnabled: Boolean = true,
    val intelligentRecommendations: Boolean = true,
    val autoCorrection: Boolean = true,
    val dataCollection: Boolean = true,
    val privacyLevel: String = "normal" // "high", "normal", "low"
)

/**
 * 用户统计
 */
data class UserStatistics(
    val totalInputs: Long = 0,
    val totalSessions: Int = 0,
    val totalSyncs: Int = 0,
    val averageSessionLength: Double = 0.0,
    val mostUsedApplication: String? = null,
    val preferredLanguages: List<String> = emptyList(),
    val inputSpeed: Float = 0f, // WPM
    val accuracy: Float = 0f, // 0-1
    val productivity: Float = 0f // 0-1
)

/**
 * 输入记录
 */
data class InputRecord(
    val id: String,
    val sessionId: String,
    val text: String,
    val timestamp: Long,
    val inputType: String,
    val category: String,
    val importance: Importance,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 重要性级别
 */
enum class Importance(val value: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4)
}

/**
 * 输入类型
 */
enum class InputType(val value: String) {
    TEXT("text"),
    PASSWORD("password"),
    EMAIL("email"),
    PHONE("phone"),
    URL("url"),
    NUMBER("number"),
    SYMBOL("symbol"),
    EMOJI("emoji"),
    COMMAND("command")
}