package com.hancekim.mynote.core.domain.stats

import com.hancekim.mynote.core.domain.model.QuizAttemptSummary
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class WeeklyDigestTest {
    private val zone = ZoneOffset.UTC
    private val week = LocalDate.of(2026, 9, 28)
    private val notes =
        listOf(
            NoteRef("hash", "해시 테이블", "datastructure"),
            NoteRef("flow", "Flow 기초", "kotlin"),
            NoteRef("tcp", "TCP", "cs"),
        )

    private fun at(
        date: LocalDate,
        hour: Int = 10,
    ) = date.atTime(hour, 0).toInstant(zone).toEpochMilli()

    private fun session(
        id: String,
        category: String,
        start: Long,
        minutes: Int,
        percent: Int,
    ) = ReadSession(id, category, start, start + minutes * 60_000L, minutes * 60_000L, percent)

    @Test
    fun `이번 주 숫자와 지난주 같은 구간 비교와 차이를 도메인이 계산한다`() {
        val now = LocalDateTime.of(2026, 10, 1, 18, 0).atZone(zone)
        val window =
            StudyWindow(
                sessions =
                    listOf(
                        session("hash", "datastructure", at(week), 20, 40),
                        session("hash", "datastructure", at(week.plusDays(1)), 15, 60),
                        session("flow", "kotlin", at(week.plusDays(2)), 10, 100),
                        session("tcp", "cs", at(week.minusDays(6)), 30, 100),
                        session("tcp", "cs", at(week.minusDays(1)), 30, 100),
                    ),
                attempts =
                    listOf(
                        QuizAttemptSummary("flow", "kotlin", at(week.plusDays(2)), at(week.plusDays(2)), 4, 3),
                        QuizAttemptSummary("tcp", "cs", at(week.minusDays(6)), at(week.minusDays(6)), 4, 1),
                    ),
                bookmarkEvents = emptyList(),
            )

        val digest = WeeklyDigestBuilder.of(week, now, window, notes, firstRecordAt = at(week.minusDays(10)))

        assertEquals(3, digest.days)
        assertEquals(3, digest.daysLeft)
        assertEquals(DigestTotals(notes = 2, minutes = 45, quizzes = 1, accuracy = 75), digest.current)
        assertEquals(DigestTotals(notes = 1, minutes = 30, quizzes = 1, accuracy = 25), digest.previous)
        assertEquals(DigestTotals(notes = 1, minutes = 15, quizzes = 0, accuracy = 50), digest.change)
        assertEquals(listOf("cs", "datastructure", "kotlin"), digest.categoryIds)
        assertEquals(DigestCategory("cs", 0, 0, 0), digest.categories[0])
        assertEquals(DigestCategory("kotlin", 10, 3, 4), digest.categories[2])
        assertEquals("datastructure", digest.top)
        assertEquals("cs", digest.least)
        assertEquals(DigestTrend.More, digest.trend)
        assertEquals(listOf("해시 테이블"), digest.titles)
        assertEquals(listOf("해시 테이블"), digest.stalled)
        assertEquals("해시 테이블", digest.reread)
    }

    @Test
    fun `지난주 비교 시작이 첫 기록보다 앞이면 비교하지 않는다`() {
        val now = LocalDateTime.of(2026, 10, 1, 18, 0).atZone(zone)
        val window = StudyWindow(listOf(session("hash", "datastructure", at(week), 20, 40)), emptyList(), emptyList())

        val digest = WeeklyDigestBuilder.of(week, now, window, notes, firstRecordAt = at(week.minusDays(3)))

        assertNull(digest.previous)
        assertNull(digest.change)
        assertNull(digest.trend)
    }

    @Test
    fun `끝난 주는 남은 날이 0 이고 7 일 전체를 본다`() {
        val now = LocalDateTime.of(2026, 10, 6, 9, 0).atZone(zone)
        val window =
            StudyWindow(
                listOf(session("hash", "datastructure", at(week.plusDays(6), hour = 23), 20, 95)),
                emptyList(),
                emptyList(),
            )

        val digest = WeeklyDigestBuilder.of(week, now, window, notes, firstRecordAt = at(week))

        assertEquals(0, digest.daysLeft)
        assertEquals(20, digest.current.minutes)
        assertEquals(emptyList<String>(), digest.stalled)
        assertNull(digest.reread)
    }
}
