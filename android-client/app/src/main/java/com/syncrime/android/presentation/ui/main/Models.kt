package com.syncrime.android.presentation.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

data class MainUiState(
    val isLoading: Boolean = false,
    val isCapturing: Boolean = false,
    val currentSession: InputSession? = null,
    val showApplicationSelector: Boolean = false,
    val userName: String = "用户",
    val recentInputs: List<String> = emptyList(),
    val error: String? = null
)

data class InputSession(
    val id: String,
    val application: String,
    val startTime: Long,
    val inputCount: Int = 0
)

sealed class MainUiEvent {
    object Refresh : MainUiEvent()
    object ClearError : MainUiEvent()
    object SyncData : MainUiEvent()
    data class ShowError(val message: String) : MainUiEvent()
}

data class SyncStatus(
    val status: Status = Status.IDLE,
    val lastSyncTime: Long = 0
) {
    enum class Status { IDLE, SYNCING, SUCCESS, ERROR }
    val statusText: String get() = when(status) {
        Status.IDLE -> "待同步"
        Status.SYNCING -> "同步中..."
        Status.SUCCESS -> "同步成功"
        Status.ERROR -> "同步失败"
    }
}

data class Statistics(
    val totalInputsToday: Long = 0,
    val totalSessionsToday: Int = 0,
    val syncCountToday: Int = 0
)

data class Recommendation(
    val text: String,
    val type: String,
    val confidence: Float
)
