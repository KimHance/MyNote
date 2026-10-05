package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.InsightStamp
import com.hancekim.mynote.core.domain.model.InsightStatus
import com.hancekim.mynote.core.domain.model.InsightWorkState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.WeeklySummary
import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.InsightScheduler
import com.hancekim.mynote.core.domain.repository.QuizModelSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

internal class FakeInsights(
    private val calls: MutableList<String> = mutableListOf(),
) : InsightRepository {
    var prunedBefore: LocalDate? = null

    override fun observeLatestSummary(stamp: InsightStamp): Flow<WeeklySummary?> = flowOf(null)

    override fun observeSummaries(stamp: InsightStamp): Flow<List<WeeklySummary>> = flowOf(emptyList())

    override fun observeLatestPattern(stamp: InsightStamp): Flow<QuizPattern?> = flowOf(null)

    override fun observeStatus(): Flow<InsightStatus> = flowOf(InsightStatus())

    override suspend fun summaries(stamp: InsightStamp): List<WeeklySummary> = emptyList()

    override suspend fun latestPattern(stamp: InsightStamp): QuizPattern? = null

    override suspend fun status(): InsightStatus = InsightStatus()

    override suspend fun save(
        summary: WeeklySummary,
        stamp: InsightStamp,
    ) = Unit

    override suspend fun save(
        pattern: QuizPattern,
        stamp: InsightStamp,
    ) = Unit

    override suspend fun markSuccess(at: Long) = Unit

    override suspend fun markFailure(
        at: Long,
        kind: InsightFailureKind,
        summaryFailed: Boolean,
    ) = Unit

    override suspend fun markSkipped(at: Long) = Unit

    override suspend fun bumpEpoch(): Long {
        calls += "epoch"
        return 1L
    }

    override suspend fun clearStatus() {
        calls += "clearStatus"
    }

    override suspend fun prune(before: LocalDate) {
        prunedBefore = before
    }
}

internal class FakeScheduler(
    private val calls: MutableList<String> = mutableListOf(),
) : InsightScheduler {
    override val stamp = InsightStamp("commit", 1)

    override suspend fun schedule() {
        calls += "schedule"
    }

    override fun cancel() {
        calls += "cancel"
    }

    override fun state(): Flow<InsightWorkState> = flowOf(InsightWorkState.Idle)
}

internal class FakeModelSource(
    status: ModelStatus,
) : QuizModelSource {
    private val current = MutableStateFlow(status)

    override fun status(): Flow<ModelStatus> = current

    override fun start() = Unit

    override fun cancel() = Unit

    override fun discardPartial() = Unit

    override suspend fun deleteModel() = Unit
}
