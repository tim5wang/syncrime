package com.syncrime.android.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.syncrime.android.data.local.dao.InputRecordDao
import com.syncrime.android.data.local.dao.InputSessionDao
import com.syncrime.android.data.local.dao.SyncRecordDao
import com.syncrime.android.data.local.entity.InputRecordEntity
import com.syncrime.android.data.local.entity.InputSessionEntity
import com.syncrime.android.data.local.entity.SyncRecordEntity

/**
 * SyncRime Room 数据库
 * 版本 1: 初始版本
 */
@Database(
    entities = [
        InputSessionEntity::class,
        InputRecordEntity::class,
        SyncRecordEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SyncRimeDatabase : RoomDatabase() {
    
    abstract fun inputSessionDao(): InputSessionDao
    abstract fun inputRecordDao(): InputRecordDao
    abstract fun syncRecordDao(): SyncRecordDao
    
    companion object {
        @Volatile
        private var INSTANCE: SyncRimeDatabase? = null
        
        fun getDatabase(context: Context): SyncRimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SyncRimeDatabase::class.java,
                    "syncrime_database"
                )
                .fallbackToDestructiveMigration() // 开发阶段使用，生产环境需要 proper migrations
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
