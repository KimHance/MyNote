package com.hancekim.mynote.core.domain.stats

import com.hancekim.mynote.core.domain.model.QuizAttemptSummary
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class DigestTotals(
    val notes: Int,
    val minutes: Int,
    val quizzes: Int,
    val accuracy: Int?,
)

data class DigestCategory(
    val id: String,
    val minutes: Int,
    val correct: Int,
    val asked: Int,
)

enum class DigestTrend {
    More,
    Less,
    Same,
}

data class WeeklyDigest(
    val weekStart: LocalDate,
    val days: Int,
    val daysLeft: Int,
    val current: DigestTotals,
    val previous: DigestTotals?,
    val change: DigestTotals?,
    val categories: List<DigestCategory>,
    val stalled: List<String>,
    val reread: String?,
) {
    val categoryIds: List<String> get() = categories.map { it.id }

    val top: String? get() = categories.filter { it.minutes > 0 }.maxByOrNull { it.minutes }?.id

    val least: String?
        get() =
            categories
                .takeIf { it.size >= 2 }
                ?.minWithOrNull(compareBy<DigestCategory> { it.minutes }.thenBy { it.asked })
                ?.takeIf { it.id != top }
                ?.id

    val trend: DigestTrend?
        get() =
            change?.minutes?.let {
                when {
                    it > 0 -> DigestTrend.More
                    it < 0 -> DigestTrend.Less
                    else -> DigestTrend.Same
                }
            }

    val titles: List<String> get() = (stalled + listOfNotNull(reread)).distinct()
}

object WeeklyDigestBuilder {
    fun of(
        weekStart: LocalDate,
        now: ZonedDateTime,
        window: StudyWindow,
        notes: List<NoteRef>,
        firstRecordAt: Long?,
    ): WeeklyDigest {
        val zone = now.zone
        val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekEnd =
            weekStart
                .plusWeeks(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        val end = minOf(weekEnd, now.toInstant().toEpochMilli() + 1)
        val prevStart =
            weekStart
                .minusWeeks(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        val prevEnd = prevStart + (end - start)
        val span = start until end
        val prevSpan = prevStart until prevEnd

        val sessions = window.sessions.filter { it.startedAt in span }
        val attempts = window.attempts.filter { it.finishedAt in span }
        val current = totals(sessions, attempts)
        val previous =
            if (firstRecordAt != null && firstRecordAt < prevStart + DAY_MILLIS) {
                totals(
                    window.sessions.filter { it.startedAt in prevSpan },
                    window.attempts.filter {
                        it.finishedAt in
                            prevSpan
                    },
                )
            } else {
                null
            }
        val noteById = notes.associateBy { it.id }
        val today = now.toLocalDate()
        val lastDay = weekStart.plusDays(DAYS_PER_WEEK - 1)

        return WeeklyDigest(
            weekStart = weekStart,
            days =
                (sessions.map { it.startedAt } + attempts.map { it.finishedAt })
                    .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                    .distinct()
                    .size,
            daysLeft = if (today.isAfter(lastDay)) 0 else ChronoUnit.DAYS.between(today, lastDay).toInt(),
            current = current,
            previous = previous,
            change = previous?.let { current.minus(it) },
            categories = categories(notes, sessions, attempts),
            stalled = stalled(sessions, noteById),
            reread = reread(sessions, noteById),
        )
    }

    private fun totals(
        sessions: List<ReadSession>,
        attempts: List<QuizAttemptSummary>,
    ): DigestTotals =
        DigestTotals(
            notes = sessions.map { it.noteId }.distinct().size,
            minutes = minutesOf(sessions),
            quizzes = attempts.size,
            accuracy = percentOf(attempts.sumOf { it.correct }, attempts.sumOf { it.asked }),
        )

    private fun DigestTotals.minus(other: DigestTotals): DigestTotals =
        DigestTotals(
            notes = notes - other.notes,
            minutes = minutes - other.minutes,
            quizzes = quizzes - other.quizzes,
            accuracy = accuracy?.let { now -> other.accuracy?.let { now - it } },
        )

    private fun categories(
        notes: List<NoteRef>,
        sessions: List<ReadSession>,
        attempts: List<QuizAttemptSummary>,
    ): List<DigestCategory> {
        val minutes = sessions.groupBy { it.category }
        val quizzes = attempts.groupBy { it.category }
        return (notes.map { it.category } + sessions.map { it.category } + attempts.map { it.category })
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .map { id ->
                val taken = quizzes[id].orEmpty()
                DigestCategory(
                    id = id,
                    minutes = minutesOf(minutes[id].orEmpty()),
                    correct = taken.sumOf { it.correct },
                    asked = taken.sumOf { it.asked },
                )
            }
    }

    private fun stalled(
        sessions: List<ReadSession>,
        noteById: Map<String, NoteRef>,
    ): List<String> =
        sessions
            .groupBy { it.noteId }
            .mapNotNull { (_, rows) -> rows.maxByOrNull { it.endedAt } }
            .filter { it.maxPercent < StatsThresholds.STALLED_BELOW_PERCENT }
            .sortedByDescending { it.endedAt }
            .mapNotNull { noteById[it.noteId]?.title }
            .take(StatsThresholds.DIGEST_STALLED_LIMIT)

    private fun reread(
        sessions: List<ReadSession>,
        noteById: Map<String, NoteRef>,
    ): String? =
        sessions
            .groupBy { it.noteId }
            .filter { (id, rows) -> rows.size >= 2 && id in noteById }
            .maxWithOrNull(
                compareBy<Map.Entry<String, List<ReadSession>>> { it.value.size }.thenBy { minutesOf(it.value) },
            )?.let { noteById[it.key]?.title }

    private fun minutesOf(sessions: List<ReadSession>): Int =
        (sessions.sumOf { it.activeMillis } / MINUTE_MILLIS)
            .toInt()

    private const val MINUTE_MILLIS = 60_000L
    private const val DAY_MILLIS = 86_400_000L
    private const val DAYS_PER_WEEK = 7L
}
