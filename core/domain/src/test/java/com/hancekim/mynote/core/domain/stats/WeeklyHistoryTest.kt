package com.hancekim.mynote.core.domain.stats

import com.hancekim.mynote.core.domain.model.QuizAttemptSummary
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.WeeklySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class WeeklyHistoryTest {
    private val zone = ZoneOffset.UTC
    private val thisWeek = LocalDate.of(2026, 9, 28)

    private fun at(date: LocalDate) = date.atTime(10, 0).toInstant(zone).toEpochMilli()

    @Test
    fun `첫 기록 주부터 이번 주까지 빠짐없이 최신 주가 위로`() {
        val first = thisWeek.minusWeeks(2)
        val window =
            StudyWindow(
                sessions =
                    listOf(
                        ReadSession("a", "cs", at(first.plusDays(1)), at(first.plusDays(1)) + 1, 600_000, 100),
                    ),
                attempts = listOf(QuizAttemptSummary("a", "cs", at(thisWeek), at(thisWeek), 3, 2)),
                bookmarkEvents = emptyList(),
            )
        val summary = WeeklySummary(first, at(first.plusDays(6)), "h", "d", null)

        val weeks = WeeklyHistory.of(first, thisWeek, zone, window, listOf(summary))

        assertEquals(listOf(thisWeek, thisWeek.minusWeeks(1), first), weeks.map { it.weekStart })
        assertEquals(true, weeks[0].current)
        assertEquals(1, weeks[0].quizzes)
        assertEquals(67, weeks[0].accuracy)
        assertNull(weeks[1].accuracy)
        assertEquals(10, weeks[2].minutes)
        assertEquals(summary, weeks[2].summary)
    }

    @Test
    fun `52 주보다 오래된 첫 기록은 이번 주에서 51 주 전으로 자른다`() {
        val old = at(thisWeek.minusWeeks(80))

        assertEquals(thisWeek.minusWeeks(51), WeeklyHistory.firstWeek(thisWeek, old, zone))
        assertEquals(
            thisWeek.minusWeeks(3),
            WeeklyHistory.firstWeek(thisWeek, at(thisWeek.minusWeeks(3).plusDays(4)), zone),
        )
    }
}
