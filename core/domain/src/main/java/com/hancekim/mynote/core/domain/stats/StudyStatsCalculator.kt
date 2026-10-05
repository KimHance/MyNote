package com.hancekim.mynote.core.domain.stats

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.BookmarkEventKind
import com.hancekim.mynote.core.domain.model.NoteFacts
import com.hancekim.mynote.core.domain.model.QuizAttemptSummary
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class NoteRef(
    val id: String,
    val title: String,
    val category: String,
)

data class StatsTotals(
    val notes: Int,
    val minutes: Int,
    val quizzes: Int,
    val asked: Int,
    val correct: Int,
) {
    val accuracy: Int? get() = percentOf(correct, asked)
}

data class BucketMinutes(
    val bucket: Bucket,
    val minutes: Int,
)

data class CategoryMinutes(
    val category: String,
    val axisIndex: Int,
    val minutes: Int,
)

data class StalledNote(
    val note: NoteRef,
    val percent: Int,
    val endedAt: Long,
)

data class CategoryAccuracy(
    val category: String,
    val axisIndex: Int,
    val asked: Int,
    val correct: Int,
    val lowest: Boolean,
) {
    val accuracy: Int? get() = percentOf(correct, asked)
}

data class RetryTrend(
    val note: NoteRef,
    val firstAccuracy: Int,
    val lastAccuracy: Int,
    val ordinal: Int,
    val lastAt: Long,
) {
    val improved: Boolean get() = lastAccuracy > firstAccuracy
}

data class BookmarkBucket(
    val bucket: Bucket,
    val added: Int,
    val removed: Int,
)

sealed interface StaleReason {
    data class LastOpened(
        val days: Int,
    ) : StaleReason

    data object Unread : StaleReason

    data object UnreadSinceLog : StaleReason
}

data class StaleBookmark(
    val note: NoteRef,
    val reason: StaleReason,
    val ageDays: Int,
)

data class StudyStats(
    val range: StatsRange,
    val firstRecord: LocalDate?,
    val current: StatsTotals,
    val previous: StatsTotals?,
    val axis: List<String>,
    val timeBars: List<BucketMinutes>,
    val categoryMinutes: List<CategoryMinutes>,
    val stalled: List<StalledNote>,
    val accuracy: List<CategoryAccuracy>,
    val retries: List<RetryTrend>,
    val bookmarkBuckets: List<BookmarkBucket>,
    val added: Int,
    val removed: Int,
    val staleBookmarks: List<StaleBookmark>,
)

object StudyStatsCalculator {
    fun compute(
        range: StatsRange,
        now: ZonedDateTime,
        firstRecordAt: Long?,
        window: StudyWindow,
        facts: NoteFacts,
        notes: List<NoteRef>,
        bookmarks: List<Bookmark>,
    ): StudyStats {
        val zone = now.zone
        val start = range.start.toEpochMilli()
        val compare =
            range.compareStart?.let { from ->
                range.compareEnd?.let { end -> from.toEpochMilli() until end.toEpochMilli() }
            }
        val sessions = window.sessions.filter { it.startedAt >= start }
        val attempts = window.attempts.filter { it.finishedAt >= start }
        val events = window.bookmarkEvents.filter { it.at >= start }
        val noteById = notes.associateBy { it.id }

        val axis =
            (
                notes.map { it.category } +
                    sessions.map { it.category } +
                    attempts.map { it.category } +
                    events.map { it.category }
            ).filter { it.isNotEmpty() }
                .distinct()
                .sorted()

        val previous =
            compare?.let { span ->
                totals(
                    window.sessions.filter { it.startedAt in span },
                    window.attempts.filter { it.finishedAt in span },
                )
            }

        return StudyStats(
            range = range,
            firstRecord = firstRecordAt?.toLocalDate(zone),
            current = totals(sessions, attempts),
            previous = previous,
            axis = axis,
            timeBars =
                range.buckets.map { bucket ->
                    BucketMinutes(bucket, minutesOf(sessions.filter { it.startedAt.toLocalDate(zone) in bucket }))
                },
            categoryMinutes = categoryMinutes(axis, sessions),
            stalled = stalled(facts.latestSessions, start, noteById),
            accuracy = accuracy(axis, attempts),
            retries = retries(facts.attempts, start, noteById),
            bookmarkBuckets =
                range.buckets.map { bucket ->
                    val inBucket = events.filter { it.at.toLocalDate(zone) in bucket }
                    BookmarkBucket(
                        bucket = bucket,
                        added = inBucket.count { it.kind == BookmarkEventKind.Added },
                        removed = inBucket.count { it.kind == BookmarkEventKind.Removed },
                    )
                },
            added = events.count { it.kind == BookmarkEventKind.Added },
            removed = events.count { it.kind == BookmarkEventKind.Removed },
            staleBookmarks = staleBookmarks(bookmarks, facts.latestSessions, firstRecordAt, noteById, now),
        )
    }

    private fun totals(
        sessions: List<ReadSession>,
        attempts: List<QuizAttemptSummary>,
    ): StatsTotals =
        StatsTotals(
            notes = sessions.map { it.noteId }.distinct().size,
            minutes = minutesOf(sessions),
            quizzes = attempts.size,
            asked = attempts.sumOf { it.asked },
            correct = attempts.sumOf { it.correct },
        )

    private fun categoryMinutes(
        axis: List<String>,
        sessions: List<ReadSession>,
    ): List<CategoryMinutes> {
        val byCategory = sessions.groupBy { it.category }
        return axis
            .mapIndexed { index, category ->
                CategoryMinutes(category, index, minutesOf(byCategory[category].orEmpty()))
            }.sortedWith(compareByDescending<CategoryMinutes> { it.minutes }.thenBy { it.axisIndex })
    }

    private fun stalled(
        latest: List<ReadSession>,
        start: Long,
        noteById: Map<String, NoteRef>,
    ): List<StalledNote> =
        latest
            .groupBy { it.noteId }
            .mapNotNull { (_, rows) -> rows.maxByOrNull { it.endedAt } }
            .filter { it.startedAt >= start && it.maxPercent < StatsThresholds.STALLED_BELOW_PERCENT }
            .sortedByDescending { it.endedAt }
            .mapNotNull { session ->
                noteById[session.noteId]?.let { StalledNote(it, session.maxPercent, session.endedAt) }
            }.take(StatsThresholds.LIST_LIMIT)

    private fun accuracy(
        axis: List<String>,
        attempts: List<QuizAttemptSummary>,
    ): List<CategoryAccuracy> {
        val byCategory = attempts.groupBy { it.category }
        val rows =
            axis.mapIndexed { index, category ->
                val taken = byCategory[category].orEmpty()
                CategoryAccuracy(
                    category = category,
                    axisIndex = index,
                    asked = taken.sumOf { it.asked },
                    correct = taken.sumOf { it.correct },
                    lowest = false,
                )
            }
        val taken = rows.mapNotNull { it.accuracy }
        val lowest = taken.minOrNull()?.takeIf { taken.size >= StatsThresholds.MIN_CATEGORIES_FOR_LOWEST }
        return rows
            .map { if (lowest != null && it.accuracy == lowest) it.copy(lowest = true) else it }
            .sortedWith(
                compareBy<CategoryAccuracy> { it.accuracy == null }
                    .thenBy { it.accuracy ?: 0 }
                    .thenBy { it.axisIndex },
            )
    }

    private fun retries(
        attempts: List<QuizAttemptSummary>,
        start: Long,
        noteById: Map<String, NoteRef>,
    ): List<RetryTrend> =
        attempts
            .groupBy { it.noteId }
            .mapNotNull { (noteId, rows) ->
                val ordered = rows.sortedBy { it.finishedAt }
                val lastIndex = ordered.indexOfLast { it.finishedAt >= start }
                val note = noteById[noteId]
                if (ordered.size < 2 || lastIndex < 0 || note == null) return@mapNotNull null
                val last = ordered[lastIndex]
                RetryTrend(
                    note = note,
                    firstAccuracy = percentOf(ordered.first().correct, ordered.first().asked) ?: 0,
                    lastAccuracy = percentOf(last.correct, last.asked) ?: 0,
                    ordinal = lastIndex + 1,
                    lastAt = last.finishedAt,
                )
            }.sortedByDescending { it.lastAt }
            .take(StatsThresholds.LIST_LIMIT)

    private fun staleBookmarks(
        bookmarks: List<Bookmark>,
        latest: List<ReadSession>,
        firstRecordAt: Long?,
        noteById: Map<String, NoteRef>,
        now: ZonedDateTime,
    ): List<StaleBookmark> {
        if (firstRecordAt == null) return emptyList()
        val zone = now.zone
        val today = now.toLocalDate()
        val lastSession = latest.groupBy { it.noteId }.mapValues { (_, rows) -> rows.maxOf { it.endedAt } }
        return bookmarks
            .sortedBy { it.addedAt }
            .mapNotNull { bookmark ->
                val note = noteById[bookmark.noteId] ?: return@mapNotNull null
                val opened = lastSession[bookmark.noteId]
                val base = opened ?: maxOf(bookmark.addedAt, firstRecordAt)
                if (daysBetween(base, today, zone) <= StatsThresholds.STALE_BOOKMARK_DAYS) return@mapNotNull null
                val reason =
                    when {
                        opened != null -> StaleReason.LastOpened(daysBetween(opened, today, zone))
                        bookmark.addedAt >= firstRecordAt -> StaleReason.Unread
                        else -> StaleReason.UnreadSinceLog
                    }
                StaleBookmark(note, reason, daysBetween(bookmark.addedAt, today, zone))
            }.take(StatsThresholds.LIST_LIMIT)
    }

    private fun minutesOf(sessions: List<ReadSession>): Int =
        (
            sessions.sumOf {
                it.activeMillis
            } / MILLIS_PER_MINUTE
        ).toInt()

    private fun daysBetween(
        at: Long,
        today: LocalDate,
        zone: ZoneId,
    ): Int = ChronoUnit.DAYS.between(at.toLocalDate(zone), today).toInt()

    private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    private fun ZonedDateTime.toEpochMilli(): Long = toInstant().toEpochMilli()

    private const val MILLIS_PER_MINUTE = 60_000L
}

internal fun percentOf(
    part: Int,
    whole: Int,
): Int? = if (whole <= 0) null else ((part * 200L + whole) / (2L * whole)).toInt()
