package com.hancekim.mynote.core.domain.stats

import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.BookmarkEventKind
import com.hancekim.mynote.core.domain.model.QuizAttemptSummary
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.WeeklySummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class InsightPlanTest {
    private val zone = ZoneOffset.UTC
    private val thisWeek = LocalDate.of(2026, 9, 28)
    private val lastWeek = LocalDate.of(2026, 9, 21)

    private fun at(
        date: LocalDate,
        hour: Int = 12,
    ): Long = date.atTime(hour, 0).toInstant(zone).toEpochMilli()

    private fun now(dateTime: LocalDateTime): ZonedDateTime = dateTime.atZone(zone)

    private fun session(start: Long) = ReadSession("n", "android", start, start + 60_000, 60_000, 50)

    private fun attempt(end: Long) = QuizAttemptSummary("n", "android", end - 60_000, end, 3, 2)

    private fun window(
        sessions: List<Long> = emptyList(),
        attempts: List<Long> = emptyList(),
        events: List<Long> = emptyList(),
    ) = StudyWindow(
        sessions = sessions.map(::session),
        attempts = attempts.map(::attempt),
        bookmarkEvents = events.map { BookmarkEvent("n", "android", BookmarkEventKind.Added, it) },
    )

    private fun facts(
        now: ZonedDateTime,
        window: StudyWindow = window(),
        summaries: List<WeeklySummary> = emptyList(),
        pattern: QuizPattern? = null,
        wrongRecent: Int = 0,
        wrongSincePattern: Int = 0,
    ) = InsightFacts(now, window, summaries, pattern, wrongRecent, wrongSincePattern)

    private fun summary(
        week: LocalDate,
        generatedAt: Long,
    ) = WeeklySummary(week, generatedAt, "h", "d", null)

    @Test
    fun `기록이 없으면 할 일이 없다`() {
        assertEquals(emptyList<InsightTarget>(), InsightPlan.of(facts(now(LocalDateTime.of(2026, 10, 1, 9, 0)))))
    }

    @Test
    fun `월요일 첫 실행은 지난주 확정만 한다`() {
        val monday = now(LocalDateTime.of(2026, 9, 28, 3, 0))
        val window = window(sessions = listOf(at(lastWeek), at(lastWeek.plusDays(2)), at(lastWeek.plusDays(4))))

        assertEquals(
            listOf(InsightTarget.Summary(lastWeek, SummaryKind.LastWeek)),
            InsightPlan.of(facts(monday, window)),
        )
    }

    @Test
    fun `이번 주 세션 2 퀴즈 1 이면 최소량이 안 된다`() {
        val window =
            window(
                sessions = listOf(at(thisWeek), at(thisWeek.plusDays(1))),
                attempts = listOf(at(thisWeek.plusDays(1))),
            )

        assertEquals(
            emptyList<InsightTarget>(),
            InsightPlan.of(facts(now(LocalDateTime.of(2026, 10, 1, 9, 0)), window)),
        )
    }

    @Test
    fun `이번 주 세션 3 이면 첫 요약을 만든다`() {
        val window = window(sessions = listOf(at(thisWeek), at(thisWeek.plusDays(1)), at(thisWeek.plusDays(2))))

        assertEquals(
            listOf(InsightTarget.Summary(thisWeek, SummaryKind.ThisWeekFirst)),
            InsightPlan.of(facts(now(LocalDateTime.of(2026, 10, 1, 9, 0)), window)),
        )
    }

    @Test
    fun `이번 주 퀴즈 2 회도 최소량이다`() {
        val window = window(attempts = listOf(at(thisWeek), at(thisWeek.plusDays(1))))

        assertEquals(
            listOf(InsightTarget.Summary(thisWeek, SummaryKind.ThisWeekFirst)),
            InsightPlan.of(facts(now(LocalDateTime.of(2026, 10, 1, 9, 0)), window)),
        )
    }

    private fun refreshCase(
        newEvents: Int,
        hoursSince: Long,
    ): List<InsightTarget> {
        val now = now(LocalDateTime.of(2026, 10, 3, 12, 0))
        val generatedAt = now.toInstant().toEpochMilli() - hoursSince * 3_600_000L
        val events = (1..newEvents).map { generatedAt + it * 1_000L }
        return InsightPlan.of(facts(now, window(events = events), summaries = listOf(summary(thisWeek, generatedAt))))
    }

    @Test
    fun `재생성은 새 이벤트 20 개와 24 시간이 둘 다 있어야 한다`() {
        assertEquals(emptyList<InsightTarget>(), refreshCase(newEvents = 19, hoursSince = 25))
        assertEquals(emptyList<InsightTarget>(), refreshCase(newEvents = 20, hoursSince = 23))
        assertEquals(
            listOf(InsightTarget.Summary(thisWeek, SummaryKind.ThisWeekRefresh)),
            refreshCase(newEvents = 20, hoursSince = 24),
        )
    }

    @Test
    fun `일요일 밤에 만든 지난주 요약은 그 뒤 이벤트가 없으면 다시 만들지 않는다`() {
        val sundayNight = at(lastWeek.plusDays(6), hour = 23)
        val window = window(sessions = listOf(at(lastWeek), at(lastWeek.plusDays(1)), at(lastWeek.plusDays(2))))

        assertEquals(
            emptyList<InsightTarget>(),
            InsightPlan.of(
                facts(now(LocalDateTime.of(2026, 9, 29, 9, 0)), window, listOf(summary(lastWeek, sundayNight))),
            ),
        )
    }

    @Test
    fun `주 중간에 만든 지난주 요약은 그 뒤 이벤트가 있으면 확정한다`() {
        val wednesday = at(lastWeek.plusDays(2), hour = 13)
        val window =
            window(
                sessions =
                    listOf(
                        at(lastWeek),
                        at(lastWeek.plusDays(1)),
                        at(lastWeek.plusDays(2)),
                        at(lastWeek.plusDays(5)),
                    ),
            )

        assertEquals(
            listOf(InsightTarget.Summary(lastWeek, SummaryKind.LastWeek)),
            InsightPlan.of(
                facts(now(LocalDateTime.of(2026, 9, 29, 9, 0)), window, listOf(summary(lastWeek, wednesday))),
            ),
        )
    }

    @Test
    fun `지난주 요약을 다음 주에 만들었으면 다시 만들지 않는다`() {
        val window =
            window(
                sessions =
                    listOf(
                        at(lastWeek),
                        at(lastWeek.plusDays(1)),
                        at(lastWeek.plusDays(2)),
                        at(lastWeek.plusDays(5)),
                    ),
            )

        assertEquals(
            emptyList<InsightTarget>(),
            InsightPlan.of(
                facts(now(LocalDateTime.of(2026, 9, 30, 9, 0)), window, listOf(summary(lastWeek, at(thisWeek)))),
            ),
        )
    }

    @Test
    fun `오답이 2 개면 패턴을 만들지 않고 3 개면 만든다`() {
        val now = now(LocalDateTime.of(2026, 10, 1, 9, 0))

        assertEquals(emptyList<InsightTarget>(), InsightPlan.of(facts(now, wrongRecent = 2)))
        assertEquals(listOf(InsightTarget.Pattern(thisWeek)), InsightPlan.of(facts(now, wrongRecent = 3)))
    }

    @Test
    fun `패턴 재생성은 새 오답 3 개와 24 시간이 둘 다 있어야 한다`() {
        val now = now(LocalDateTime.of(2026, 10, 1, 9, 0))
        val millis = now.toInstant().toEpochMilli()

        fun pattern(hours: Long) = QuizPattern(thisWeek, millis - hours * 3_600_000L, listOf("t"), "s")

        assertEquals(
            emptyList<InsightTarget>(),
            InsightPlan.of(facts(now, pattern = pattern(30), wrongRecent = 9, wrongSincePattern = 2)),
        )
        assertEquals(
            emptyList<InsightTarget>(),
            InsightPlan.of(facts(now, pattern = pattern(23), wrongRecent = 9, wrongSincePattern = 3)),
        )
        assertEquals(
            listOf(InsightTarget.Pattern(thisWeek)),
            InsightPlan.of(facts(now, pattern = pattern(24), wrongRecent = 9, wrongSincePattern = 3)),
        )
    }

    @Test
    fun `한 번에 지난주 확정 이번 주 요약 패턴 순서로 셋까지`() {
        val window =
            window(
                sessions =
                    listOf(at(lastWeek), at(lastWeek.plusDays(1)), at(lastWeek.plusDays(2))) +
                        listOf(at(thisWeek), at(thisWeek.plusDays(1)), at(thisWeek.plusDays(2))),
            )

        assertEquals(
            listOf(
                InsightTarget.Summary(lastWeek, SummaryKind.LastWeek),
                InsightTarget.Summary(thisWeek, SummaryKind.ThisWeekFirst),
                InsightTarget.Pattern(thisWeek),
            ),
            InsightPlan.of(facts(now(LocalDateTime.of(2026, 10, 1, 9, 0)), window, wrongRecent = 4)),
        )
    }
}
