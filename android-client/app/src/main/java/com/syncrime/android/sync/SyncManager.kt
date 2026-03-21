package com.syncrime.android.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.syncrime.android.network.ApiClient
import com.syncrime.android.network.AuthService
import com.syncrime.shared.data.local.AppDatabase
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SyncManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_WORK_NAME = "syncrime_sync"
        
        @Volatile
        private var INSTANCE: SyncManager? = null
        
        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 启动自动同步（每小时同步一次）
     */
    fun startAutoSync() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            1, TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        
        Log.d(TAG, "自动同步已启动")
    }
    
    /**
     * 停止自动同步
     */
    fun stopAutoSync() {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        Log.d(TAG, "自动同步已停止")
    }
    
    /**
     * 手动触发同步
     */
    fun syncNow() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(syncRequest)
        Log.d(TAG, "手动同步已触发")
    }
}

/**
 * 同步 Worker
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SyncWorker"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "开始同步")
        
        // 检查登录状态
        if (!AuthService.isLoggedIn()) {
            Log.d(TAG, "未登录，跳过同步")
            return Result.success()
        }
        
        return try {
            // 推送本地数据到云端
            val pushResult = pushToServer()
            if (!pushResult) {
                Log.e(TAG, "推送失败")
                return Result.retry()
            }
            
            // 从云端拉取数据
            val pullResult = pullFromServer()
            if (!pullResult) {
                Log.e(TAG, "拉取失败")
                return Result.retry()
            }
            
            Log.i(TAG, "同步完成")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "同步失败", e)
            Result.retry()
        }
    }
    
    private suspend fun pushToServer(): Boolean {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val records = database.inputDao().getRecentSync(100)
            
            if (records.isEmpty()) {
                Log.d(TAG, "没有需要推送的数据")
                return true
            }
            
            val recordsJson = JSONArray()
            records.forEach { record ->
                recordsJson.put(JSONObject().apply {
                    put("content", record.content)
                    put("app", record.application)
                    put("timestamp", record.createdAt)
                })
            }
            
            val deviceId = android.provider.Settings.Secure.getString(
                applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            val body = JSONObject().apply {
                put("records", recordsJson)
                put("deviceId", deviceId)
            }
            
            val response = ApiClient.post("/sync/push", body)
            
            if (response.isSuccess) {
                Log.i(TAG, "推送成功: ${records.size} 条记录")
                true
            } else {
                Log.e(TAG, "推送失败: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "推送异常", e)
            false
        }
    }
    
    private suspend fun pullFromServer(): Boolean {
        return try {
            val response = ApiClient.get("/sync/pull?limit=1000")
            
            if (response.isSuccess) {
                val json = response.toJson()!!
                val data = json.getJSONObject("data")
                val recordsArray = data.getJSONArray("records")
                
                val database = AppDatabase.getDatabase(applicationContext)
                
                for (i in 0 until recordsArray.length()) {
                    val item = recordsArray.getJSONObject(i)
                    // 保存到本地（忽略重复）
                    // TODO: 实现去重逻辑
                }
                
                Log.i(TAG, "拉取成功: ${recordsArray.length()} 条记录")
                true
            } else {
                Log.e(TAG, "拉取失败: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "拉取异常", e)
            false
        }
    }
}