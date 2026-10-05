package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.InsightSnapshot
import com.hancekim.mynote.core.domain.model.InsightWorkState
import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.InsightScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class ObserveInsightsUseCase @Inject constructor(
    private val insights: InsightRepository,
    private val scheduler: InsightScheduler,
) {
    operator fun invoke(): Flow<InsightSnapshot> =
        combine(
            insights.observeLatestSummary(scheduler.stamp),
            insights.observeLatestPattern(scheduler.stamp),
            insights.observeStatus(),
            scheduler.state(),
        ) { latest, pattern, status, state ->
            InsightSnapshot(
                latest = latest,
                pattern = pattern,
                lastFailureAt = status.lastFailureAt,
                generating = state == InsightWorkState.Generating,
            )
        }.distinctUntilChanged()
}
