package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun note(vararg blocks: String): String =
    """{"id":"n","title":"t","date":"2026.01.01","blocks":[${blocks.joinToString(",")}]}"""

class MyNoteJsonFormatTest {
    private val format = MyNoteJsonFormat(BlockJson)

    private fun unsupported(block: NoteBlockDto): NoteBlockDto.Unsupported = block as NoteBlockDto.Unsupported

    @Test
    fun `모르는 span style 은 일반 글자로 읽고 블록을 버리지 않는다`() {
        val blocks = format.parse(note("""{"type":"paragraph","spans":[{"text":"x","style":"italic"}]}"""))

        assertEquals(listOf(NoteBlockDto.Paragraph(listOf(NoteSpanDto("x", SpanStyleDto.TEXT)))), blocks)
    }

    @Test
    fun `style 이 null 숫자 불리언이어도 일반 글자다`() {
        val blocks =
            format.parse(
                note(
                    """{"type":"paragraph","spans":[{"text":"a","style":null},{"text":"b","style":3},{"text":"c","style":true}]}""",
                ),
            )

        val spans = (blocks.single() as NoteBlockDto.Paragraph).spans
        assertEquals(listOf("a", "b", "c"), spans.map { it.text })
        assertTrue(spans.all { it.style == SpanStyleDto.TEXT })
    }

    @Test
    fun `style 이 객체나 배열이면 그 블록만 BrokenBlock 이다`() {
        val blocks =
            format.parse(
                note(
                    """{"type":"paragraph","spans":[{"text":"a","style":{}}]}""",
                    """{"type":"paragraph","spans":[{"text":"b"}]}""",
                ),
            )

        assertEquals(UnsupportedReasonDto.BROKEN_BLOCK, unsupported(blocks[0]).reason)
        assertEquals("a", unsupported(blocks[0]).text)
        assertTrue(blocks[1] is NoteBlockDto.Paragraph)
    }

    @Test
    fun `필수 필드가 빠진 블록만 BrokenBlock 이 되고 앞뒤 블록은 남는다`() {
        val blocks =
            format.parse(
                note(
                    """{"type":"paragraph","spans":[{"text":"앞"}]}""",
                    """{"type":"heading","spans":[{"text":"레벨 없음"}]}""",
                    """{"type":"paragraph"}""",
                    """{"type":"code","code":"뒤"}""",
                ),
            )

        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is NoteBlockDto.Paragraph)
        assertEquals(UnsupportedReasonDto.BROKEN_BLOCK, unsupported(blocks[1]).reason)
        assertEquals("heading", unsupported(blocks[1]).original)
        assertEquals(UnsupportedReasonDto.BROKEN_BLOCK, unsupported(blocks[2]).reason)
        assertEquals(NoteBlockDto.Code("뒤"), blocks[3])
    }

    @Test
    fun `envelope 의 id title date 가 없어도 블록을 읽는다`() {
        val blocks = format.parse("""{"blocks":[{"type":"code","code":"x"}]}""")

        assertEquals(listOf(NoteBlockDto.Code("x")), blocks)
    }

    @Test
    fun `모르는 type 은 원래 type 과 원문을 든 UnknownType 이 된다`() {
        val block = unsupported(format.parse(note("""{"type":"timeline","events":[1,2]}""")).single())

        assertEquals(UnsupportedReasonDto.UNKNOWN_TYPE, block.reason)
        assertEquals("timeline", block.original)
        assertTrue(block.raw.contains("\"timeline\""))
        assertTrue(block.raw.contains("\"events\""))
    }

    @Test
    fun `JSON 이 아니면 노트 전체가 원문을 든 BrokenNote 하나가 된다`() {
        listOf("not json", """{"blocks":{}}""", """[1,2]""", """{"id":"n"}""", """{"blocks":[],}""").forEach { raw ->
            val block = unsupported(format.parse(raw).single())

            assertEquals(raw, UnsupportedReasonDto.BROKEN_NOTE, block.reason)
            assertEquals(raw, block.raw)
            assertNull(block.text)
        }
    }

    @Test
    fun `원격의 type unsupported 와 unknown 은 내부 블록으로 해석하지 않는다`() {
        val blocks =
            format.parse(
                note(
                    """{"type":"unsupported","reason":"broken_note","raw":"x"}""",
                    """{"type":"unknown"}""",
                ),
            )

        assertEquals(listOf("unsupported", "unknown"), blocks.map { unsupported(it).original })
        assertTrue(blocks.all { unsupported(it).reason == UnsupportedReasonDto.UNKNOWN_TYPE })
    }

    @Test
    fun `fallback 문자열은 Unsupported 의 text 가 된다`() {
        val block = unsupported(format.parse(note("""{"type":"table","fallback":"| a | b |"}""")).single())

        assertEquals("| a | b |", block.text)
    }

    @Test
    fun `type 이 문자열이 아니거나 블록이 객체가 아니면 BrokenBlock 이다`() {
        val blocks = format.parse(note("""{"type":3}""", """{"spans":[]}""", "1", "\"s\"", "null", "[]"))

        assertEquals(6, blocks.size)
        blocks.forEach { block ->
            assertEquals(UnsupportedReasonDto.BROKEN_BLOCK, unsupported(block).reason)
            assertNull(unsupported(block).original)
        }
    }

    @Test
    fun `건진 글자는 fallback 이 먼저고 없으면 spans code items 의 글자다`() {
        val blocks =
            format.parse(
                note(
                    """{"type":"timeline","items":[{"spans":[{"text":"a"}]}]}""",
                    """{"type":"heading","level":2}""",
                    """{"type":"callout","fallback":"x","spans":[{"text":"y"}]}""",
                    """{"type":"code"}""",
                    """{"type":"box","spans":[{"text":"가"},{"text":"나"}],"items":[{"spans":[{"text":"1"}]},{"spans":[{"text":"2"}]}]}""",
                ),
            )

        assertEquals(listOf("a", null, "x", null, "가나\n1\n2"), blocks.map { unsupported(it).text })
    }

    @Test
    fun `blocks 가 빈 배열이면 본문이 빈 정상 노트다`() {
        assertTrue(format.parse(note()).isEmpty())
    }

    @Test
    fun `블록 원문은 두 칸 들여쓰기로 다시 쓴다`() {
        val block = unsupported(format.parse(note("""{"type":"timeline","n":1}""")).single())

        assertEquals("{\n  \"type\": \"timeline\",\n  \"n\": 1\n}", block.raw)
    }

    @Test
    fun `캐시 인코딩은 Unsupported 를 포함해 라운드트립된다`() {
        val blocks = format.parse(note("""{"type":"paragraph","spans":[{"text":"x"}]}""", """{"type":"timeline"}"""))

        assertEquals(blocks, BlockCache.decode(BlockCache.encode(blocks)))
    }

    @Test
    fun `옛 캐시의 unknown 블록은 원문 없는 UnknownType 이 된다`() {
        val block = BlockCache.decode("""[{"type":"unknown"}]""").single().toDomain()

        assertEquals(NoteBlock.Unsupported(UnsupportedReason.UnknownType, type = null, text = null, raw = ""), block)
    }

    @Test
    fun `WIRE_TYPES 는 캐시 전용 둘을 뺀 sealed 하위 타입 전부다`() {
        assertEquals(
            setOf("heading", "paragraph", "code", "quote", "bulletList", "orderedList", "divider", "spacer"),
            WIRE_TYPES,
        )
    }
}
