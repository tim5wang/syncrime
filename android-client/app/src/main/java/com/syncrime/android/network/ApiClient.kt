package com.syncrime.android.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val TAG = "ApiClient"
    var baseUrl: String = "https://syncrime-api.claw.carc.top/api/v1"
    private var authToken: String? = null
    
    fun setAuthToken(token: String?) { authToken = token }
    fun getAuthToken(): String? = authToken
    
    suspend fun get(endpoint: String): ApiResponse = withContext(Dispatchers.IO) {
        request("GET", endpoint, null)
    }
    
    suspend fun post(endpoint: String, body: JSONObject? = null): ApiResponse = withContext(Dispatchers.IO) {
        request("POST", endpoint, body)
    }
    
    private fun request(method: String, endpoint: String, body: JSONObject?): ApiResponse {
        return try {
            val url = URL("$baseUrl$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            authToken?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            
            if (body != null && (method == "POST" || method == "PUT")) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            
            val code = conn.responseCode
            val response = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            ApiResponse(code, response)
        } catch (e: Exception) {
            Log.e(TAG, "Request failed: $endpoint", e)
            ApiResponse(-1, "{\"error\":\"${e.message}\"}")
        }
    }
}

data class ApiResponse(val code: Int, val body: String) {
    val isSuccess: Boolean get() = code in 200..299
    fun toJson(): JSONObject? = try { JSONObject(body) } catch (e: Exception) { null }
    fun getError(): String = try { JSONObject(body).optString("error", "Unknown error") } catch (e: Exception) { body }
}