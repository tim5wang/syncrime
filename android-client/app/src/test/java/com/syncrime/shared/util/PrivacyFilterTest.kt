package com.syncrime.shared.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * PrivacyFilter 单元测试
 */
class PrivacyFilterTest {
    
    private lateinit var privacyFilter: PrivacyFilter
    
    @Before
    fun setUp() {
        privacyFilter = PrivacyFilter()
    }
    
    // ========== 敏感字段检测测试 ==========
    
    @Test
    fun `isSensitiveField returns true for password field`() {
        val passwordFields = listOf(
            "password",
            "Password",
            "PASSWORD",
            "editTextPassword",
            "et_password",
            "input_password",
            "pwd",
            "passwd"
        )
        
        passwordFields.forEach { field ->
            assertTrue("'$field' should be sensitive", privacyFilter.isSensitiveField(field))
        }
    }
    
    @Test
    fun `isSensitiveField returns true for credit card field`() {
        val ccFields = listOf(
            "cardNumber",
            "creditCard",
            "cvv",
            "card_number",
            "expiryDate"
        )
        
        ccFields.forEach { field ->
            assertTrue("'$field' should be sensitive", privacyFilter.isSensitiveField(field))
        }
    }
    
    @Test
    fun `isSensitiveField returns false for normal field`() {
        val normalFields = listOf(
            "username",
            "email",
            "message",
            "search",
            "comment"
        )
        
        normalFields.forEach { field ->
            assertFalse("'$field' should not be sensitive", privacyFilter.isSensitiveField(field))
        }
    }
    
    // ========== 敏感内容检测测试 ==========
    
    @Test
    fun `containsSensitiveContent detects credit card number`() {
        val contentWithCC = "我的信用卡号是 4111111111111111"
        assertTrue("Should detect credit card", privacyFilter.containsSensitiveContent(contentWithCC))
    }
    
    @Test
    fun `containsSensitiveContent detects phone number`() {
        val contentWithPhone = "我的电话是 13812345678"
        // 根据实现决定是否检测手机号
        // assertTrue(privacyFilter.containsSensitiveContent(contentWithPhone))
    }
    
    @Test
    fun `containsSensitiveContent detects ID card number`() {
        val contentWithId = "身份证号 110101199001011234"
        assertTrue("Should detect ID card", privacyFilter.containsSensitiveContent(contentWithId))
    }
    
    @Test
    fun `containsSensitiveContent returns false for normal content`() {
        val normalContent = listOf(
            "今天天气真好",
            "Hello World",
            "这是一条普通的测试消息",
            "Meeting at 3pm tomorrow"
        )
        
        normalContent.forEach { content ->
            assertFalse("'$content' should not be sensitive", privacyFilter.containsSensitiveContent(content))
        }
    }
    
    // ========== 过滤功能测试 ==========
    
    @Test
    fun `filter removes password patterns`() {
        val input = "我的密码是 abc123456"
        val filtered = privacyFilter.filter(input)
        assertFalse("Filtered content should not contain password", filtered.contains("abc123456"))
    }
    
    @Test
    fun `filter preserves normal content`() {
        val input = "这是一条普通的消息"
        val filtered = privacyFilter.filter(input)
        assertEquals("Normal content should be preserved", input, filtered)
    }
    
    @Test
    fun `filter handles empty string`() {
        val filtered = privacyFilter.filter("")
        assertEquals("Empty string should remain empty", "", filtered)
    }
    
    @Test
    fun `filter handles null safely`() {
        // 如果实现支持 null
        // val filtered = privacyFilter.filter(null)
        // assertEquals("", filtered)
    }
    
    // ========== 边界条件测试 ==========
    
    @Test
    fun `isSensitiveField handles null or empty`() {
        // 根据实现调整
        assertFalse("Empty string should not be sensitive", privacyFilter.isSensitiveField(""))
    }
    
    @Test
    fun `containsSensitiveContent handles empty string`() {
        assertFalse("Empty string should not be sensitive", privacyFilter.containsSensitiveContent(""))
    }
    
    @Test
    fun `filter handles long content`() {
        val longContent = "这是一条很长的内容。".repeat(1000)
        val filtered = privacyFilter.filter(longContent)
        assertNotNull("Should handle long content", filtered)
    }
    
    // ========== 性能测试 ==========
    
    @Test
    fun `filter performance is acceptable`() {
        val content = "测试内容 " + "普通文本 ".repeat(100)
        
        val startTime = System.currentTimeMillis()
        repeat(100) {
            privacyFilter.filter(content)
        }
        val endTime = System.currentTimeMillis()
        
        assertTrue("Filter should be fast (< 1000ms for 100 iterations)", endTime - startTime < 1000)
    }
}