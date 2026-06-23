package com.hancekim.mynote.core.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteDtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `블록 배열을 type discriminator 로 polymorphic 역직렬화한다`() {
        val text = """
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
}
