package com.syncrime.android.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * SyncRime API 客户端
 */
object ApiClient {
    private const val TAG = "ApiClient"
    
    // 后端服务地址
    var baseUrl: String = "https://syncrime-api.claw.carc.top/api/v1"
    
    // JWT Token
    private var authToken: String? = null
    
    /**
     * 设置认证 Token
     */
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    /**
     * 获取认证 Token
     */
    fun getAuthToken(): String? = authToken
    
    /**
     * GET 请求
     */
    suspend fun get(endpoint: String): ApiResponse = withContext(Dispatchers.IO) {
        request("GET", endpoint, null)
    }
    
    /**
     * POST 请求
     */
    suspend fun post(endpoint: String, body: JSONObject? = null): ApiResponse = withContext(Dispatchers.IO) {
        request("POST", endpoint, body)
    }
    
    /**
     * DELETE 请求
     */
    suspend fun delete(endpoint: String): ApiResponse = withContext(Dispatchers.IO) {
        request("DELETE", endpoint, null)
    }
    
    /**
     * 通用请求方法
     */
    private fun request(method: String, endpoint: String, body: JSONObject?): ApiResponse {
        val url = URL("$baseUrl$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        
        return try {
            connection.requestMethod = method
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            
            // 添加认证头
            authToken?.let {
                connection.setRequestProperty("Authorization", "Bearer $it")
            }
            
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            // 发送请求体
            if (body != null && (method == "POST" || method == "PUT")) {
                connection.doOutput = true
                connection.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }
            
            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            
            ApiResponse(responseCode, response)
        } catch (e: Exception) {
            Log.e(TAG, "请求失败: $endpoint", e)
            ApiResponse(-1, "{\"error\":\"${e.message}\"}")
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * 健康检查
     */
    suspend fun healthCheck(): Boolean {
        return try {
            val response = get("/../health")
            response.code == 200
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * API 响应封装
 */
data class ApiResponse(
    val code: Int,
    val body: String
) {
    val isSuccess: Boolean get() = code in 200..299
    
    fun toJson(): JSONObject? {
        return try {
            JSONObject(body)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getError(): String {
        return try {
            JSONObject(body).optString("error", "Unknown error")
        } catch (e: Exception) {
            body
        }
    }
}