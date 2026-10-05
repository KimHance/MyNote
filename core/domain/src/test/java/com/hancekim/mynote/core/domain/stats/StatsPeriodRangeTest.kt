package com.hancekim.mynote.core.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
private val NEW_YORK: ZoneId = ZoneId.of("America/New_York")

private fun seoul(
    date: String,
    time: String = "10:00",
): ZonedDateTime = LocalDateTime.parse("${date}T$time").atZone(SEOUL)

private fun date(value: String): LocalDate = LocalDate.parse(value)

class StatsPeriodRangeTest {
    @Test
    fun `월요일 당일은 월이 진행 중이고 나머지는 아직 오지 않았다`() {
        val range = StatsPeriodRange.of(StatsPeriod.Week, seoul("2026-09-28"), date("2026-09-01"))

        assertEquals(BucketState.Current, range.buckets[0].state)
        assertTrue(range.buckets.drop(1).all { it.state == BucketState.Future })
        assertEquals(seoul("2026-09-28", "00:00"), range.start)
    }

    @Test
    fun `일요일 당일은 월부터 토까지 지난 칸이다`() {
        val range = StatsPeriodRange.of(StatsPeriod.Week, seoul("2026-10-04"), date("2026-09-01"))

        assertTrue(range.buckets.take(6).all { it.state == BucketState.Past })
        assertEquals(BucketState.Current, range.buckets[6].state)
    }

    @Test
    fun `달에 걸친 주 - 10월 1일의 이번 달은 다섯 칸이고 이번 주는 9월 28일부터다`() {
        val now = seoul("2026-10-01")
        val month = StatsPeriodRange.of(StatsPeriod.Month, now, date("2026-08-01"))
        val week = StatsPeriodRange.of(StatsPeriod.Week, now, date("2026-08-01"))

        assertEquals(
            listOf(
                date("2026-10-01") to date("2026-10-05"),
                date("2026-10-05") to date("2026-10-12"),
                date("2026-10-12") to date("2026-10-19"),
                date("2026-10-19") to date("2026-10-26"),
                date("2026-10-26") to date("2026-11-01"),
            ),
            month.buckets.map { it.start to it.endExclusive },
        )
        assertEquals(date("2026-09-28"), week.buckets.first().start)
        assertEquals(date("2026-10-04"), week.buckets.last().start)
    }

    @Test
    fun `첫 기록이 이번 주 안이면 첫 주이고 비교하지 않는다`() {
        val range = StatsPeriodRange.of(StatsPeriod.Week, seoul("2026-10-01"), date("2026-09-29"))

        assertTrue(range.firstWeek)
        assertNull(range.compareStart)
        assertNull(range.compareEnd)
        assertEquals(BucketState.BeforeRecord, range.buckets[0].state)
        assertEquals(BucketState.Past, range.buckets[1].state)
    }

    @Test
    fun `지난주 비교는 지난 월요일 0시부터 같은 시각까지다`() {
        val now = seoul("2026-10-01", "14:30")
        val range = StatsPeriodRange.of(StatsPeriod.Week, now, date("2026-09-01"))

        assertFalse(range.firstWeek)
        assertEquals(seoul("2026-09-21", "00:00"), range.compareStart)
        assertEquals(seoul("2026-09-24", "14:30"), range.compareEnd)
    }

    @Test
    fun `기록 일수 경계 - 오늘 포함 13일 치는 짧고 14일 치는 아니다`() {
        val now = seoul("2026-10-20")
        val thirteen = StatsPeriodRange.of(StatsPeriod.Month, now, date("2026-10-08"))
        val fourteen = StatsPeriodRange.of(StatsPeriod.Month, now, date("2026-10-07"))
        val week = StatsPeriodRange.of(StatsPeriod.Week, now, date("2026-10-08"))
        val all = StatsPeriodRange.of(StatsPeriod.All, now, date("2026-10-14"))

        assertEquals(13, thirteen.shortRangeDays)
        assertNull(fourteen.shortRangeDays)
        assertNull(week.shortRangeDays)
        assertEquals(7, all.shortRangeDays)
    }

    @Test
    fun `같은 순간도 시간대가 다르면 요일 칸이 하루 어긋난다`() {
        val instant = Instant.parse("2026-09-27T23:30:00Z")
        val inSeoul = StatsPeriodRange.of(StatsPeriod.Week, instant.atZone(SEOUL), date("2026-09-01"))
        val inNewYork = StatsPeriodRange.of(StatsPeriod.Week, instant.atZone(NEW_YORK), date("2026-09-01"))

        assertEquals(date("2026-09-28"), inSeoul.buckets.first { it.state == BucketState.Current }.start)
        assertEquals(date("2026-09-27"), inNewYork.buckets.first { it.state == BucketState.Current }.start)
        assertEquals(0, inSeoul.buckets.indexOfFirst { it.state == BucketState.Current })
        assertEquals(6, inNewYork.buckets.indexOfFirst { it.state == BucketState.Current })
    }

    @Test
    fun `서머타임 시작일의 칸은 23시간이다`() {
        val now = LocalDateTime.parse("2026-03-08T12:00").atZone(NEW_YORK)
        val range = StatsPeriodRange.of(StatsPeriod.Week, now, date("2026-01-01"))
        val dstDay = range.buckets.first { it.start == date("2026-03-08") }

        val hours =
            Duration.between(dstDay.start.atStartOfDay(NEW_YORK), dstDay.endExclusive.atStartOfDay(NEW_YORK)).toHours()

        assertEquals(23L, hours)
        assertEquals(LocalDateTime.parse("2026-03-02T00:00").atZone(NEW_YORK), range.start)
    }

    @Test
    fun `윤년 2월의 이번 달 칸과 3월 30일의 지난달 비교`() {
        val february = StatsPeriodRange.of(StatsPeriod.Month, seoul("2028-02-29"), date("2027-01-01"))
        val march = StatsPeriodRange.of(StatsPeriod.Month, seoul("2028-03-30"), date("2027-01-01"))

        assertEquals(date("2028-03-01"), february.buckets.last().endExclusive)
        assertEquals(5, february.buckets.size)
        assertEquals(BucketState.Current, february.buckets.last().state)
        assertEquals(seoul("2028-02-01", "00:00"), march.compareStart)
        assertEquals(seoul("2028-03-01", "00:00"), march.compareEnd)
    }

    @Test
    fun `3월 31일의 지난달 비교는 2월 말일 끝까지다`() {
        val range = StatsPeriodRange.of(StatsPeriod.Month, seoul("2027-03-31"), date("2026-01-01"))

        assertEquals(seoul("2027-02-01", "00:00"), range.compareStart)
        assertEquals(seoul("2027-03-01", "00:00"), range.compareEnd)
    }

    @Test
    fun `해를 넘는 주`() {
        val range = StatsPeriodRange.of(StatsPeriod.Week, seoul("2026-12-31"), date("2026-01-01"))

        assertEquals(date("2026-12-28"), range.buckets.first().start)
        assertEquals(date("2027-01-03"), range.buckets.last().start)
    }

    @Test
    fun `전체가 13개월이면 칸은 최근 12개다`() {
        val range = StatsPeriodRange.of(StatsPeriod.All, seoul("2026-10-01"), date("2025-10-15"))

        assertEquals(12, range.buckets.size)
        assertEquals(date("2025-11-01"), range.buckets.first().start)
        assertEquals(seoul("2025-10-01", "00:00"), range.start)
        assertNull(range.compareStart)
    }

    @Test
    fun `기록 0 이면 오늘 칸만 진행 중이고 나머지는 기록 전이다`() {
        val week = StatsPeriodRange.of(StatsPeriod.Week, seoul("2026-10-01"), null)
        val all = StatsPeriodRange.of(StatsPeriod.All, seoul("2026-10-01"), null)

        assertEquals(1, week.buckets.count { it.state == BucketState.Current })
        assertEquals(6, week.buckets.count { it.state == BucketState.BeforeRecord })
        assertNull(week.compareStart)
        assertFalse(week.firstWeek)
        assertEquals(listOf(BucketState.Current), all.buckets.map { it.state })
    }

    @Test
    fun `첫 기록이 지난 금요일이고 오늘이 화요일이면 지난주 비교가 없다`() {
        val range = StatsPeriodRange.of(StatsPeriod.Week, seoul("2026-10-06"), date("2026-10-02"))

        assertFalse(range.firstWeek)
        assertNull(range.compareStart)
        assertNull(range.compareEnd)
    }

    @Test
    fun `첫 기록이 지난 월요일이면 지난주와 비교한다`() {
        val range = StatsPeriodRange.of(StatsPeriod.Week, seoul("2026-10-06"), date("2026-09-28"))

        assertEquals(seoul("2026-09-28", "00:00"), range.compareStart)
    }

    @Test
    fun `첫 기록 9월 28일 오늘 10월 2일이면 이번 달 비교가 없다`() {
        val range = StatsPeriodRange.of(StatsPeriod.Month, seoul("2026-10-02"), date("2026-09-28"))

        assertNull(range.compareStart)
        assertEquals(5, range.shortRangeDays)
    }

    @Test
    fun `첫 기록이 지난달 중간이면 14일이 넘어도 이번 달 비교가 없다`() {
        val range = StatsPeriodRange.of(StatsPeriod.Month, seoul("2026-10-20"), date("2026-09-15"))

        assertNull(range.shortRangeDays)
        assertNull(range.compareStart)
    }
}
