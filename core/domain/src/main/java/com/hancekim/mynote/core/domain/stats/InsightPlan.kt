package com.hancekim.mynote.core.domain.stats

import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.WeeklySummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

data class InsightFacts(
    val now: ZonedDateTime,
    val window: StudyWindow,
    val summaries: List<WeeklySummary>,
    val pattern: QuizPattern?,
    val wrongRecent: Int,
    val wrongSincePattern: Int,
)

enum class SummaryKind {
    LastWeek,
    ThisWeekFirst,
    ThisWeekRefresh,
}

sealed interface InsightTarget {
    data class Summary(
        val weekStart: LocalDate,
        val kind: SummaryKind,
    ) : InsightTarget

    data class Pattern(
        val weekStart: LocalDate,
    ) : InsightTarget
}

object InsightPlan {
    fun of(facts: InsightFacts): List<InsightTarget> {
        val zone = facts.now.zone
        val thisWeek = facts.now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastWeek = thisWeek.minusWeeks(1)
        val thisStart = thisWeek.atStartOfDay(zone).toInstant().toEpochMilli()
        val lastStart = lastWeek.atStartOfDay(zone).toInstant().toEpochMilli()
        val nowMillis = facts.now.toInstant().toEpochMilli()
        val lastSpan = lastStart until thisStart
        val thisSpan = thisStart..nowMillis

        val targets = mutableListOf<InsightTarget>()
        lastWeekTarget(facts, lastWeek, lastSpan, thisStart)?.let(targets::add)
        thisWeekTarget(facts, thisWeek, thisSpan, nowMillis)?.let(targets::add)
        if (patternDue(facts, nowMillis)) targets += InsightTarget.Pattern(thisWeek)
        return targets
    }

    private fun lastWeekTarget(
        facts: InsightFacts,
        week: LocalDate,
        span: LongRange,
        weekEnd: Long,
    ): InsightTarget? {
        if (!facts.window.enoughIn(span)) return null
        val existing = facts.summaries.firstOrNull { it.weekStart == week }
        val due =
            existing == null ||
                (existing.generatedAt < weekEnd && facts.window.eventsIn(span, after = existing.generatedAt) >= 1)
        return if (due) InsightTarget.Summary(week, SummaryKind.LastWeek) else null
    }

    private fun thisWeekTarget(
        facts: InsightFacts,
        week: LocalDate,
        span: LongRange,
        now: Long,
    ): InsightTarget? {
        val existing = facts.summaries.firstOrNull { it.weekStart == week }
        return when {
            existing == null && facts.window.enoughIn(span) -> {
                InsightTarget.Summary(week, SummaryKind.ThisWeekFirst)
            }

            existing != null &&
                facts.window.eventsIn(span, after = existing.generatedAt) >= StatsThresholds.INSIGHT_REFRESH_EVENTS &&
                now - existing.generatedAt >= REFRESH_MILLIS -> {
                InsightTarget.Summary(week, SummaryKind.ThisWeekRefresh)
            }

            else -> {
                null
            }
        }
    }

    private fun patternDue(
        facts: InsightFacts,
        now: Long,
    ): Boolean {
        if (facts.wrongRecent < StatsThresholds.PATTERN_MIN_WRONG) return false
        val pattern = facts.pattern ?: return true
        return facts.wrongSincePattern >= StatsThresholds.PATTERN_REFRESH_WRONG &&
            now - pattern.generatedAt >= REFRESH_MILLIS
    }

    private fun StudyWindow.enoughIn(span: LongRange): Boolean =
        sessions.count { it.startedAt in span } >= StatsThresholds.INSIGHT_MIN_SESSIONS ||
            attempts.count { it.finishedAt in span } >= StatsThresholds.INSIGHT_MIN_QUIZZES

    private fun StudyWindow.eventsIn(
        span: LongRange,
        after: Long,
    ): Int =
        sessions.count { it.startedAt in span && it.endedAt > after } +
            attempts.count { it.finishedAt in span && it.finishedAt > after } +
            bookmarkEvents.count { it.at in span && it.at > after }

    private const val REFRESH_MILLIS = StatsThresholds.INSIGHT_REFRESH_HOURS * 3_600_000L
}
