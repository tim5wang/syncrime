package com.syncrime.shared.util

import androidx.room.TypeConverter

/**
 * Room 类型转换器
 */
class Converters {
    
    /**
     * String List 转换 (使用 ||| 分隔符)
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString("|||")
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split("|||")
        }
    }
    
    /**
     * String Map 转换
     */
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return value.entries.joinToString("|||") { "${it.key}=${it.value}" }
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return if (value.isEmpty()) {
            emptyMap()
        } else {
            value.split("|||").associate { 
                val (key, val1) = it.split("=", limit = 2)
                key to val1
            }
        }
    }
}
