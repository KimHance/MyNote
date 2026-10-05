package com.hancekim.mynote.feature.notes.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GeneratedAgoTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 10, 2)

    private fun at(
        date: LocalDate,
        hour: Int,
    ) = date
        .atTime(hour, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `달력 날짜 차로 오늘 어제 날짜 며칠 전`() {
        assertEquals(GeneratedAgo.Today, GeneratedAgo.of(at(today, 0), today, zone))
        assertEquals(GeneratedAgo.Yesterday, GeneratedAgo.of(at(today.minusDays(1), 23), today, zone))
        assertEquals(GeneratedAgo.Recent(today.minusDays(2)), GeneratedAgo.of(at(today.minusDays(2), 12), today, zone))
        assertEquals(GeneratedAgo.Recent(today.minusDays(9)), GeneratedAgo.of(at(today.minusDays(9), 12), today, zone))
        assertEquals(GeneratedAgo.Stale(10), GeneratedAgo.of(at(today.minusDays(10), 12), today, zone))
    }

    @Test
    fun `시간대가 날짜를 정한다`() {
        val lateUtc = at(today.minusDays(1), 23)

        assertEquals(GeneratedAgo.Yesterday, GeneratedAgo.of(lateUtc, today, zone))
        assertEquals(GeneratedAgo.Yesterday, GeneratedAgo.of(lateUtc, today, ZoneId.of("UTC")))
    }
}
