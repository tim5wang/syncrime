package com.syncrime.android

import android.app.Application
import android.util.Log
import com.syncrime.android.network.ApiClient
import com.syncrime.android.network.AuthService

class SyncRimeApplication : Application() {
    
    companion object {
        private const val TAG = "SyncRimeApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // 初始化网络服务
            ApiClient.baseUrl = BuildConfig.API_BASE_URL + "/api/v1"
            Log.d(TAG, "API URL: ${ApiClient.baseUrl}")
            
            // 初始化认证服务
            AuthService.init(this)
            Log.d(TAG, "Auth service initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
        }
    }
}
