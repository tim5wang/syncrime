package com.syncrime.android.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import android.text.InputType
import android.util.Log
import java.util.regex.Pattern

/**
 * 输入过滤器管理器
 * 
 * 负责过滤敏感字段和敏感内容，保护用户隐私
 */
class InputFilterManager {
    
    companion object {
        private const val TAG = "InputFilterManager"
        
        // 敏感信息正则表达式
        private val PATTERNS = mapOf(
            "email" to Pattern.compile(
                "[a-zA-Z0-9._-]+@[a-z0-9]+\\.[a-z]{2,4}"
            ),
            "phone" to Pattern.compile(
                "(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}"
            ),
            "id_card" to Pattern.compile(
                "[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]"
            ),
            "bank_card" to Pattern.compile(
                "\\d{16,19}"
            ),
            "password_url" to Pattern.compile(
                "(password|passwd|pwd|secret|token)[=:\\s]",
                Pattern.CASE_INSENSITIVE
            )
        )
        
        // 敏感词库（示例，实际应该从配置加载）
        private val SENSITIVE_WORDS = setOf(
            "密码", "password", "passwd", "pwd",
            "银行卡", "credit card",
            "身份证", "id card",
            "验证码", "verification code"
        )
    }
    
    /**
     * 检查是否是敏感字段
     */
    fun isSensitiveField(node: AccessibilityNodeInfo): Boolean {
        // 检查输入类型
        val inputType = node.inputType
        
        // 密码类字段
        if (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 ||
            inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD != 0 ||
            inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD != 0) {
            Log.d(TAG, "检测到密码字段")
            return true
        }
        
        // 邮箱地址字段
        if (inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS != 0) {
            Log.d(TAG, "检测到邮箱字段")
            return true
        }
        
        // 数字密码（可能包含银行卡号等）
        if (inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0) {
            Log.d(TAG, "检测到数字密码字段")
            return true
        }
        
        // 检查节点 ID 是否包含敏感关键词
        val viewIdResourceName = node.viewIdResourceName ?: ""
        if (containsSensitiveKeyword(viewIdResourceName)) {
            Log.d(TAG, "视图 ID 包含敏感词：$viewIdResourceName")
            return true
        }
        
        // 检查节点文本是否包含敏感关键词
        val hintText = node.hintText?.toString() ?: ""
        if (containsSensitiveKeyword(hintText)) {
            Log.d(TAG, "提示文本包含敏感词：$hintText")
            return true
        }
        
        return false
    }
    
    /**
     * 检查内容是否包含敏感信息
     */
    fun containsSensitiveContent(content: String): Boolean {
        // 检查敏感词
        if (containsSensitiveKeyword(content)) {
            return true
        }
        
        // 检查正则匹配
        for ((name, pattern) in PATTERNS) {
            if (pattern.matcher(content).find()) {
                Log.d(TAG, "检测到敏感内容类型：$name")
                return true
            }
        }
        
        // 检查长度（过长的数字串可能是卡号）
        if (content.length > 15 && content.all { it.isDigit() }) {
            Log.d(TAG, "检测到长数字串，可能是敏感信息")
            return true
        }
        
        return false
    }
    
    /**
     * 检查是否包含敏感关键词
     */
    private fun containsSensitiveKeyword(text: String): Boolean {
        if (text.isBlank()) return false
        
        val lowerText = text.lowercase()
        for (sensitiveWord in SENSITIVE_WORDS) {
            if (lowerText.contains(sensitiveWord.lowercase())) {
                return true
            }
        }
        return false
    }
    
    /**
     * 对内容进行脱敏处理
     */
    fun desensitizeContent(content: String): String {
        var result = content
        
        // 替换邮箱
        result = PATTERNS["email"]?.matcher(result)?.replaceAll("[EMAIL]") ?: result
        
        // 替换手机号
        result = PATTERNS["phone"]?.matcher(result)?.replaceAll("[PHONE]") ?: result
        
        // 替换身份证号
        result = PATTERNS["id_card"]?.matcher(result)?.replaceAll("[ID_CARD]") ?: result
        
        // 替换银行卡号
        result = PATTERNS["bank_card"]?.matcher(result)?.replaceAll("[BANK_CARD]") ?: result
        
        return result
    }
    
    /**
     * 获取敏感内容类型
     */
    fun getSensitiveContentType(content: String): String? {
        for ((name, pattern) in PATTERNS) {
            if (pattern.matcher(content).find()) {
                return name
            }
        }
        return null
    }
}
