package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.InsightStatusStore
import com.hancekim.mynote.core.data.datasource.InsightStore
import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.InsightStamp
import com.hancekim.mynote.core.domain.model.InsightStatus
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.WeeklySummary
import com.hancekim.mynote.core.domain.repository.InsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class InsightRepositoryImpl @Inject constructor(
    private val store: InsightStore,
    private val statusStore: InsightStatusStore,
) : InsightRepository {
    override fun observeLatestSummary(stamp: InsightStamp): Flow<WeeklySummary?> = store.observeLatestSummary(stamp)

    override fun observeSummaries(stamp: InsightStamp): Flow<List<WeeklySummary>> = store.observeSummaries(stamp)

    override fun observeLatestPattern(stamp: InsightStamp): Flow<QuizPattern?> = store.observeLatestPattern(stamp)

    override fun observeStatus(): Flow<InsightStatus> = statusStore.observe()

    override suspend fun summaries(stamp: InsightStamp): List<WeeklySummary> = store.summaries(stamp)

    override suspend fun latestPattern(stamp: InsightStamp): QuizPattern? = store.latestPattern(stamp)

    override suspend fun status(): InsightStatus = statusStore.observe().first()

    override suspend fun save(
        summary: WeeklySummary,
        stamp: InsightStamp,
    ) = store.upsert(summary, stamp)

    override suspend fun save(
        pattern: QuizPattern,
        stamp: InsightStamp,
    ) = store.upsert(pattern, stamp)

    override suspend fun markSuccess(at: Long) = statusStore.markSuccess(at)

    override suspend fun markFailure(
        at: Long,
        kind: InsightFailureKind,
        summaryFailed: Boolean,
    ) = statusStore.markFailure(at, kind, summaryFailed)

    override suspend fun markSkipped(at: Long) = statusStore.markSkipped(at)

    override suspend fun bumpEpoch(): Long = statusStore.bumpEpoch()

    override suspend fun clearStatus() = statusStore.clear()

    override suspend fun prune(before: LocalDate) = store.prune(before)
}
