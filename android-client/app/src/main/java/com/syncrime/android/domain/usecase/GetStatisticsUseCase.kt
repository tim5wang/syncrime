package com.syncrime.android.domain.usecase

import com.syncrime.android.data.local.dao.InputRecordDao
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
        val todayInputs: Int,
        val totalRecords: Int,
        val todayRecords: Int,
        val todaySyncCount: Int,
        val appStats: List<AppStat>,
        val wordFrequency: List<WordStat>
    )
    
    data class AppStat(
        val application: String,
        val count: Int
    )
    
    data class WordStat(
        val content: String,
        val count: Int
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
        ) { args ->
            val totalSessions = args[0] as Int
            val todaySessions = args[1] as Int
            val totalRecords = args[2] as Int
            val todayRecords = args[3] as Int
            val todayInputs = (args[4] as Int?) ?: 0
            val todaySyncCount = args[5] as Int
            val appStatsRaw = args[6] as List<InputRecordDao.AppStat>
            val wordFrequencyRaw = args[7] as List<InputRecordDao.WordStat>
            
            StatisticsData(
                totalSessions = totalSessions,
                todaySessions = todaySessions,
                totalInputs = totalRecords,
                todayInputs = todayInputs,
                totalRecords = totalRecords,
                todayRecords = todayRecords,
                todaySyncCount = todaySyncCount,
                appStats = appStatsRaw.map { AppStat(it.application, it.count) },
                wordFrequency = wordFrequencyRaw.map { WordStat(it.content, it.count) }
            )
        }
    }
}
