package com.hancekim.mynote.core.designsystem.block

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteListMarkerTest {
    @Test
    fun `NF-6 글머리는 실제 깊이로 돈다`() {
        assertEquals(listOf("•", "◦", "▪", "•", "◦", "▪"), (1..6).map { bulletGlyph(it) })
    }

    @Test
    fun `NF-6 보이는 들여쓰기는 3단에서 멈춘다`() {
        assertFalse(stopsIndent(1))
        assertFalse(stopsIndent(3))
        assertTrue(stopsIndent(4))
        assertTrue(stopsIndent(32))
    }
}
