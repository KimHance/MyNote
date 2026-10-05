package com.hancekim.mynote.feature.notes.stats

import com.hancekim.mynote.core.domain.model.InsightSnapshot
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.WeeklySummary
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class SummaryCardRulesTest {
    private val zone = ZoneOffset.UTC
    private val now = LocalDateTime.of(2026, 10, 1, 9, 0).atZone(zone)
    private val thisWeek = LocalDate.of(2026, 9, 28)

    private fun at(date: LocalDate) = date.atTime(8, 0).toInstant(zone).toEpochMilli()

    private fun summary(
        week: LocalDate = thisWeek,
        generated: LocalDate = LocalDate.of(2026, 9, 30),
        focus: String? = "cs",
    ) = WeeklySummary(week, at(generated), "h", "d", focus)

    private fun snapshot(
        latest: WeeklySummary? = summary(),
        failureAt: Long? = null,
        generating: Boolean = false,
        pattern: QuizPattern? = null,
    ) = InsightSnapshot(latest, pattern, failureAt, generating)

    @Test
    fun `모델이 없으면 카드가 없다`() {
        assertNull(summaryCardOf(snapshot(), modelReady = false, period = StatsPeriod.Week, now = now))
    }

    @Test
    fun `캐시가 없으면 카드가 없다`() {
        assertNull(summaryCardOf(snapshot(latest = null), modelReady = true, period = StatsPeriod.Week, now = now))
    }

    @Test
    fun `이번 주 지난주 그 이전 라벨`() {
        fun label(week: LocalDate) = summaryCardOf(snapshot(summary(week = week)), true, StatsPeriod.Week, now)?.label

        assertEquals(SummaryLabel.ThisWeek, label(thisWeek))
        assertEquals(SummaryLabel.LastWeek, label(thisWeek.minusWeeks(1)))
        assertEquals(
            SummaryLabel.Range(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)),
            label(thisWeek.minusWeeks(2)),
        )
    }

    @Test
    fun `9 일은 오래되지 않았고 10 일은 오래됐다`() {
        val nine =
            summaryCardOf(
                snapshot(summary(generated = LocalDate.of(2026, 9, 22))),
                true,
                StatsPeriod.Week,
                now,
            )!!
        val ten = summaryCardOf(snapshot(summary(generated = LocalDate.of(2026, 9, 21))), true, StatsPeriod.Week, now)!!

        assertFalse(nine.stale)
        assertEquals(GeneratedAgo.Recent(LocalDate.of(2026, 9, 22)), nine.generated)
        assertTrue(ten.stale)
        assertEquals(GeneratedAgo.Stale(10), ten.generated)
    }

    @Test
    fun `분석 중이 실패를 이긴다`() {
        val card = summaryCardOf(snapshot(failureAt = Long.MAX_VALUE, generating = true), true, StatsPeriod.Week, now)!!

        assertTrue(card.running)
        assertFalse(card.failed)
    }

    @Test
    fun `실패 시각이 생성 시각보다 뒤일 때만 실패다`() {
        val generatedAt = summary().generatedAt

        assertTrue(summaryCardOf(snapshot(failureAt = generatedAt + 1), true, StatsPeriod.Week, now)!!.failed)
        assertFalse(summaryCardOf(snapshot(failureAt = generatedAt), true, StatsPeriod.Week, now)!!.failed)
    }

    @Test
    fun `포기만 있으면 실패가 아니다`() {
        assertFalse(summaryCardOf(snapshot(failureAt = null), true, StatsPeriod.Week, now)!!.failed)
    }

    @Test
    fun `오래됐으면 범례 초점을 붙이지 않는다`() {
        val stale =
            summaryCardOf(
                snapshot(summary(generated = LocalDate.of(2026, 9, 18))),
                true,
                StatsPeriod.Week,
                now,
            )!!

        assertFalse(stale.focusInLegend)
    }

    @Test
    fun `기간이 이번 주가 아니거나 지난주 요약이면 범례 초점이 없다`() {
        assertTrue(summaryCardOf(snapshot(), true, StatsPeriod.Week, now)!!.focusInLegend)
        assertFalse(summaryCardOf(snapshot(), true, StatsPeriod.Month, now)!!.focusInLegend)
        assertFalse(
            summaryCardOf(
                snapshot(summary(week = thisWeek.minusWeeks(1))),
                true,
                StatsPeriod.Week,
                now,
            )!!.focusInLegend,
        )
        assertFalse(summaryCardOf(snapshot(summary(focus = null)), true, StatsPeriod.Week, now)!!.focusInLegend)
    }

    @Test
    fun `패턴은 모델 Ready 이고 10 일 미만일 때만 보인다`() {
        fun pattern(generated: LocalDate) = QuizPattern(thisWeek, at(generated), listOf("스택"), "s")

        assertNotNull(patternOf(snapshot(pattern = pattern(LocalDate.of(2026, 9, 22))), true, now))
        assertNull(patternOf(snapshot(pattern = pattern(LocalDate.of(2026, 9, 21))), true, now))
        assertNull(patternOf(snapshot(pattern = pattern(LocalDate.of(2026, 9, 30))), false, now))
        assertNull(patternOf(snapshot(pattern = null), true, now))
    }
}
