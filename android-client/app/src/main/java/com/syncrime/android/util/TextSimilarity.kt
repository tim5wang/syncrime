package com.syncrime.android.util

/**
 * 文本相似度计算工具
 * 
 * 支持：
 * - Levenshtein 距离
 * - Jaccard 相似度
 * - 余弦相似度（基于字符 n-gram）
 */
object TextSimilarity {
    
    /**
     * 计算两个文本的综合相似度 (0.0 - 1.0)
     * 
     * 综合考虑：
     * - 编辑距离相似度 (权重 0.4)
     * - Jaccard 相似度 (权重 0.3)
     * - 包含关系 (权重 0.3)
     */
    fun calculateSimilarity(text1: String, text2: String): Float {
        if (text1.isEmpty() || text2.isEmpty()) return 0f
        if (text1 == text2) return 1f
        
        // 归一化文本
        val normalized1 = text1.trim().lowercase()
        val normalized2 = text2.trim().lowercase()
        
        if (normalized1 == normalized2) return 1f
        
        // 编辑距离相似度
        val levenshteinSim = levenshteinSimilarity(normalized1, normalized2)
        
        // Jaccard 相似度（基于字符）
        val jaccardSim = jaccardSimilarity(normalized1, normalized2)
        
        // 包含关系得分
        val containsSim = containsSimilarity(normalized1, normalized2)
        
        // 综合相似度
        return levenshteinSim * 0.4f + jaccardSim * 0.3f + containsSim * 0.3f
    }
    
    /**
     * Levenshtein 编辑距离相似度
     */
    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        val distance = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        return 1f - distance.toFloat() / maxLen
    }
    
    /**
     * Levenshtein 编辑距离
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        if (len1 == 0) return len2
        if (len2 == 0) return len1
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // 删除
                    dp[i][j - 1] + 1,      // 插入
                    dp[i - 1][j - 1] + cost // 替换
                )
            }
        }
        
        return dp[len1][len2]
    }
    
    /**
     * Jaccard 相似度（基于字符 n-gram）
     */
    private fun jaccardSimilarity(s1: String, s2: String, n: Int = 2): Float {
        if (s1.length < n || s2.length < n) {
            // 短文本直接比较字符集合
            val set1 = s1.toSet()
            val set2 = s2.toSet()
            val intersection = set1.intersect(set2).size
            val union = set1.union(set2).size
            return if (union == 0) 0f else intersection.toFloat() / union
        }
        
        val ngrams1 = getNgrams(s1, n)
        val ngrams2 = getNgrams(s2, n)
        
        val intersection = ngrams1.intersect(ngrams2).size
        val union = ngrams1.union(ngrams2).size
        
        return if (union == 0) 0f else intersection.toFloat() / union
    }
    
    /**
     * 获取文本的 n-gram 集合
     */
    private fun getNgrams(text: String, n: Int): Set<String> {
        if (text.length < n) return setOf(text)
        return (0..text.length - n).map { text.substring(it, it + n) }.toSet()
    }
    
    /**
     * 包含关系相似度
     */
    private fun containsSimilarity(s1: String, s2: String): Float {
        val len1 = s1.length
        val len2 = s2.length
        
        return when {
            s1 == s2 -> 1f
            s1.contains(s2) -> {
                // s1 包含 s2，相似度取决于被包含部分的比例
                len2.toFloat() / len1
            }
            s2.contains(s1) -> {
                // s2 包含 s1
                len1.toFloat() / len2
            }
            // 检查公共前缀/后缀
            else -> {
                val prefixLen = commonPrefixLength(s1, s2)
                val suffixLen = commonSuffixLength(s1, s2)
                val maxCommon = maxOf(prefixLen, suffixLen)
                maxCommon.toFloat() / maxOf(len1, len2)
            }
        }
    }
    
    /**
     * 公共前缀长度
     */
    private fun commonPrefixLength(s1: String, s2: String): Int {
        val minLen = minOf(s1.length, s2.length)
        var i = 0
        while (i < minLen && s1[i] == s2[i]) i++
        return i
    }
    
    /**
     * 公共后缀长度
     */
    private fun commonSuffixLength(s1: String, s2: String): Int {
        var i = 1
        val maxLen = minOf(s1.length, s2.length)
        while (i <= maxLen && s1[s1.length - i] == s2[s2.length - i]) i++
        return i - 1
    }
    
    /**
     * 快速判断是否应该去重
     * 
     * @param newText 新文本
     * @param existingText 已存在的文本
     * @param timeDiffMs 时间差（毫秒）
     * @param similarityThreshold 相似度阈值（默认 0.85）
     * @param timeThresholdMs 时间阈值（默认 60秒）
     */
    fun shouldDeduplicate(
        newText: String,
        existingText: String,
        timeDiffMs: Long,
        similarityThreshold: Float = 0.85f,
        timeThresholdMs: Long = 60_000L
    ): Boolean {
        // 时间差超过阈值，不去重
        if (timeDiffMs > timeThresholdMs) return false
        
        // 计算相似度
        val similarity = calculateSimilarity(newText, existingText)
        
        return similarity >= similarityThreshold
    }
}