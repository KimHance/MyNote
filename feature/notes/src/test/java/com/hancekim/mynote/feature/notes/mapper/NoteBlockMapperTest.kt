package com.hancekim.mynote.feature.notes.mapper

import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import com.hancekim.mynote.core.designsystem.block.UnsupportedKind
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBlockMapperTest {
    private fun unsupported(
        reason: UnsupportedReason = UnsupportedReason.UnknownType,
        text: String? = "건진 글자",
        raw: String = "{\n  \"type\": \"timeline\"\n}",
    ) = NoteBlock.Unsupported(reason = reason, type = "timeline", text = text, raw = raw)

    @Test
    fun `이유 세 가지가 같은 이름의 UI 종류로 간다`() {
        val kinds =
            listOf(UnsupportedReason.UnknownType, UnsupportedReason.BrokenBlock, UnsupportedReason.BrokenNote)
                .map { reason ->
                    (
                        listOf(
                            unsupported(reason = reason),
                        ).toContentBlocks().single() as NoteContentBlock.Unsupported
                    ).kind
                }

        assertEquals(
            listOf(UnsupportedKind.UnknownType, UnsupportedKind.BrokenBlock, UnsupportedKind.BrokenNote),
            kinds,
        )
    }

    @Test
    fun `건진 글자와 원문은 그대로 옮긴다`() {
        val block = listOf(unsupported()).toContentBlocks().single() as NoteContentBlock.Unsupported

        assertEquals("건진 글자", block.salvaged)
        assertEquals("{\n  \"type\": \"timeline\"\n}", block.raw)
    }

    @Test
    fun `원문이 비어 있으면 null 이라 원문 보기 버튼이 없다`() {
        val block = listOf(unsupported(raw = "")).toContentBlocks().single() as NoteContentBlock.Unsupported

        assertNull(block.raw)
    }

    @Test
    fun `건진 글자가 없거나 공백뿐이면 null 이다`() {
        val missing = listOf(unsupported(text = null)).toContentBlocks().single() as NoteContentBlock.Unsupported
        val blank = listOf(unsupported(text = "  \n")).toContentBlocks().single() as NoteContentBlock.Unsupported

        assertNull(missing.salvaged)
        assertNull(blank.salvaged)
    }

    @Test
    fun `상자는 원래 블록 자리에 남고 앞뒤 블록 순서가 그대로다`() {
        val blocks =
            listOf(
                NoteBlock.Paragraph(listOf(NoteSpan("앞"))),
                unsupported(),
                NoteBlock.Code("뒤"),
            ).toContentBlocks()

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is NoteContentBlock.Paragraph)
        assertTrue(blocks[1] is NoteContentBlock.Unsupported)
        assertEquals(NoteContentBlock.Code("뒤"), blocks[2])
    }
}
