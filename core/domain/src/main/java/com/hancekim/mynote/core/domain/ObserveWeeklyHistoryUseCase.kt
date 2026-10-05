package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.InsightScheduler
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.domain.stats.HistoryWeek
import com.hancekim.mynote.core.domain.stats.WeeklyHistory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.ZonedDateTime
import javax.inject.Inject

class ObserveWeeklyHistoryUseCase @Inject constructor(
    private val studyLog: StudyLogRepository,
    private val insights: InsightRepository,
    private val scheduler: InsightScheduler,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(now: ZonedDateTime): Flow<List<HistoryWeek>> =
        studyLog
            .observeFirstRecordAt()
            .distinctUntilChanged()
            .flatMapLatest { firstRecordAt ->
                if (firstRecordAt == null) {
                    flowOf(emptyList())
                } else {
                    val thisWeek = WeeklyHistory.mondayOf(now.toLocalDate())
                    val firstWeek = WeeklyHistory.firstWeek(thisWeek, firstRecordAt, now.zone)
                    val from = firstWeek.atStartOfDay(now.zone).toInstant().toEpochMilli()
                    combine(
                        studyLog.observeWindow(from, Long.MAX_VALUE),
                        insights.observeSummaries(scheduler.stamp),
                    ) { window, summaries ->
                        WeeklyHistory.of(firstWeek, thisWeek, now.zone, window, summaries)
                    }
                }
            }
}
