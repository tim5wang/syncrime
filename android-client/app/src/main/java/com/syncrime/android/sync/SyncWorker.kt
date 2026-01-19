package com.syncrime.android.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.syncrime.android.data.repository.SyncRepository
import com.syncrime.android.data.repository.InputRepository
import com.syncrime.android.domain.model.*
import com.syncrime.android.intelligence.AndroidIntelligenceEngineManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 数据同步 Worker
 * 
 * 负责在后台执行数据同步任务，支持多种同步策略
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    private val syncRepository: SyncRepository,
    private val inputRepository: InputRepository,
    private val intelligenceEngine: AndroidIntelligenceEngineManager
) : CoroutineWorker(context, WorkerParameters()) {

    companion object {
        const val TAG = "SyncWorker"
        const val DEFAULT_SYNC_INTERVAL = 15L // 分钟
        const val MAX_RETRIES = 3
        const val BACKOFF_DELAY = 30L // 秒
        
        fun buildRequest(
            syncType: SyncType = SyncType.AUTOMATIC,
            priority: Priority = Priority.LOW
        ): OneTimeWorkRequestBuilder {
            return OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY,
                    TimeUnit.SECONDS
                )
                .setInputData(
                    workDataOf(
                        "sync_type" to syncType.name,
                        "priority" to priority.name
                    )
                )
                .setPriority(priority)
        }
        
        fun buildPeriodicRequest(
            intervalMinutes: Long = DEFAULT_SYNC_INTERVAL,
            flexMinutes: Long = 5L
        ): PeriodicWorkRequestBuilder {
            return PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes, TimeUnit.MINUTES,
                flexMinutes, TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresCharging(true)
                    .build()
            ).setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY,
                TimeUnit.SECONDS
            )
        }
    }

    override suspend fun doWork(): Result {
        val syncType = inputData.getString("sync_type", SyncType.AUTOMATIC.name)
        val priority = inputData.getString("priority", Priority.LOW.name)
        
        return try {
            when (SyncType.valueOf(syncType)) {
                SyncType.AUTOMATIC -> performAutomaticSync()
                SyncType.MANUAL -> performManualSync()
                SyncType.INCREMENTAL -> performIncrementalSync()
                SyncType.FULL -> performFullSync()
                SyncType.INTELLIGENCE -> performIntelligenceSync()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行自动同步
     */
    private suspend fun performAutomaticSync(): Result {
        return try {
            // 1. 检查同步条件
            if (!shouldPerformAutoSync()) {
                return Result.success()
            }

            // 2. 获取本地数据
            val localData = getLocalSyncData()
            if (localData.isEmpty()) {
                return Result.success()
            }

            // 3. 增量同步
            val syncResult = syncRepository.incrementalSync(localData)
            
            // 4. 处理同步结果
            when (syncResult) {
                is SyncResult.Success -> {
                    updateLastSyncTime()
                    syncIntelligenceData()
                    Result.success()
                }
                is SyncResult.Error -> {
                    handleSyncError(syncResult)
                    Result.retry()
                }
                else -> Result.success()
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行手动同步
     */
    private suspend fun performManualSync(): Result {
        return try {
            // 1. 强制全量同步
            val localData = getAllLocalData()
            
            // 2. 上传本地数据
            val uploadResult = syncRepository.uploadData(localData)
            
            // 3. 下载服务器数据
            val downloadResult = syncRepository.downloadData()
            
            // 4. 合并数据
            if (uploadResult.isSuccess() && downloadResult.isSuccess()) {
                val mergedData = mergeRemoteData(downloadResult.getOrNull())
                updateLocalData(mergedData)
                
                // 5. 同步智能化数据
                syncIntelligenceData()
                
                updateLastSyncTime()
                Result.success()
            } else {
                Result.retry()
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行增量同步
     */
    private suspend fun performIncrementalSync(): Result {
        return try {
            val lastSyncTime = getLastSyncTime()
            val incrementalData = inputRepository.getInputsSince(lastSyncTime)
            
            if (incrementalData.isEmpty()) {
                return Result.success()
            }

            val syncResult = syncRepository.uploadData(incrementalData)
            
            when (syncResult) {
                is SyncResult.Success -> {
                    updateLastSyncTime()
                    Result.success()
                }
                is SyncResult.Error -> {
                    handleSyncError(syncResult)
                    Result.retry()
                }
                else -> Result.success()
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行全量同步
     */
    private suspend fun performFullSync(): Result {
        return try {
            // 1. 获取所有本地数据
            val allLocalData = getAllLocalData()
            
            // 2. 全量上传
            val uploadResult = syncRepository.fullUpload(allLocalData)
            
            // 3. 全量下载
            val downloadResult = syncRepository.fullDownload()
            
            // 4. 处理冲突和合并
            if (uploadResult.isSuccess() && downloadResult.isSuccess()) {
                val remoteData = downloadResult.getOrNull()
                val conflictResolution = resolveConflicts(allLocalData, remoteData)
                
                updateLocalData(conflictResolution.mergedData)
                
                // 5. 同步智能化数据
                syncIntelligenceData()
                
                updateLastSyncTime()
                Result.success()
            } else {
                Result.failure(Exception("Full sync failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行智能化数据同步
     */
    private suspend fun performIntelligenceSync(): Result {
        return try {
            // 1. 同步用户偏好
            val userPreferences = intelligenceEngine.config.first()
            syncRepository.syncUserPreferences(userPreferences)
            
            // 2. 同步学习数据
            val learningData = getLearningData()
            syncRepository.syncLearningData(learningData)
            
            // 3. 同步推荐模型
            val recommendationModels = getRecommendationModels()
            syncRepository.syncRecommendationModels(recommendationModels)
            
            Result.success()

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 同步智能化数据
     */
    private suspend fun syncIntelligenceData() {
        try {
            // 获取智能化配置和统计数据
            val config = intelligenceEngine.config.first()
            val stats = intelligenceEngine.getPerformanceStats()
            
            // 同步到服务器
            syncRepository.syncIntelligenceData(config, stats)
            
        } catch (e: Exception) {
            // 静默处理同步失败
        }
    }

    /**
     * 检查是否应该执行自动同步
     */
    private suspend fun shouldPerformAutoSync(): Boolean {
        // 1. 检查网络状态
        val networkInfo = getNetworkInfo()
        if (!networkInfo.isConnected) return false
        
        // 2. 检查电池状态
        val batteryInfo = getBatteryInfo()
        if (batteryInfo.isLow) return false
        
        // 3. 检查是否有新数据
        val lastSyncTime = getLastSyncTime()
        val newDataCount = inputRepository.getNewDataCountSince(lastSyncTime)
        
        return newDataCount > 0
    }

    /**
     * 获取本地同步数据
     */
    private suspend fun getLocalSyncData(): List<SyncData> {
        val lastSyncTime = getLastSyncTime()
        return inputRepository.getInputsSince(lastSyncTime).map { input ->
            SyncData(
                id = input.id,
                type = SyncDataType.INPUT,
                data = input,
                timestamp = input.timestamp,
                metadata = mapOf(
                    "application" to input.sessionId,
                    "inputType" to input.inputType,
                    "category" to input.category
                )
            )
        }
    }

    /**
     * 获取所有本地数据
     */
    private suspend fun getAllLocalData(): List<SyncData> {
        val inputs = inputRepository.getAllInputs()
        val sessions = inputRepository.getAllSessions()
        
        return (inputs.map { input ->
            SyncData(
                id = input.id,
                type = SyncDataType.INPUT,
                data = input,
                timestamp = input.timestamp,
                metadata = mapOf(
                    "sessionId" to input.sessionId,
                    "inputType" to input.inputType,
                    "category" to input.category
                )
            )
        } + sessions.map { session ->
            SyncData(
                id = session.id,
                type = SyncDataType.SESSION,
                data = session,
                timestamp = session.startTime,
                metadata = mapOf(
                    "application" to session.application,
                    "inputCount" to session.inputCount,
                    "duration" to session.duration
                )
            )
        })
    }

    /**
     * 获取学习数据
     */
    private suspend fun getLearningData(): List<LearningData> {
        // 这里从智能化引擎获取学习数据
        return emptyList() // 示例
    }

    /**
     * 获取推荐模型
     */
    private suspend fun getRecommendationModels(): List<RecommendationModel> {
        // 这里从智能化引擎获取推荐模型
        return emptyList() // 示例
    }

    /**
     * 解决数据冲突
     */
    private suspend fun resolveConflicts(
        localData: List<SyncData>,
        remoteData: List<SyncData>?
    ): ConflictResolution {
        // 这里实现冲突解决逻辑
        return ConflictResolution(
            conflicts = emptyList(),
            mergedData = localData + (remoteData ?: emptyList()),
            resolutionStrategy = "prefer_local"
        )
    }

    /**
     * 更新本地数据
     */
    private suspend fun updateLocalData(data: List<SyncData>) {
        data.forEach { syncData ->
            when (syncData.type) {
                SyncDataType.INPUT -> {
                    if (syncData.data is InputRecord) {
                        inputRepository.insertOrUpdateInput(syncData.data)
                    }
                }
                SyncDataType.SESSION -> {
                    if (syncData.data is InputSession) {
                        inputRepository.insertOrUpdateSession(syncData.data)
                    }
                }
                else -> {
                    // 忽略其他类型
                }
            }
        }
    }

    /**
     * 处理同步错误
     */
    private fun handleSyncError(error: SyncResult.Error) {
        // 记录错误日志
        // 发送通知（如果需要）
        // 更新同步状态
    }

    /**
     * 更新最后同步时间
     */
    private suspend fun updateLastSyncTime() {
        // 更新最后同步时间到本地存储
    }

    /**
     * 获取最后同步时间
     */
    private suspend fun getLastSyncTime(): Long {
        // 从本地存储获取最后同步时间
        return 0L // 示例
    }

    /**
     * 获取网络信息
     */
    private fun getNetworkInfo(): NetworkInfo {
        // 获取当前网络状态
        return NetworkInfo(isConnected = true, networkType = "WiFi")
    }

    /**
     * 获取电池信息
     */
    private fun getBatteryInfo(): BatteryInfo {
        // 获取当前电池状态
        return BatteryInfo(level = 80, isLow = false, isCharging = false)
    }

    /**
     * 合并远程数据
     */
    private suspend fun mergeRemoteData(remoteData: Any?): List<SyncData> {
        // 实现远程数据合并逻辑
        return emptyList() // 示例
    }
}

/**
 * 同步调度器
 */
@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager
) {

    /**
     * 调度定期同步
     */
    fun schedulePeriodicSync(
        intervalMinutes: Long = SyncWorker.DEFAULT_SYNC_INTERVAL,
        flexMinutes: Long = 5L
    ) {
        val periodicRequest = SyncWorker.buildPeriodicRequest(intervalMinutes, flexMinutes)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    /**
     * 调度立即同步
     */
    fun scheduleImmediateSync(
        syncType: SyncType = SyncType.MANUAL,
        priority: Priority = Priority.HIGH
    ) {
        val immediateRequest = SyncWorker.buildRequest(syncType, priority)
            .build()
        
        workManager.enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }

    /**
     * 调度增量同步
     */
    fun scheduleIncrementalSync() {
        val incrementalRequest = SyncWorker.buildRequest(
            SyncType.INCREMENTAL,
            Priority.MEDIUM
        ).build()
        
        workManager.enqueueUniqueWork(
            "incremental_sync",
            ExistingWorkPolicy.REPLACE,
            incrementalRequest
        )
    }

    /**
     * 取消所有同步任务
     */
    fun cancelAllSync() {
        workManager.cancelAllWorkByTag("sync")
    }

    /**
     * 获取同步状态
     */
    fun getSyncStatus(): Flow<SyncStatus> {
        return workManager.getWorkInfosByTag("sync")
            .map { workInfos ->
                if (workInfos.isEmpty()) {
                    SyncStatus(Status.IDLE)
                } else {
                    val runningWork = workInfos.filter { it.state == WorkInfo.State.RUNNING }
                    if (runningWork.isNotEmpty()) {
                        SyncStatus(Status.SYNCING)
                    } else {
                        SyncStatus(Status.SUCCESS)
                    }
                }
            }
    }
}

/**
 * 同步数据类型
 */
enum class SyncDataType {
    INPUT,
    SESSION,
    USER_PREFERENCES,
    LEARNING_DATA,
    RECOMMENDATION_MODELS
}

/**
 * 同步类型
 */
enum class SyncType {
    AUTOMATIC,
    MANUAL,
    INCREMENTAL,
    FULL,
    INTELLIGENCE
}

/**
 * 同步结果
 */
sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String, val code: Int = 0) : SyncResult()
    data class PartialSuccess(val syncedCount: Int, val totalCount: Int) : SyncResult()
}

/**
 * 冲突解决结果
 */
data class ConflictResolution(
    val conflicts: List<Conflict>,
    val mergedData: List<SyncData>,
    val resolutionStrategy: String
)

/**
 * 冲突信息
 */
data class Conflict(
    val id: String,
    val type: SyncDataType,
    val localData: Any,
    val remoteData: Any,
    val conflictType: ConflictType
)

/**
 * 冲突类型
 */
enum class ConflictType {
    CREATE_CONFLICT,
    UPDATE_CONFLICT,
    DELETE_CONFLICT,
    VERSION_CONFLICT
}

/**
 * 同步数据
 */
data class SyncData(
    val id: String,
    val type: SyncDataType,
    val data: Any,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 网络信息
 */
data class NetworkInfo(
    val isConnected: Boolean,
    val networkType: String,
    val signalStrength: Int = 0
)

/**
 * 电池信息
 */
data class BatteryInfo(
    val level: Int,
    val isLow: Boolean,
    val isCharging: Boolean
)

/**
 * 学习数据
 */
data class LearningData(
    val id: String,
    val type: String,
    val data: Map<String, Any>,
    val timestamp: Long
)

/**
 * 推荐模型
 */
data class RecommendationModel(
    val id: String,
    val type: String,
    val version: String,
    val data: ByteArray,
    val metadata: Map<String, Any>
)