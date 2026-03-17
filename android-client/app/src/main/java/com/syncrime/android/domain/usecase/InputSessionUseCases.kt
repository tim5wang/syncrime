package com.syncrime.android.domain.usecase

import com.syncrime.android.data.local.entity.InputSessionEntity
import com.syncrime.android.data.repository.InputRepository

/**
 * 开始输入会话用例
 */
class StartInputSessionUseCase(
    private val inputRepository: InputRepository
) {
    suspend operator fun invoke(
        application: String,
        packageName: String,
        metadata: String? = null
    ): InputSessionEntity {
        return inputRepository.createSession(application, packageName, metadata)
    }
}

/**
 * 结束输入会话用例
 */
class EndInputSessionUseCase(
    private val inputRepository: InputRepository
) {
    suspend operator fun invoke(sessionId: String, endTime: Long = System.currentTimeMillis()) {
        inputRepository.endSession(sessionId, endTime)
    }
}

/**
 * 记录输入内容用例
 */
class RecordInputUseCase(
    private val inputRepository: InputRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        content: String,
        application: String,
        context: String? = null,
        isSensitive: Boolean = false,
        category: String? = null,
        confidence: Float = 1.0f,
        isRecommended: Boolean = false,
        characterCount: Int = 0
    ) {
        inputRepository.createRecord(
            sessionId = sessionId,
            content = content,
            application = application,
            context = context,
            isSensitive = isSensitive,
            category = category,
            confidence = confidence,
            isRecommended = isRecommended
        )
        inputRepository.incrementSessionInputCount(sessionId, characterCount)
    }
}
