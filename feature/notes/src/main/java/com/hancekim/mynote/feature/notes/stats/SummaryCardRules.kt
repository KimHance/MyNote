package com.hancekim.mynote.feature.notes.stats

import com.hancekim.mynote.core.domain.model.InsightSnapshot
import com.hancekim.mynote.core.domain.stats.HistoryWeek
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.core.domain.stats.WeeklyHistory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.time.ZonedDateTime

internal fun summaryCardOf(
    snapshot: InsightSnapshot,
    modelReady: Boolean,
    period: StatsPeriod,
    now: ZonedDateTime,
): SummaryCardUi? {
    val latest = snapshot.latest ?: return null
    if (!modelReady) return null
    val thisWeek = WeeklyHistory.mondayOf(now.toLocalDate())
    val label =
        when (latest.weekStart) {
            thisWeek -> SummaryLabel.ThisWeek
            thisWeek.minusWeeks(1) -> SummaryLabel.LastWeek
            else -> SummaryLabel.Range(latest.weekStart, latest.weekStart.plusDays(DAYS_IN_WEEK - 1))
        }
    val generated = GeneratedAgo.of(latest.generatedAt, now.toLocalDate(), now.zone)
    val stale = generated is GeneratedAgo.Stale
    val running = snapshot.generating
    val failed = !running && (snapshot.lastFailureAt ?: Long.MIN_VALUE) > latest.generatedAt
    return SummaryCardUi(
        label = label,
        generated = generated,
        headline = latest.headline,
        detail = latest.detail,
        focus = latest.focus,
        stale = stale,
        running = running,
        failed = failed,
        focusInLegend = period == StatsPeriod.Week && label == SummaryLabel.ThisWeek && !stale && latest.focus != null,
    )
}

internal fun patternOf(
    snapshot: InsightSnapshot,
    modelReady: Boolean,
    now: ZonedDateTime,
): PatternUi? {
    val pattern = snapshot.pattern ?: return null
    if (!modelReady) return null
    val generated = GeneratedAgo.of(pattern.generatedAt, now.toLocalDate(), now.zone)
    if (generated is GeneratedAgo.Stale) return null
    return PatternUi(pattern.tags.toImmutableList(), pattern.sentence, generated)
}

internal fun List<HistoryWeek>.toHistoryUi(now: ZonedDateTime): ImmutableList<HistoryWeekUi> =
    map { week ->
        val summary = week.summary
        HistoryWeekUi(
            weekStart = week.weekStart,
            current = week.current,
            headline = summary?.headline,
            detail = summary?.detail,
            focus = summary?.focus,
            generatedOn = summary?.let { Instant.ofEpochMilli(it.generatedAt).atZone(now.zone).toLocalDate() },
            notes = week.notes,
            minutes = week.minutes,
            quizzes = week.quizzes,
            accuracy = week.accuracy,
        )
    }.toImmutableList()

private const val DAYS_IN_WEEK = 7L
