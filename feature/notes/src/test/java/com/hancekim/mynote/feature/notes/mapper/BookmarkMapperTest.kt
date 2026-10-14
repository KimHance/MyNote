package com.hancekim.mynote.feature.notes.mapper

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

private val categories =
    listOf(
        CategoryUi(
            "CS",
            persistentListOf(
                NoteUi("tcp", "TCP 3-way handshake", "2026.04.08"),
                NoteUi("http", "HTTP 캐싱 기초", "2026.04.22"),
            ),
        ),
        CategoryUi("Android", persistentListOf(NoteUi("compose", "Compose 상태 관리", "2026.06.12"))),
    )

class BookmarkMapperTest {
    @Test
    fun `최근에 북마크한 것이 위로 오고 노트 날짜는 보지 않는다`() {
        val result =
            bookmarkedNotes(
                categories,
                listOf(Bookmark("compose", 10L), Bookmark("tcp", 30L), Bookmark("http", 20L)),
            )

        assertEquals(listOf("tcp", "http", "compose"), result.map { it.note.id })
        assertEquals("CS", result.first().categoryName)
    }

    @Test
    fun `각 항목이 자기 addedAt 을 싣는다`() {
        val result = bookmarkedNotes(categories, listOf(Bookmark("compose", 10L), Bookmark("tcp", 30L)))

        assertEquals(listOf(30L, 10L), result.map { it.addedAt })
    }

    @Test
    fun `addedAt 이 같으면 noteId 오름차순으로 고정한다`() {
        val result =
            bookmarkedNotes(categories, listOf(Bookmark("tcp", 5L), Bookmark("compose", 5L), Bookmark("http", 5L)))

        assertEquals(listOf("compose", "http", "tcp"), result.map { it.note.id })
    }

    @Test
    fun `LN-11 노트 없는 북마크 키는 보이지 않는다`() {
        val result = bookmarkedNotes(categories, listOf(Bookmark("ghost", 99L), Bookmark("tcp", 1L)))

        assertEquals(listOf("tcp"), result.map { it.note.id })
    }

    @Test
    fun `북마크가 없으면 빈 목록이다`() {
        assertEquals(0, bookmarkedNotes(categories, emptyList()).size)
    }
}
