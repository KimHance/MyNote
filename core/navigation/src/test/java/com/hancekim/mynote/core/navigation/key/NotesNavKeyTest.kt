package com.hancekim.mynote.core.navigation.key

import org.junit.Assert.assertEquals
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
}
