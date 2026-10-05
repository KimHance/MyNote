package com.hancekim.mynote.feature.notes.screen.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val zone: ZoneId = ZoneId.of("Asia/Seoul")
private val today: LocalDate = LocalDate.of(2026, 10, 1)

private fun at(
    date: LocalDate,
    hour: Int,
    minute: Int,
): Long =
    LocalDateTime
        .of(date, java.time.LocalTime.of(hour, minute))
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

class BookmarkTimeKindTest {
    @Test
    fun `같은 날이면 오늘이다`() {
        assertEquals(BookmarkTimeKind.Today, BookmarkTimeKind.of(at(today, 0, 0), today, zone))
        assertEquals(BookmarkTimeKind.Today, BookmarkTimeKind.of(at(today, 23, 59), today, zone))
    }

    @Test
    fun `하루 전이면 어제다`() {
        assertEquals(BookmarkTimeKind.Yesterday, BookmarkTimeKind.of(at(today.minusDays(1), 22, 5), today, zone))
    }

    @Test
    fun `이틀 전부터 올해 안이면 올해다`() {
        assertEquals(BookmarkTimeKind.ThisYear, BookmarkTimeKind.of(at(today.minusDays(2), 9, 0), today, zone))
        assertEquals(BookmarkTimeKind.ThisYear, BookmarkTimeKind.of(at(LocalDate.of(2026, 1, 1), 9, 0), today, zone))
    }

    @Test
    fun `해가 다르면 다른 해다`() {
        assertEquals(BookmarkTimeKind.OtherYear, BookmarkTimeKind.of(at(LocalDate.of(2025, 12, 31), 9, 0), today, zone))
    }

    @Test
    fun `연초의 어제는 해가 달라도 어제다`() {
        val newYear = LocalDate.of(2026, 1, 1)
        assertEquals(
            BookmarkTimeKind.Yesterday,
            BookmarkTimeKind.of(at(LocalDate.of(2025, 12, 31), 9, 0), newYear, zone),
        )
    }

    @Test
    fun `판정은 기기 시간대를 따른다`() {
        val lateNightSeoul = at(today, 0, 30)
        val utc = ZoneId.of("UTC")
        assertEquals(BookmarkTimeKind.Today, BookmarkTimeKind.of(lateNightSeoul, today, zone))
        assertEquals(BookmarkTimeKind.Yesterday, BookmarkTimeKind.of(lateNightSeoul, today, utc))
    }
}
