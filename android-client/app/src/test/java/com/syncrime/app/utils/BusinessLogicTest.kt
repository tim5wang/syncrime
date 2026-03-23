package com.syncrime.app.utils

import com.syncrime.app.presentation.formatTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * 业务逻辑单元测试
 */
class BusinessLogicTest {
    
    // ========== 时间格式化测试 ==========
    
    @Test
    fun formatTime_formatsCorrectly() {
        // 使用固定的测试时间戳
        val timestamp = 1640995200000L // 2022-01-01 00:00:00 UTC
        val expected = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
        
        val result = formatTime(timestamp)
        
        // 验证格式符合预期
        assertEquals(expected, result)
    }
    
    @Test
    fun formatTime_handlesDifferentTimestamps() {
        val timestamps = listOf(
            1640995200000L, // 2022-01-01 00:00:00
            1641081600000L, // 2022-01-02 00:00:00
            1672531200000L, // 2023-01-01 00:00:00
        )
        
        timestamps.forEach { timestamp ->
            val result = formatTime(timestamp)
            // 验证格式长度正确 (MM-dd HH:mm = 11 chars: "12-31 23:59")
            assert(result.length >= 8 && result.length <= 11) { "Invalid time format: $result" }
        }
    }
    
    @Test
    fun formatTime_handlesCurrentTime() {
        val currentTime = System.currentTimeMillis()
        val result = formatTime(currentTime)
        
        // 验证当前时间格式化不会崩溃且有合理输出
        assert(result.isNotEmpty()) { "Result should not be empty" }
        assert(!result.contains("null")) { "Result should not contain null" }
    }
    
    @Test
    fun formatTime_handlesEdgeCases() {
        val edgeCases = listOf(
            0L,                           // Unix epoch
            2147483647000L,              // Max 32-bit timestamp (2038)
            System.currentTimeMillis(),    // Current time
            System.currentTimeMillis() - 1000, // 1 second ago
            System.currentTimeMillis() + 1000  // 1 second in future
        )
        
        edgeCases.forEach { timestamp ->
            val result = formatTime(timestamp)
            assert(result.isNotEmpty()) { "Result should not be empty for timestamp $timestamp" }
        }
    }
}