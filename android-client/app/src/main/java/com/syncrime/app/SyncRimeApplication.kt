package com.syncrime.app

import android.app.Application

/**
 * SyncRime 应用入口 (简化版)
 */
class SyncRimeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        @Volatile
        private var instance: SyncRimeApplication? = null
        
        fun getInstance(): SyncRimeApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}
