package com.syncrime.android.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object RemoteAIService {
    private const val TAG = "RemoteAIService"
    
    suspend fun completion(context: String, maxLength: Int = 100): AIResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("context", context)
                put("maxLength", maxLength)
            }
            val response = ApiClient.post("/ai/completion", body)
            if (response.isSuccess) {
                val data = response.toJson()!!.getJSONObject("data")
                AIResult.Success(data.getString("text"))
            } else AIResult.Error(response.getError())
        } catch (e: Exception) { AIResult.Error(e.message ?: "续写失败") }
    }
    
    suspend fun summarize(content: String, maxLength: Int = 200): AIResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("content", content)
                put("maxLength", maxLength)
            }
            val response = ApiClient.post("/ai/summarize", body)
            if (response.isSuccess) {
                val data = response.toJson()!!.getJSONObject("data")
                AIResult.Success(data.getString("summary"))
            } else AIResult.Error(response.getError())
        } catch (e: Exception) { AIResult.Error(e.message ?: "摘要失败") }
    }
    
    suspend fun chat(messages: List<Pair<String, String>>): AIResult = withContext(Dispatchers.IO) {
        try {
            val messagesJson = JSONArray()
            messages.forEach { (role, content) ->
                messagesJson.put(JSONObject().apply {
                    put("role", role)
                    put("content", content)
                })
            }
            val body = JSONObject().apply { put("messages", messagesJson) }
            val response = ApiClient.post("/ai/chat", body)
            if (response.isSuccess) {
                val data = response.toJson()!!.getJSONObject("data")
                AIResult.Success(data.getString("message"))
            } else AIResult.Error(response.getError())
        } catch (e: Exception) { AIResult.Error(e.message ?: "对话失败") }
    }
}

sealed class AIResult { data class Success(val text: String) : AIResult(); data class Error(val message: String) : AIResult() }
