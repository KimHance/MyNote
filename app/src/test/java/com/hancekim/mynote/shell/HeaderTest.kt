package com.hancekim.mynote.shell

import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.key.activeNoteId
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeaderTest {
    private fun headerAt(vararg stack: NotesNavKey): HeaderUi {
        val keys = persistentListOf(*stack)
        return headerOf(FeatureStates(shell = ShellUiState(backStack = keys, activeNoteId = keys.activeNoteId())))
    }

    @Test
    fun `LN-9 저장소 노트에는 고치기와 지우기가 없다`() {
        val header = headerAt(NotesNavKey.Home, NotesNavKey.Reader("s1/notes/android/a.md"))

        assertNull(header.deviceNoteId)
        assertTrue("저장소 노트 리더는 새로고침 그대로", header.showRefresh)
    }

    @Test
    fun `LN-9 기기 노트 리더는 새로고침 자리에 고치기와 지우기다`() {
        val header = headerAt(NotesNavKey.Home, NotesNavKey.Reader("local/abc.md"))

        assertEquals("local/abc.md", header.deviceNoteId)
        assertFalse(header.showRefresh)
    }

    @Test
    fun `LN-3 편집기와 붙여넣기는 셸 상단 바를 비운다`() {
        assertEquals(HeaderPlace.OwnChrome, headerAt(NotesNavKey.Home, NotesNavKey.Editor()).place)
        assertEquals(HeaderPlace.OwnChrome, headerAt(NotesNavKey.Home, NotesNavKey.Paste).place)
        assertEquals(HeaderPlace.OwnChrome, headerAt(NotesNavKey.Home, NotesNavKey.Connect).place)
        assertEquals(HeaderPlace.Other, headerAt(NotesNavKey.Home).place)
    }

    @Test
    fun `LN-3 쌓은 편집기와 붙여넣기는 셸 크롬을 숨긴다`() {
        assertTrue(
            ShellUiState(
                backStack = persistentListOf(NotesNavKey.Home, NotesNavKey.Editor(noteId = "local/a.md")),
            ).ownsChrome,
        )
        assertTrue(ShellUiState(backStack = persistentListOf(NotesNavKey.Home, NotesNavKey.Paste)).ownsChrome)
        assertFalse(
            ShellUiState(backStack = persistentListOf(NotesNavKey.Home, NotesNavKey.Reader("local/a.md"))).ownsChrome,
        )
        // 맨 위로 버튼도 같은 판정을 쓴다
        assertFalse(scrollToTopVisible(scrolled = true, quizOpen = false, connectOpen = true, tokenEditing = false))
    }
}
