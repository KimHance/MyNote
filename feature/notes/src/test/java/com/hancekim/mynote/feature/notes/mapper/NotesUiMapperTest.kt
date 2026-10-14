package com.hancekim.mynote.feature.notes.mapper

import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesUiMapperTest {
    private fun note(
        vararg blocks: NoteBlock,
    ) = Note(id = "s0/tcp", title = "TCP", date = "2026.04.08", blocks = blocks.toList())

    private fun unsupported(reason: UnsupportedReason) =
        NoteBlock.Unsupported(
            reason = reason,
            type = null,
            text = null,
            raw = "{}",
        )

    @Test
    fun `LN-5 출처 표시는 기기 노트에만 있다`() {
        val device = Note(id = "local/7f3a", title = "코루틴", date = "2026.10.14", blocks = emptyList()).toUi()
        val legacy = note().toUi()
        val connected = Note(id = "2f1c/android/flow", title = "Flow", date = "2026.10.01", blocks = emptyList()).toUi()

        assertTrue(device.device)
        assertFalse(legacy.device)
        assertFalse(connected.device)
        assertEquals(listOf(true, false, false), listOf(device, legacy, connected).map { it.device })
    }

    @Test
    fun `노트 전체를 읽지 못한 노트는 읽을 수 없다`() {
        val ui = note(unsupported(UnsupportedReason.BrokenNote)).toUi()

        assertFalse(ui.readable)
    }

    @Test
    fun `블록 몇 개만 그리지 못한 노트는 읽을 수 있다`() {
        val ui =
            note(
                NoteBlock.Paragraph(listOf(NoteSpan("본문"))),
                unsupported(UnsupportedReason.UnknownType),
                unsupported(UnsupportedReason.BrokenBlock),
            ).toUi()

        assertTrue(ui.readable)
    }

    @Test
    fun `노트 전체를 읽지 못한 노트는 발췌가 없다`() {
        val ui = note(unsupported(UnsupportedReason.BrokenNote)).toUi()

        assertNull(ui.excerpt)
    }

    @Test
    fun `NF-8 한도 넘음 노트는 퀴즈 카드를 숨긴다`() {
        val ui = note(unsupported(UnsupportedReason.LimitExceeded)).toUi()

        assertFalse(ui.readable)
        assertNull(ui.excerpt)
    }
}
