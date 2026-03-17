package com.syncrime.android.domain.usecase

import com.syncrime.android.data.repository.InputRepository
import com.syncrime.android.data.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 获取统计数据用例
 */
class GetStatisticsUseCase(
    private val inputRepository: InputRepository,
    private val syncRepository: SyncRepository
) {
    
    data class StatisticsData(
        val totalSessions: Int,
        val todaySessions: Int,
        val totalInputs: Int,
        val todayInputs: Int?,
        val totalRecords: Int,
        val todayRecords: Int,
        val todaySyncCount: Int,
        val appStats: List<InputStats.AppStat>,
        val wordFrequency: List<InputStats.WordStat>
    )
    
    operator fun invoke(): Flow<StatisticsData> {
        return combine(
            inputRepository.getTotalSessionsCount(),
            inputRepository.getTodaySessionsCount(),
            inputRepository.getTotalRecordsCount(),
            inputRepository.getTodayRecordsCount(),
            inputRepository.getTodayTotalInputs(),
            syncRepository.getTodaySyncCount(),
            inputRepository.getTodayAppStats(),
            inputRepository.getTodayWordFrequency()
        ) { totalSessions, todaySessions, totalRecords, todayRecords, todayInputs, todaySyncCount, appStats, wordFrequency ->
            StatisticsData(
                totalSessions = totalSessions,
                todaySessions = todaySessions,
                totalInputs = totalRecords,
                todayInputs = todayInputs ?: 0,
                totalRecords = totalRecords,
                todayRecords = todayRecords,
                todaySyncCount = todaySyncCount,
                appStats = appStats,
                wordFrequency = wordFrequency
            )
        }
    }
}
