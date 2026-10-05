package com.hancekim.mynote.core.navigation.key

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NotesNavKeyTest {
    @Test
    fun `홈만 있으면 열린 노트가 없다`() {
        assertNull(listOf(NotesNavKey.Home).activeNoteId())
    }

    @Test
    fun `맨 위가 리더면 그 노트가 열린 노트다`() {
        val stack = listOf(NotesNavKey.Home, NotesNavKey.Reader("compose-state"))
        assertEquals("compose-state", stack.activeNoteId())
    }

    @Test
    fun `리더가 맨 위가 아니면 열린 노트가 아니다`() {
        val stack = listOf(NotesNavKey.Reader("a"), NotesNavKey.Home)
        assertNull(stack.activeNoteId())
    }

    @Test
    fun `맨 위가 북마크 화면이면 열린 노트가 없고 설정도 아니다`() {
        val stack = listOf(NotesNavKey.Home, NotesNavKey.Bookmarks)
        assertNull(stack.activeNoteId())
        assertFalse(stack.isSettingsTop())
    }

    @Test
    fun `맨 위가 통계 화면이면 열린 노트가 없고 설정도 아니다`() {
        val stack = listOf(NotesNavKey.Home, NotesNavKey.Stats)
        assertNull(stack.activeNoteId())
        assertFalse(stack.isSettingsTop())
    }

    @Test
    fun `통계 위의 리더는 열린 노트다`() {
        val stack = listOf(NotesNavKey.Home, NotesNavKey.Stats, NotesNavKey.Reader("hash-map"))
        assertEquals("hash-map", stack.activeNoteId())
    }

    @Test
    fun `맨 위가 지난 요약이면 열린 노트가 없고 설정도 아니다`() {
        val stack = listOf(NotesNavKey.Home, NotesNavKey.Stats, NotesNavKey.StatsHistory)
        assertNull(stack.activeNoteId())
        assertFalse(stack.isSettingsTop())
    }

    @Test
    fun `맨 위가 연결 화면이면 열린 노트가 없고 설정도 아니다`() {
        val stack = listOf(NotesNavKey.Home, NotesNavKey.Settings, NotesNavKey.Connect)
        assertNull(stack.activeNoteId())
        assertFalse(stack.isSettingsTop())
    }
}
