package com.syncrime.android

import android.app.Application
import com.syncrime.android.network.ApiClient
import com.syncrime.android.network.AuthService

class SyncRimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化网络服务
        ApiClient.baseUrl = BuildConfig.API_BASE_URL + "/api/v1"
        
        // 初始化认证服务
        AuthService.init(this)
    }
}
