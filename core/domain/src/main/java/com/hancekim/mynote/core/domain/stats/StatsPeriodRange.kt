package com.hancekim.mynote.core.domain.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class StatsPeriod {
    Week,
    Month,
    All,
}

enum class BucketState {
    Past,
    Current,
    Future,
    BeforeRecord,
}

data class Bucket(
    val start: LocalDate,
    val endExclusive: LocalDate,
    val state: BucketState,
) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && date.isBefore(endExclusive)
}

data class StatsRange(
    val period: StatsPeriod,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val compareStart: ZonedDateTime?,
    val compareEnd: ZonedDateTime?,
    val buckets: List<Bucket>,
    val firstWeek: Boolean,
    val shortRangeDays: Int?,
)

object StatsPeriodRange {
    fun of(
        period: StatsPeriod,
        now: ZonedDateTime,
        firstRecord: LocalDate?,
    ): StatsRange {
        val today = now.toLocalDate()
        val shortDays =
            firstRecord
                ?.let { (ChronoUnit.DAYS.between(it, today) + 1).toInt().coerceAtLeast(1) }
                ?.takeIf { period != StatsPeriod.Week && it < StatsThresholds.SHORT_RANGE_DAYS }
        return when (period) {
            StatsPeriod.Week -> week(now, today, firstRecord)
            StatsPeriod.Month -> month(now, today, firstRecord, shortDays)
            StatsPeriod.All -> all(now, today, firstRecord, shortDays)
        }
    }

    private fun week(
        now: ZonedDateTime,
        today: LocalDate,
        firstRecord: LocalDate?,
    ): StatsRange {
        val zone = now.zone
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val buckets =
            (0L until DAYS_IN_WEEK).map { offset ->
                val day = monday.plusDays(offset)
                bucket(day, day.plusDays(1), today, firstRecord)
            }
        val firstWeek = firstRecord != null && !firstRecord.isBefore(monday)
        val previousMonday = monday.minusWeeks(1)
        val compare = firstRecord != null && !previousMonday.isBefore(firstRecord)
        return StatsRange(
            period = StatsPeriod.Week,
            start = monday.atStartOfDay(zone),
            end = now,
            compareStart = if (compare) previousMonday.atStartOfDay(zone) else null,
            compareEnd = if (compare) now.minusDays(DAYS_IN_WEEK) else null,
            buckets = buckets,
            firstWeek = firstWeek,
            shortRangeDays = null,
        )
    }

    private fun month(
        now: ZonedDateTime,
        today: LocalDate,
        firstRecord: LocalDate?,
        shortDays: Int?,
    ): StatsRange {
        val zone = now.zone
        val first = today.withDayOfMonth(1)
        val next = first.plusMonths(1)
        val buckets = mutableListOf<Bucket>()
        var start = first
        while (start.isBefore(next)) {
            val end = minOf(start.with(TemporalAdjusters.next(DayOfWeek.MONDAY)), next)
            buckets += bucket(start, end, today, firstRecord)
            start = end
        }
        val previous = first.minusMonths(1)
        val compare = firstRecord != null && !previous.isBefore(firstRecord)
        val compareEnd =
            if (today.dayOfMonth <= previous.lengthOfMonth()) now.minusMonths(1) else first.atStartOfDay(zone)
        return StatsRange(
            period = StatsPeriod.Month,
            start = first.atStartOfDay(zone),
            end = now,
            compareStart = if (compare) previous.atStartOfDay(zone) else null,
            compareEnd = if (compare) compareEnd else null,
            buckets = buckets,
            firstWeek = false,
            shortRangeDays = shortDays,
        )
    }

    private fun all(
        now: ZonedDateTime,
        today: LocalDate,
        firstRecord: LocalDate?,
        shortDays: Int?,
    ): StatsRange {
        val firstMonth = (firstRecord ?: today).withDayOfMonth(1)
        val thisMonth = today.withDayOfMonth(1)
        val months = generateSequence(firstMonth) { it.plusMonths(1) }.takeWhile { !it.isAfter(thisMonth) }.toList()
        val buckets =
            months
                .takeLast(StatsThresholds.MAX_MONTH_BUCKETS)
                .map { bucket(it, it.plusMonths(1), today, firstRecord) }
        return StatsRange(
            period = StatsPeriod.All,
            start = firstMonth.atStartOfDay(now.zone),
            end = now,
            compareStart = null,
            compareEnd = null,
            buckets = buckets,
            firstWeek = false,
            shortRangeDays = shortDays,
        )
    }

    private fun bucket(
        start: LocalDate,
        endExclusive: LocalDate,
        today: LocalDate,
        firstRecord: LocalDate?,
    ): Bucket {
        val state =
            when {
                !today.isBefore(start) && today.isBefore(endExclusive) -> BucketState.Current
                firstRecord == null -> BucketState.BeforeRecord
                start.isAfter(today) -> BucketState.Future
                !endExclusive.isAfter(firstRecord) -> BucketState.BeforeRecord
                else -> BucketState.Past
            }
        return Bucket(start, endExclusive, state)
    }

    private const val DAYS_IN_WEEK = 7L
}
