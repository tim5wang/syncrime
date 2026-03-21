package com.syncrime.android.sync

import android.content.Context
import android.util.Log
import com.syncrime.android.network.ApiClient
import com.syncrime.android.network.AuthService
import com.syncrime.shared.data.local.AppDatabase
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class SyncManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5分钟
        
        @Volatile
        private var INSTANCE: SyncManager? = null
        
        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var lastSyncTime: Long = 0
    private var lastRecordCount: Int = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * 检查并触发增量同步
     * 条件：有新记录 + 距离上次同步超过5分钟
     */
    fun checkAndSync() {
        if (!AuthService.isLoggedIn()) return
        
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val currentCount = database.inputDao().getTotalCountSync()
                
                val hasNewRecords = currentCount > lastRecordCount
                val timeElapsed = System.currentTimeMillis() - lastSyncTime > SYNC_INTERVAL_MS
                
                if (hasNewRecords && timeElapsed) {
                    Log.d(TAG, "发现新记录 ($lastRecordCount -> $currentCount)，触发增量同步")
                    incrementalSync(database, lastRecordCount)
                    lastRecordCount = currentCount
                    lastSyncTime = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查同步失败", e)
            }
        }
    }
    
    /**
     * 强制立即同步
     */
    fun syncNow(callback: ((Boolean, String) -> Unit)? = null) {
        if (!AuthService.isLoggedIn()) {
            callback?.invoke(false, "未登录")
            return
        }
        
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val success = incrementalSync(database, 0)
                lastSyncTime = System.currentTimeMillis()
                lastRecordCount = database.inputDao().getTotalCountSync()
                
                withContext(Dispatchers.Main) {
                    callback?.invoke(success, if (success) "同步成功" else "同步失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "同步失败", e)
                withContext(Dispatchers.Main) {
                    callback?.invoke(false, "同步失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 增量同步：只上传新记录
     */
    private suspend fun incrementalSync(database: AppDatabase, fromCount: Int): Boolean {
        return try {
            // 获取需要同步的记录
            val allRecords = database.inputDao().getRecentSync(1000)
            val newRecords = if (fromCount > 0 && fromCount < allRecords.size) {
                allRecords.take(fromCount) // 取最新的记录
            } else {
                allRecords
            }
            
            if (newRecords.isEmpty()) {
                Log.d(TAG, "没有需要同步的记录")
                return true
            }
            
            val recordsJson = JSONArray()
            newRecords.forEach { record ->
                recordsJson.put(JSONObject().apply {
                    put("content", record.content)
                    put("app", record.application)
                    put("category", record.category ?: "")
                    put("timestamp", record.createdAt)
                })
            }
            
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            
            val body = JSONObject().apply {
                put("records", recordsJson)
                put("deviceId", deviceId)
            }
            
            val response = ApiClient.post("/sync/push", body)
            
            if (response.isSuccess) {
                val data = response.toJson()?.getJSONObject("data")
                val syncedCount = data?.optInt("syncedCount", newRecords.size) ?: newRecords.size
                Log.i(TAG, "✅ 增量同步成功: $syncedCount 条记录")
                true
            } else {
                Log.e(TAG, "同步失败: ${response.code} - ${response.getError()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "增量同步异常", e)
            false
        }
    }
    
    /**
     * 从云端拉取数据
     */
    suspend fun pullFromCloud(): Boolean {
        if (!AuthService.isLoggedIn()) return false
        
        return try {
            val response = ApiClient.get("/sync/pull?limit=1000")
            
            if (response.isSuccess) {
                val json = response.toJson()!!
                val data = json.getJSONObject("data")
                val recordsArray = data.getJSONArray("records")
                
                val database = AppDatabase.getDatabase(context)
                
                for (i in 0 until recordsArray.length()) {
                    val item = recordsArray.getJSONObject(i)
                    // TODO: 实现去重保存
                }
                
                Log.i(TAG, "✅ 拉取成功: ${recordsArray.length()} 条记录")
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
    
    /**
     * 同步剪藏到云端
     */
    fun syncClip(clipId: Long, action: String, callback: ((Boolean) -> Unit)? = null) {
        if (!AuthService.isLoggedIn()) {
            callback?.invoke(false)
            return
        }
        
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val clip = database.clipDao().getByIdSync(clipId)
                
                if (clip == null) {
                    withContext(Dispatchers.Main) { callback?.invoke(false) }
                    return@launch
                }
                
                // TODO: 调用云端 API 保存剪藏
                Log.d(TAG, "同步剪藏: ${clip.title}")
                
                withContext(Dispatchers.Main) { callback?.invoke(true) }
            } catch (e: Exception) {
                Log.e(TAG, "同步剪藏失败", e)
                withContext(Dispatchers.Main) { callback?.invoke(false) }
            }
        }
    }
}