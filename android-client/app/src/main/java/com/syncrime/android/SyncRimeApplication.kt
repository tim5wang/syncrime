package com.syncrime.android

import android.app.Application
import android.util.Log
import com.syncrime.android.network.ApiClient
import com.syncrime.android.network.AuthService
import com.syncrime.android.sync.SyncManager

class SyncRimeApplication : Application() {
    
    companion object { private const val TAG = "SyncRimeApp" }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== Application onCreate ===")
        
        try {
            // 初始化网络服务
            ApiClient.baseUrl = "https://syncrime-api.claw.carc.top/api/v1"
            
            // 初始化认证服务
            AuthService.init(this)
            
            // 如果已登录，启动自动同步
            if (AuthService.isLoggedIn()) {
                SyncManager.getInstance(this).startAutoSync()
                Log.d(TAG, "已登录，启动自动同步")
            }
            
            Log.d(TAG, "初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
        }
    }
}