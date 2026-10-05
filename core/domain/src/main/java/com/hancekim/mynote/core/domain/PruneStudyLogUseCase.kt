package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.domain.stats.StatsThresholds
import com.hancekim.mynote.core.domain.stats.WeeklyHistory
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class PruneStudyLogUseCase(
    private val studyLog: StudyLogRepository,
    private val insights: InsightRepository,
    private val now: () -> Long,
    private val zone: () -> ZoneId,
) {
    @Inject
    constructor(studyLog: StudyLogRepository, insights: InsightRepository) :
        this(studyLog, insights, System::currentTimeMillis, ZoneId::systemDefault)

    suspend operator fun invoke() {
        val at = now()
        studyLog.prune(before = at - RETENTION_MILLIS)
        studyLog.pruneAnswers(before = at - ANSWER_RETENTION_MILLIS)
        insights.prune(before = insightCutoff(at, zone()))
    }

    companion object {
        const val RETENTION_DAYS = 365L
        const val RETENTION_MILLIS = RETENTION_DAYS * 86_400_000L
        const val ANSWER_RETENTION_DAYS = 90L
        const val ANSWER_RETENTION_MILLIS = ANSWER_RETENTION_DAYS * 86_400_000L

        fun insightCutoff(
            at: Long,
            zone: ZoneId,
        ) = WeeklyHistory
            .mondayOf(Instant.ofEpochMilli(at).atZone(zone).toLocalDate())
            .minusWeeks(StatsThresholds.INSIGHT_KEEP_WEEKS - 1)
    }
}
