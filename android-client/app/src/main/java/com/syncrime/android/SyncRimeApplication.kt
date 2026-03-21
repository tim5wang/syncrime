package com.syncrime.android

import android.app.Application
import android.util.Log
import com.syncrime.android.network.ApiClient
import com.syncrime.android.network.AuthService

class SyncRimeApplication : Application() {
    
    companion object { private const val TAG = "SyncRimeApp" }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== Application onCreate ===")
        
        try {
            ApiClient.baseUrl = "https://syncrime-api.claw.carc.top/api/v1"
            AuthService.init(this)
            Log.d(TAG, "Init success, API: ${ApiClient.baseUrl}")
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
        }
    }
}