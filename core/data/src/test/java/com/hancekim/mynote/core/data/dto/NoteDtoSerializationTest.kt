package com.hancekim.mynote.core.data.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteDtoSerializationTest {
    private val json = NotesJson

    @Test
    fun `블록 배열을 type discriminator 로 polymorphic 역직렬화한다`() {
        val text =
            """
            [
              {"type":"heading","level":2,"spans":[{"text":"제목"}]},
              {"type":"paragraph","spans":[{"text":"본문 "},{"text":"굵게","style":"bold"}]},
              {"type":"code","code":"val x = 1"},
              {"type":"bulletList","items":[{"spans":[{"text":"a"}]}]}
            ]
            """.trimIndent()

        val blocks = json.decodeFromString<List<NoteBlockDto>>(text)

        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is NoteBlockDto.Heading)
        assertEquals(2, (blocks[0] as NoteBlockDto.Heading).level)
        val paragraph = blocks[1] as NoteBlockDto.Paragraph
        assertEquals(SpanStyleDto.BOLD, paragraph.spans[1].style)
        assertTrue(blocks[2] is NoteBlockDto.Code)
        assertTrue(blocks[3] is NoteBlockDto.BulletList)
    }

    @Test
    fun `style 을 생략하면 TEXT 로 역직렬화된다`() {
        val span = json.decodeFromString<NoteSpanDto>("""{"text":"hi"}""")
        assertEquals(SpanStyleDto.TEXT, span.style)
    }

    @Test
    fun `divider 와 spacer 는 type 만으로 라운드트립된다`() {
        val text = """[{"type":"divider"},{"type":"spacer"}]"""
        val blocks = json.decodeFromString<List<NoteBlockDto>>(text)
        assertTrue(blocks[0] is NoteBlockDto.Divider)
        assertTrue(blocks[1] is NoteBlockDto.Spacer)
        val reDecoded = json.decodeFromString<List<NoteBlockDto>>(json.encodeToString(blocks))
        assertEquals(blocks, reDecoded)
    }

    @Test
    fun `모르는 블록 타입이 와도 던지지 않고 Unknown 으로 읽는다`() {
        val text = """[{"type":"paragraph","spans":[{"text":"본문"}]},{"type":"timeline","events":[1,2]}]"""

        val blocks = json.decodeFromString<List<NoteBlockDto>>(text)

        assertEquals(2, blocks.size)
        assertTrue(blocks[1] is NoteBlockDto.Unknown)
    }

    @Test
    fun `모르는 블록은 도메인으로 갈 때 빠진다`() {
        val text =
            """
            {"id":"n","title":"t","date":"2026.01.01","blocks":[
              {"type":"paragraph","spans":[{"text":"본문"}]},
              {"type":"timeline","events":[1,2]}
            ]}
            """.trimIndent()

        val note = json.decodeFromString<NoteDto>(text).toDomain()

        assertEquals(1, note.blocks.size)
    }

    @Test
    fun `모르는 블록 하나가 노트 전체를 못 쓰게 만들지 않는다`() {
        val text = """{"id":"n","title":"t","date":"2026.01.01","blocks":[{"type":"timeline"}]}"""

        val note = json.decodeFromString<NoteDto>(text).toDomain()

        assertEquals("t", note.title)
        assertTrue(note.blocks.isEmpty())
    }
}
