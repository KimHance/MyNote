package com.hancekim.mynote.feature.stats

import com.hancekim.mynote.core.domain.stats.Bucket
import com.hancekim.mynote.core.domain.stats.StaleReason
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.core.domain.stats.StatsTotals
import com.hancekim.mynote.core.domain.stats.StudyStats
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.YearMonth

internal fun StudyStats.toUi(): StatsContentUi {
    val period = range.period
    val first = firstRecord
    val today = range.end.toLocalDate()
    return StatsContentUi(
        period = period,
        intro = first == null,
        periodNote = range.shortRangeDays?.let { days -> first?.let { PeriodNoteUi(it, days) } },
        numbers = numbers(),
        comparison =
            when {
                first == null -> ComparisonUi.Pending
                period == StatsPeriod.All -> ComparisonUi.Since(first)
                previous == null -> ComparisonUi.Since(first)
                period == StatsPeriod.Week -> ComparisonUi.Week(today.dayOfWeek)
                else -> ComparisonUi.Month(range.compareEnd?.minusNanos(1)?.dayOfMonth ?: today.dayOfMonth)
            },
        reading =
            ReadingUi(
                bars =
                    timeBars
                        .map {
                            TimeBarUi(
                                it.bucket.label(period),
                                it.minutes,
                                it.bucket.state,
                            )
                        }.toImmutableList(),
                legend =
                    categoryMinutes
                        .map {
                            CategoryMinutesUi(
                                it.category,
                                it.axisIndex,
                                it.minutes,
                            )
                        }.toImmutableList(),
                categoryTotal = categoryMinutes.sumOf { it.minutes },
                stalled =
                    stalled
                        .map { StalledNoteUi(it.note.id, it.note.title, it.note.category, it.percent, it.endedAt) }
                        .toImmutableList(),
            ),
        quiz =
            QuizStatsUi(
                asked = current.asked,
                rows = accuracy.map { AccuracyRowUi(it.category, it.asked, it.accuracy, it.lowest) }.toImmutableList(),
                retries =
                    retries
                        .map {
                            RetryUi(
                                it.note.id,
                                it.note.title,
                                it.note.category,
                                it.firstAccuracy,
                                it.lastAccuracy,
                                it.ordinal,
                            )
                        }.toImmutableList(),
            ),
        bookmarks =
            BookmarkStatsUi(
                buckets =
                    bookmarkBuckets
                        .map { BookmarkBucketUi(it.bucket.label(period), it.added, it.removed, it.bucket.state) }
                        .toImmutableList(),
                added = added,
                removed = removed,
                stale =
                    staleBookmarks
                        .map {
                            StaleBookmarkUi(
                                it.note.id,
                                it.note.title,
                                it.note.category,
                                it.reason.toUi(),
                                it.ageDays,
                            )
                        }.toImmutableList(),
            ),
    )
}

private fun StudyStats.numbers() =
    persistentListOf(
        StatNumberUi(StatKind.Notes, current.notes, delta { it.notes }),
        StatNumberUi(StatKind.Minutes, current.minutes, delta { it.minutes }),
        StatNumberUi(StatKind.Quizzes, current.quizzes, delta { it.quizzes }),
        StatNumberUi(StatKind.Accuracy, current.accuracy, accuracyDelta()),
    )

private fun StudyStats.delta(pick: (StatsTotals) -> Int): DeltaUi {
    val before = previous
    return when {
        firstRecord == null -> DeltaUi.None
        range.period == StatsPeriod.All -> DeltaUi.Blank
        range.period == StatsPeriod.Week && range.firstWeek -> DeltaUi.FirstWeek
        before == null -> DeltaUi.None
        else -> DeltaUi.Value(pick(current) - pick(before))
    }
}

private fun StudyStats.accuracyDelta(): DeltaUi {
    val now = current.accuracy
    val before = previous?.accuracy
    return when {
        firstRecord == null -> DeltaUi.None
        range.period == StatsPeriod.All -> DeltaUi.Blank
        range.period == StatsPeriod.Week && range.firstWeek -> DeltaUi.Questions(current.asked)
        now == null || before == null -> DeltaUi.None
        else -> DeltaUi.Value(now - before)
    }
}

private fun Bucket.label(period: StatsPeriod): BucketLabel =
    when (period) {
        StatsPeriod.Week -> BucketLabel.Day(start.dayOfWeek)
        StatsPeriod.Month -> BucketLabel.Week(start)
        StatsPeriod.All -> BucketLabel.Month(YearMonth.from(start))
    }

private fun StaleReason.toUi(): StaleReasonUi =
    when (this) {
        is StaleReason.LastOpened -> StaleReasonUi.LastOpened(days)
        StaleReason.Unread -> StaleReasonUi.Unread
        StaleReason.UnreadSinceLog -> StaleReasonUi.UnreadSinceLog
    }
