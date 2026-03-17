package com.syncrime.android

import android.app.Application
import com.syncrime.android.sync.SyncScheduler

class SyncRimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化后台同步调度
        SyncScheduler.initialize(this)
    }
}
