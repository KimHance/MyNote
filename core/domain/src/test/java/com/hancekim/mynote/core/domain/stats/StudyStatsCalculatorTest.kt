package com.hancekim.mynote.core.domain.stats

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.BookmarkEventKind
import com.hancekim.mynote.core.domain.model.NoteFacts
import com.hancekim.mynote.core.domain.model.QuizAttemptSummary
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private const val MINUTE = 60_000L
private val NOW: ZonedDateTime = LocalDateTime.parse("2026-10-01T15:00").atZone(ZONE)

private fun at(
    date: String,
    time: String = "10:00",
): Long =
    LocalDateTime
        .parse("${date}T$time")
        .atZone(ZONE)
        .toInstant()
        .toEpochMilli()

private fun session(
    noteId: String,
    category: String,
    date: String,
    minutes: Long,
    percent: Int = 50,
): ReadSession {
    val start = at(date)
    return ReadSession(noteId, category, start, start + minutes * MINUTE, minutes * MINUTE, percent)
}

private fun attempt(
    noteId: String,
    category: String,
    date: String,
    asked: Int,
    correct: Int,
    time: String = "11:00",
): QuizAttemptSummary = QuizAttemptSummary(noteId, category, at(date, time), at(date, time), asked, correct)

private val notes =
    listOf(
        NoteRef("hash", "해시 테이블", "datastructure"),
        NoteRef("room", "Room 마이그레이션", "android"),
        NoteRef("flow", "Flow 기초", "kotlin"),
        NoteRef("recomp", "리컴포지션", "compose"),
    )

private fun compute(
    period: StatsPeriod = StatsPeriod.Week,
    firstRecord: String? = "2026-08-01",
    sessions: List<ReadSession> = emptyList(),
    attempts: List<QuizAttemptSummary> = emptyList(),
    events: List<BookmarkEvent> = emptyList(),
    latest: List<ReadSession> = sessions,
    allAttempts: List<QuizAttemptSummary> = attempts,
    bookmarks: List<Bookmark> = emptyList(),
    noteRefs: List<NoteRef> = notes,
): StudyStats {
    val first = firstRecord?.let { LocalDate.parse(it) }
    val range = StatsPeriodRange.of(period, NOW, first)
    return StudyStatsCalculator.compute(
        range = range,
        now = NOW,
        firstRecordAt = firstRecord?.let { at(it, "09:00") },
        window = StudyWindow(sessions, attempts, events),
        facts = NoteFacts(latest, allAttempts),
        notes = noteRefs,
        bookmarks = bookmarks,
    )
}

class StudyStatsCalculatorTest {
    @Test
    fun `숫자 4칸은 지워진 노트도 센다`() {
        val stats =
            compute(
                sessions =
                    listOf(
                        session("hash", "datastructure", "2026-09-29", 12),
                        session("hash", "datastructure", "2026-09-30", 20),
                        session("gone", "cs", "2026-10-01", 9),
                    ),
                attempts = listOf(attempt("room", "android", "2026-09-30", 4, 3)),
            )

        assertEquals(2, stats.current.notes)
        assertEquals(41, stats.current.minutes)
        assertEquals(1, stats.current.quizzes)
        assertEquals(75, stats.current.accuracy)
    }

    @Test
    fun `정답률은 반올림한다 - 2 대 3 은 67`() {
        val stats = compute(attempts = listOf(attempt("room", "android", "2026-09-30", 3, 2)))

        assertEquals(67, stats.current.accuracy)
    }

    @Test
    fun `비교 구간이 없거나 문항이 없으면 정답률은 null`() {
        val firstWeek = compute(firstRecord = "2026-09-29")
        val normal =
            compute(
                sessions = listOf(session("hash", "datastructure", "2026-09-22", 10)),
                attempts = listOf(attempt("room", "android", "2026-09-30", 2, 2)),
            )

        assertNull(firstWeek.previous)
        assertEquals(1, normal.previous?.notes)
        assertNull(normal.previous?.accuracy)
        assertEquals(100, normal.current.accuracy)
    }

    @Test
    fun `지난주 비교는 같은 요일 같은 시각까지만 센다`() {
        val stats =
            compute(
                sessions =
                    listOf(
                        session("hash", "datastructure", "2026-09-24", 10).copy(startedAt = at("2026-09-24", "14:00")),
                        session("room", "android", "2026-09-24", 10).copy(startedAt = at("2026-09-24", "16:00")),
                    ),
            )

        assertEquals(1, stats.previous?.notes)
    }

    @Test
    fun `멈춘 노트는 최신 세션 기준이고 90 경계와 5개 제한과 없는 노트 제외를 지킨다`() {
        val many = (1..7).map { NoteRef("n$it", "노트 $it", "android") }
        val latest =
            listOf(
                session("n1", "android", "2026-09-29", 5, percent = 89),
                session("n2", "android", "2026-09-29", 5, percent = 90),
                session("n3", "android", "2026-09-30", 5, percent = 10),
                session("n4", "android", "2026-09-30", 5, percent = 20),
                session("n5", "android", "2026-10-01", 5, percent = 30),
                session("n6", "android", "2026-10-01", 5, percent = 40),
                session("n7", "android", "2026-09-20", 5, percent = 40),
                session("gone", "android", "2026-10-01", 5, percent = 10),
            )
        val stats = compute(latest = latest, noteRefs = many)

        assertEquals(5, stats.stalled.size)
        assertFalse(stats.stalled.any { it.note.id == "n2" || it.note.id == "n7" })
        assertTrue(stats.stalled.any { it.note.id == "n1" })
    }

    @Test
    fun `재도전은 전 기간 첫 시도와 기간 안 마지막 시도와 순번을 쓴다`() {
        val all =
            listOf(
                attempt("room", "android", "2026-08-10", 3, 1),
                attempt("room", "android", "2026-09-29", 4, 2),
                attempt("room", "android", "2026-09-30", 3, 2),
                attempt("flow", "kotlin", "2026-09-30", 5, 4),
            )
        val stats = compute(attempts = all.drop(1), allAttempts = all)

        assertEquals(1, stats.retries.size)
        val retry = stats.retries.single()
        assertEquals("room", retry.note.id)
        assertEquals(33, retry.firstAccuracy)
        assertEquals(67, retry.lastAccuracy)
        assertEquals(3, retry.ordinal)
        assertTrue(retry.improved)
    }

    @Test
    fun `오래 안 본 북마크의 보조 줄 세 가지`() {
        val stats =
            compute(
                firstRecord = "2026-08-01",
                latest = listOf(session("hash", "datastructure", "2026-09-01", 5)),
                bookmarks =
                    listOf(
                        Bookmark("hash", at("2026-08-20")),
                        Bookmark("room", at("2026-08-05")),
                        Bookmark("flow", at("2026-07-01")),
                        Bookmark("recomp", at("2026-09-25")),
                    ),
            )

        assertEquals(listOf("flow", "room", "hash"), stats.staleBookmarks.map { it.note.id })
        assertEquals(StaleReason.UnreadSinceLog, stats.staleBookmarks[0].reason)
        assertEquals(StaleReason.Unread, stats.staleBookmarks[1].reason)
        assertEquals(StaleReason.LastOpened(30), stats.staleBookmarks[2].reason)
        assertEquals(42, stats.staleBookmarks[2].ageDays)
    }

    @Test
    fun `기록이 없으면 오래 안 본 북마크도 없다`() {
        val stats = compute(firstRecord = null, bookmarks = listOf(Bookmark("hash", at("2026-01-01"))))

        assertTrue(stats.staleBookmarks.isEmpty())
    }

    @Test
    fun `카테고리 축은 현재 노트와 기간 이벤트의 합집합을 이름순으로`() {
        val stats =
            compute(
                sessions = listOf(session("gone", "cs", "2026-09-30", 10), session("x", "", "2026-09-30", 10)),
            )

        assertEquals(listOf("android", "compose", "cs", "datastructure", "kotlin"), stats.axis)
        assertEquals(10, stats.categoryMinutes.first().minutes)
        assertEquals("cs", stats.categoryMinutes.first().category)
        assertEquals(20, stats.current.minutes)
    }

    @Test
    fun `응시 카테고리가 하나면 가장 낮음 강조가 없다`() {
        val single = compute(attempts = listOf(attempt("room", "android", "2026-09-30", 4, 1)))
        val two =
            compute(
                attempts =
                    listOf(
                        attempt("room", "android", "2026-09-30", 4, 1),
                        attempt("flow", "kotlin", "2026-09-30", 4, 3),
                    ),
            )

        assertTrue(single.accuracy.none { it.lowest })
        assertEquals(listOf("android"), two.accuracy.filter { it.lowest }.map { it.category })
        assertEquals("android", two.accuracy.first().category)
        assertNull(two.accuracy.last().accuracy)
    }

    @Test
    fun `북마크 이벤트는 날짜 칸에 들어간다`() {
        val events =
            listOf(
                BookmarkEvent("hash", "datastructure", BookmarkEventKind.Added, at("2026-09-28")),
                BookmarkEvent("room", "android", BookmarkEventKind.Added, at("2026-09-28")),
                BookmarkEvent("room", "android", BookmarkEventKind.Removed, at("2026-09-30")),
            )
        val stats = compute(events = events)

        assertEquals(2, stats.bookmarkBuckets[0].added)
        assertEquals(1, stats.bookmarkBuckets[2].removed)
        assertEquals(2, stats.added)
        assertEquals(1, stats.removed)
    }

    @Test
    fun `요일 칸은 시작 날짜에 귀속된다`() {
        val stats = compute(sessions = listOf(session("hash", "datastructure", "2026-09-29", 35)))

        assertEquals(listOf(0, 35, 0, 0, 0, 0, 0), stats.timeBars.map { it.minutes })
    }

    @Test
    fun `비교 구간이 첫 기록 이전에 걸리면 지난 값을 세지 않는다`() {
        val tuesday = LocalDateTime.parse("2026-10-06T15:00").atZone(ZONE)
        val first = LocalDate.parse("2026-10-02")
        val friday = at("2026-10-02")
        val stats =
            StudyStatsCalculator.compute(
                range = StatsPeriodRange.of(StatsPeriod.Week, tuesday, first),
                now = tuesday,
                firstRecordAt = friday,
                window =
                    StudyWindow(
                        listOf(session("hash", "datastructure", "2026-10-02", 30)),
                        emptyList(),
                        emptyList(),
                    ),
                facts = NoteFacts(emptyList(), emptyList()),
                notes = notes,
                bookmarks = emptyList(),
            )

        assertNull(stats.previous)
        assertEquals(0, stats.current.minutes)
    }

    @Test
    fun `접두가 붙은 기록과 노트로 계산해도 접두 없는 쪽과 결과가 같다`() {
        fun scenario(prefix: String): StudyStats =
            compute(
                sessions =
                    listOf(
                        session("${prefix}hash", "datastructure", "2026-09-29", 12, percent = 40),
                        session("${prefix}room", "android", "2026-09-30", 20),
                    ),
                attempts =
                    listOf(
                        attempt("${prefix}room", "android", "2026-09-30", 4, 3),
                        attempt("${prefix}room", "android", "2026-10-01", 4, 4),
                    ),
                bookmarks = listOf(Bookmark("${prefix}flow", at("2026-07-01"))),
                noteRefs = notes.map { it.copy(id = prefix + it.id) },
            )

        assertEquals(scenario("").toString(), scenario("s0/").toString().replace("s0/", ""))
    }
}
