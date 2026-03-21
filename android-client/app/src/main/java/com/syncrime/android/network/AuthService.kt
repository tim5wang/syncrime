package com.syncrime.android.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AuthService {
    private const val TAG = "AuthService"
    private const val PREFS_NAME = "syncrime_auth"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_NICKNAME = "nickname"
    
    private var prefs: SharedPreferences? = null
    private var isInitialized = false
    
    fun init(context: Context) {
        try {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs?.getString(KEY_TOKEN, null)?.let { ApiClient.setAuthToken(it) }
            isInitialized = true
            Log.d(TAG, "AuthService initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "AuthService init failed", e)
        }
    }
    
    private fun ensureInitialized(): Boolean {
        if (!isInitialized || prefs == null) {
            Log.w(TAG, "AuthService not initialized")
            return false
        }
        return true
    }
    
    suspend fun register(email: String, password: String, nickname: String? = null): AuthResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
                nickname?.let { put("nickname", it) }
            }
            val response = ApiClient.post("/auth/register", body)
            if (response.isSuccess) {
                val data = response.toJson()!!.getJSONObject("data")
                saveAuth(data.getString("token"), data.getString("userId"), data.getString("email"), data.optString("nickname", email.split("@")[0]))
                AuthResult.Success
            } else AuthResult.Error(response.getError())
        } catch (e: Exception) { AuthResult.Error(e.message ?: "注册失败") }
    }
    
    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("email", email); put("password", password) }
            val response = ApiClient.post("/auth/login", body)
            if (response.isSuccess) {
                val data = response.toJson()!!.getJSONObject("data")
                saveAuth(data.getString("token"), data.getString("userId"), data.getString("email"), data.optString("nickname", email.split("@")[0]))
                AuthResult.Success
            } else AuthResult.Error(response.getError())
        } catch (e: Exception) { AuthResult.Error(e.message ?: "登录失败") }
    }
    
    fun logout() { 
        prefs?.edit()?.clear()?.apply()
        ApiClient.setAuthToken(null) 
    }
    
    fun isLoggedIn(): Boolean = ApiClient.getAuthToken() != null
    
    fun getUserId(): String? {
        return prefs?.getString(KEY_USER_ID, null)
    }
    
    fun getNickname(): String? {
        return prefs?.getString(KEY_NICKNAME, null)
    }
    
    private fun saveAuth(token: String, userId: String, email: String, nickname: String) {
        prefs?.edit()
            ?.putString(KEY_TOKEN, token)
            ?.putString(KEY_USER_ID, userId)
            ?.putString(KEY_EMAIL, email)
            ?.putString(KEY_NICKNAME, nickname)
            ?.apply()
        ApiClient.setAuthToken(token)
    }
}

sealed class AuthResult { object Success : AuthResult(); data class Error(val message: String) : AuthResult() }
data class UserInfo(val id: String, val email: String, val nickname: String)
