package com.syncrime.android

import android.app.Application
import android.util.Log

class SyncRimeApplication : Application() {
    
    companion object {
        private const val TAG = "SyncRimeApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== Application onCreate ===")
        // 暂时不做任何初始化
    }
}