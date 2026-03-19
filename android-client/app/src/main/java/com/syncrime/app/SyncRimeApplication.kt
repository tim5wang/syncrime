package com.syncrime.app

import android.app.Application
import com.syncrime.shared.data.local.AppDatabase
import com.syncrime.inputmethod.repository.ClipRepository
import com.syncrime.inputmethod.repository.InputRepository

/**
 * SyncRime 应用入口
 */
class SyncRimeApplication : Application() {
    
    // 数据库
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
    
    // 仓库
    val inputRepository: InputRepository by lazy {
        InputRepository(database.inputDao())
    }
    
    val clipRepository: ClipRepository by lazy {
        ClipRepository(database.clipDao())
    }
    
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
