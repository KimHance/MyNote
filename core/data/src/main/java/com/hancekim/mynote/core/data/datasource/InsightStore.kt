package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.InsightStamp
import com.hancekim.mynote.core.domain.model.InsightStatus
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.WeeklySummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface InsightStore {
    fun observeLatestSummary(stamp: InsightStamp): Flow<WeeklySummary?>

    fun observeSummaries(stamp: InsightStamp): Flow<List<WeeklySummary>>

    fun observeLatestPattern(stamp: InsightStamp): Flow<QuizPattern?>

    suspend fun summaries(stamp: InsightStamp): List<WeeklySummary>

    suspend fun latestPattern(stamp: InsightStamp): QuizPattern?

    suspend fun upsert(
        summary: WeeklySummary,
        stamp: InsightStamp,
    )

    suspend fun upsert(
        pattern: QuizPattern,
        stamp: InsightStamp,
    )

    suspend fun prune(before: LocalDate)
}

interface InsightStatusStore {
    fun observe(): Flow<InsightStatus>

    suspend fun markSuccess(at: Long)

    suspend fun markFailure(
        at: Long,
        kind: InsightFailureKind,
        summaryFailed: Boolean,
    )

    suspend fun markSkipped(at: Long)

    suspend fun bumpEpoch(): Long

    suspend fun clear()
}
