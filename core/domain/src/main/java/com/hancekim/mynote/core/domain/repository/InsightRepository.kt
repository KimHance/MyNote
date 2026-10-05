package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.InsightStamp
import com.hancekim.mynote.core.domain.model.InsightStatus
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.WeeklySummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface InsightRepository {
    fun observeLatestSummary(stamp: InsightStamp): Flow<WeeklySummary?>

    fun observeSummaries(stamp: InsightStamp): Flow<List<WeeklySummary>>

    fun observeLatestPattern(stamp: InsightStamp): Flow<QuizPattern?>

    fun observeStatus(): Flow<InsightStatus>

    suspend fun summaries(stamp: InsightStamp): List<WeeklySummary>

    suspend fun latestPattern(stamp: InsightStamp): QuizPattern?

    suspend fun status(): InsightStatus

    suspend fun save(
        summary: WeeklySummary,
        stamp: InsightStamp,
    )

    suspend fun save(
        pattern: QuizPattern,
        stamp: InsightStamp,
    )

    suspend fun markSuccess(at: Long)

    suspend fun markFailure(
        at: Long,
        kind: InsightFailureKind,
        summaryFailed: Boolean,
    )

    suspend fun markSkipped(at: Long)

    suspend fun bumpEpoch(): Long

    suspend fun clearStatus()

    suspend fun prune(before: LocalDate)
}
