package com.hancekim.mynote.feature.stats

import com.hancekim.mynote.core.domain.model.NoteFacts
import com.hancekim.mynote.core.domain.model.QuizAttemptSummary
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.core.domain.stats.StatsPeriodRange
import com.hancekim.mynote.core.domain.stats.StudyStatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val ZONE = ZoneId.of("Asia/Seoul")
private val NOW = LocalDateTime.parse("2026-10-01T15:00").atZone(ZONE)

private fun at(date: String): Long =
    LocalDate
        .parse(date)
        .atTime(10, 0)
        .atZone(ZONE)
        .toInstant()
        .toEpochMilli()

private fun content(
    period: StatsPeriod,
    firstRecord: String?,
    sessions: List<ReadSession> = emptyList(),
    attempts: List<QuizAttemptSummary> = emptyList(),
    now: ZonedDateTime = NOW,
): StatsContentUi {
    val first = firstRecord?.let { LocalDate.parse(it) }
    return StudyStatsCalculator
        .compute(
            range = StatsPeriodRange.of(period, now, first),
            now = now,
            firstRecordAt = firstRecord?.let { at(it) },
            window = StudyWindow(sessions, attempts, emptyList()),
            facts = NoteFacts(sessions, attempts),
            notes = emptyList(),
            bookmarks = emptyList(),
        ).toUi()
}

private fun session(date: String) = ReadSession("a", "android", at(date), at(date) + 60_000, 60_000, 50)

class StatsUiMapperTest {
    @Test
    fun `기록 0 이면 안내와 대시와 대기 캡션`() {
        val ui = content(StatsPeriod.Week, firstRecord = null)

        assertTrue(ui.intro)
        assertTrue(ui.numbers.all { it.delta == DeltaUi.None })
        assertNull(ui.numbers.last().value)
        assertEquals(0, ui.numbers.first().value)
        assertEquals(ComparisonUi.Pending, ui.comparison)
    }

    @Test
    fun `첫 주는 변화량 대신 첫 주와 문항 수`() {
        val attempt = QuizAttemptSummary("a", "android", at("2026-09-30"), at("2026-09-30"), 3, 2)
        val ui = content(StatsPeriod.Week, firstRecord = "2026-09-29", attempts = listOf(attempt))

        assertFalse(ui.intro)
        assertEquals(List(3) { DeltaUi.FirstWeek }, ui.numbers.take(3).map { it.delta })
        assertEquals(DeltaUi.Questions(3), ui.numbers.last().delta)
        assertEquals(ComparisonUi.Since(LocalDate.parse("2026-09-29")), ui.comparison)
    }

    @Test
    fun `평소 이번 주는 지난주 같은 요일까지와 비교한다`() {
        val ui =
            content(
                StatsPeriod.Week,
                firstRecord = "2026-08-01",
                sessions = listOf(session("2026-09-22"), session("2026-09-30")),
            )

        assertEquals(DeltaUi.Value(0), ui.numbers.first().delta)
        assertEquals(DeltaUi.None, ui.numbers.last().delta)
        assertEquals(ComparisonUi.Week(DayOfWeek.THURSDAY), ui.comparison)
    }

    @Test
    fun `전체는 변화량 자리가 비고 첫 기록일부터`() {
        val ui = content(StatsPeriod.All, firstRecord = "2026-08-01")

        assertTrue(ui.numbers.all { it.delta == DeltaUi.Blank })
        assertEquals(ComparisonUi.Since(LocalDate.parse("2026-08-01")), ui.comparison)
        assertNull(ui.periodNote)
    }

    @Test
    fun `이번 달에 첫 기록이 있으면 비교하지 않고 첫 기록일부터라고 쓴다`() {
        val ui = content(StatsPeriod.Month, firstRecord = "2026-10-01")

        assertTrue(ui.numbers.all { it.delta == DeltaUi.None })
        assertEquals(ComparisonUi.Since(LocalDate.parse("2026-10-01")), ui.comparison)
    }

    @Test
    fun `첫 기록이 지난 금요일이고 오늘이 화요일이면 이번 주는 비교하지 않는다`() {
        val tuesday = LocalDateTime.parse("2026-10-06T15:00").atZone(ZONE)
        val ui =
            content(
                StatsPeriod.Week,
                firstRecord = "2026-10-02",
                sessions = listOf(session("2026-10-05")),
                now = tuesday,
            )

        assertTrue(ui.numbers.all { it.delta == DeltaUi.None })
        assertEquals(ComparisonUi.Since(LocalDate.parse("2026-10-02")), ui.comparison)
    }

    @Test
    fun `첫 기록 9월 28일 오늘 10월 2일 이번 달은 9월 28일부터`() {
        val friday = LocalDateTime.parse("2026-10-02T15:00").atZone(ZONE)
        val ui =
            content(
                StatsPeriod.Month,
                firstRecord = "2026-09-28",
                sessions = listOf(session("2026-09-29")),
                now = friday,
            )

        assertTrue(ui.numbers.all { it.delta == DeltaUi.None })
        assertEquals(ComparisonUi.Since(LocalDate.parse("2026-09-28")), ui.comparison)
    }

    @Test
    fun `비교 시작일이 첫 기록일과 같으면 비교한다`() {
        val ui = content(StatsPeriod.Month, firstRecord = "2026-09-01", sessions = listOf(session("2026-09-01")))

        assertEquals(ComparisonUi.Month(1), ui.comparison)
        assertEquals(DeltaUi.Value(-1), ui.numbers.first().delta)
    }

    @Test
    fun `14일 미만 안내는 7일 이하와 8에서 13일로 나뉜다`() {
        val short = content(StatsPeriod.Month, firstRecord = "2026-09-28")
        val partial = content(StatsPeriod.All, firstRecord = "2026-09-21")
        val enough = content(StatsPeriod.Month, firstRecord = "2026-08-18")

        assertEquals(PeriodNoteUi(LocalDate.parse("2026-09-28"), 4), short.periodNote)
        assertTrue(short.periodNote?.short == true)
        assertEquals(ComparisonUi.Since(LocalDate.parse("2026-09-28")), short.comparison)
        assertEquals(11, partial.periodNote?.days)
        assertFalse(partial.periodNote?.short == true)
        assertNull(enough.periodNote)
        assertEquals(ComparisonUi.Month(1), enough.comparison)
    }
}
