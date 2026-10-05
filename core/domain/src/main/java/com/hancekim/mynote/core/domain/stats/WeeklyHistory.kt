package com.hancekim.mynote.core.domain.stats

import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.WeeklySummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HistoryWeek(
    val weekStart: LocalDate,
    val current: Boolean,
    val summary: WeeklySummary?,
    val notes: Int,
    val minutes: Int,
    val quizzes: Int,
    val accuracy: Int?,
)

object WeeklyHistory {
    fun firstWeek(
        thisWeek: LocalDate,
        firstRecordAt: Long,
        zone: ZoneId,
    ): LocalDate {
        val first = mondayOf(Instant.ofEpochMilli(firstRecordAt).atZone(zone).toLocalDate())
        val floor = thisWeek.minusWeeks(StatsThresholds.HISTORY_WEEKS - 1)
        return if (first.isBefore(floor)) floor else first
    }

    fun of(
        firstWeek: LocalDate,
        thisWeek: LocalDate,
        zone: ZoneId,
        window: StudyWindow,
        summaries: List<WeeklySummary>,
    ): List<HistoryWeek> {
        val sessions = window.sessions.groupBy { mondayOf(it.startedAt, zone) }
        val attempts = window.attempts.groupBy { mondayOf(it.finishedAt, zone) }
        val byWeek = summaries.associateBy { it.weekStart }
        return generateSequence(thisWeek) { it.minusWeeks(1) }
            .takeWhile { !it.isBefore(firstWeek) }
            .map { week ->
                val weekSessions = sessions[week].orEmpty()
                val weekAttempts = attempts[week].orEmpty()
                HistoryWeek(
                    weekStart = week,
                    current = week == thisWeek,
                    summary = byWeek[week],
                    notes = weekSessions.map { it.noteId }.distinct().size,
                    minutes = (weekSessions.sumOf { it.activeMillis } / MINUTE_MILLIS).toInt(),
                    quizzes = weekAttempts.size,
                    accuracy = percentOf(weekAttempts.sumOf { it.correct }, weekAttempts.sumOf { it.asked }),
                )
            }.toList()
    }

    fun mondayOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun mondayOf(
        at: Long,
        zone: ZoneId,
    ): LocalDate = mondayOf(Instant.ofEpochMilli(at).atZone(zone).toLocalDate())

    private const val MINUTE_MILLIS = 60_000L
}
