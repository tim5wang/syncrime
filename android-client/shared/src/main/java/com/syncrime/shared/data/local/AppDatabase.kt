package com.syncrime.shared.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.syncrime.shared.data.local.dao.ClipDao
import com.syncrime.shared.data.local.dao.InputDao
import com.syncrime.shared.data.local.dao.SearchHistoryDao
import com.syncrime.shared.data.local.entity.SearchHistoryEntity
import com.syncrime.shared.model.InputRecord
import com.syncrime.shared.model.KnowledgeClip
import com.syncrime.shared.util.Converters

/**
 * SyncRime 应用数据库
 */
@Database(
    entities = [
        InputRecord::class,
        KnowledgeClip::class,
        SearchHistoryEntity::class
    ],
    version = 3,  // 增加版本号以应用索引更改
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * 输入记录 DAO
     */
    abstract fun inputDao(): InputDao
    
    /**
     * 知识剪藏 DAO
     */
    abstract fun clipDao(): ClipDao
    
    /**
     * 搜索历史 DAO
     */
    abstract fun searchHistoryDao(): SearchHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库单例
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "syncrime_database"
                )
                    .fallbackToDestructiveMigration() // 开发阶段使用
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
