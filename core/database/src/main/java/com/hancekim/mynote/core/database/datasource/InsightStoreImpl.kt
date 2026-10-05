package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.data.datasource.InsightStore
import com.hancekim.mynote.core.database.dao.InsightDao
import com.hancekim.mynote.core.database.entity.QuizPatternEntity
import com.hancekim.mynote.core.database.entity.WeeklySummaryEntity
import com.hancekim.mynote.core.domain.model.InsightStamp
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.WeeklySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class InsightStoreImpl @Inject internal constructor(
    private val dao: InsightDao,
) : InsightStore {
    override fun observeLatestSummary(stamp: InsightStamp): Flow<WeeklySummary?> =
        dao.observeLatestSummary(stamp.modelCommit, stamp.schemaVersion).map { it?.toDomain() }

    override fun observeSummaries(stamp: InsightStamp): Flow<List<WeeklySummary>> =
        dao.observeSummaries(stamp.modelCommit, stamp.schemaVersion).map { rows -> rows.map { it.toDomain() } }

    override fun observeLatestPattern(stamp: InsightStamp): Flow<QuizPattern?> =
        dao.observeLatestPattern(stamp.modelCommit, stamp.schemaVersion).map { it?.toDomain() }

    override suspend fun summaries(stamp: InsightStamp): List<WeeklySummary> =
        dao.summaries(stamp.modelCommit, stamp.schemaVersion).map { it.toDomain() }

    override suspend fun latestPattern(stamp: InsightStamp): QuizPattern? =
        dao.latestPattern(stamp.modelCommit, stamp.schemaVersion)?.toDomain()

    override suspend fun upsert(
        summary: WeeklySummary,
        stamp: InsightStamp,
    ) = dao.upsertSummary(
        WeeklySummaryEntity(
            weekStart = summary.weekStart.toEpochDay(),
            generatedAt = summary.generatedAt,
            headline = summary.headline,
            detail = summary.detail,
            focus = summary.focus,
            modelCommit = stamp.modelCommit,
            schemaVersion = stamp.schemaVersion,
        ),
    )

    override suspend fun upsert(
        pattern: QuizPattern,
        stamp: InsightStamp,
    ) = dao.upsertPattern(
        QuizPatternEntity(
            weekStart = pattern.weekStart.toEpochDay(),
            generatedAt = pattern.generatedAt,
            tag0 = pattern.tags.first(),
            tag1 = pattern.tags.getOrNull(1),
            tag2 = pattern.tags.getOrNull(2),
            sentence = pattern.sentence,
            modelCommit = stamp.modelCommit,
            schemaVersion = stamp.schemaVersion,
        ),
    )

    override suspend fun prune(before: LocalDate) = dao.prune(before.toEpochDay())
}

private fun WeeklySummaryEntity.toDomain() =
    WeeklySummary(
        weekStart = LocalDate.ofEpochDay(weekStart),
        generatedAt = generatedAt,
        headline = headline,
        detail = detail,
        focus = focus,
    )

private fun QuizPatternEntity.toDomain() =
    QuizPattern(
        weekStart = LocalDate.ofEpochDay(weekStart),
        generatedAt = generatedAt,
        tags = listOfNotNull(tag0, tag1, tag2),
        sentence = sentence,
    )
