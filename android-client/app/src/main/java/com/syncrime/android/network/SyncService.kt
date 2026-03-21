package com.syncrime.android.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object SyncService {
    private const val TAG = "SyncService"
    
    suspend fun pushRecords(records: List<InputRecordData>, deviceId: String): SyncResult = withContext(Dispatchers.IO) {
        try {
            val recordsJson = JSONArray()
            records.forEach { r -> recordsJson.put(JSONObject().apply {
                put("content", r.content)
                put("app", r.app ?: "")
                put("category", r.category ?: "")
                put("timestamp", r.timestamp)
                r.tags?.let { put("tags", JSONArray(it)) }
            })}
            val body = JSONObject().apply { put("records", recordsJson); put("deviceId", deviceId) }
            val response = ApiClient.post("/sync/push", body)
            if (response.isSuccess) SyncResult.Success(response.toJson()!!.getJSONObject("data").getInt("syncedCount"))
            else SyncResult.Error(response.getError())
        } catch (e: Exception) { SyncResult.Error(e.message ?: "推送失败") }
    }
    
    suspend fun pullRecords(lastSyncTime: Long = 0, limit: Int = 1000): PullResult = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.get("/sync/pull?lastSyncTime=$lastSyncTime&limit=$limit")
            if (response.isSuccess) {
                val data = response.toJson()!!.getJSONObject("data")
                val arr = data.getJSONArray("records")
                val records = (0 until arr.length()).map { i ->
                    val item = arr.getJSONObject(i)
                    InputRecordData(
                        id = item.optLong("id"),
                        content = item.getString("content"),
                        app = item.optString("app"),
                        category = item.optString("category"),
                        timestamp = item.getLong("timestamp"),
                        tags = item.optJSONArray("tags")?.let { a -> (0 until a.length()).map { a.getString(it) } }
                    )
                }
                PullResult.Success(records)
            } else PullResult.Error(response.getError())
        } catch (e: Exception) { PullResult.Error(e.message ?: "拉取失败") }
    }
    
    suspend fun getSyncStatus(): SyncStatus? = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.get("/sync/status")
            if (response.isSuccess) {
                val data = response.toJson()!!.getJSONObject("data")
                SyncStatus(
                    totalRecords = data.getInt("totalRecords"),
                    lastSync = data.optJSONObject("lastSync")?.let {
                        LastSync(it.optString("sync_type"), it.optInt("record_count"), it.optString("synced_at"))
                    }
                )
            } else null
        } catch (e: Exception) { null }
    }
}

data class InputRecordData(
    val id: Long = 0,
    val content: String,
    val app: String? = null,
    val category: String? = null,
    val timestamp: Long,
    val tags: List<String>? = null
)
sealed class SyncResult { data class Success(val syncedCount: Int) : SyncResult(); data class Error(val message: String) : SyncResult() }
sealed class PullResult { data class Success(val records: List<InputRecordData>) : PullResult(); data class Error(val message: String) : PullResult() }
data class SyncStatus(val totalRecords: Int, val lastSync: LastSync?)
data class LastSync(val syncType: String, val recordCount: Int, val syncedAt: String)
