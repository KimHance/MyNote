package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import org.junit.Assert.assertEquals
import org.junit.Test

private val ALL_KINDS =
    """
    {"blocks":[
      {"type":"heading","level":2,"spans":[{"text":"제목"}]},
      {"type":"paragraph","spans":[{"text":"본문 "},{"text":"굵게","style":"bold"},{"text":" 와 ","style":"text"},{"text":"code","style":"code"},{"text":"\"q\" <a> & \\ é"}]},
      {"type":"code","code":"val x = 1\n  y"},
      {"type":"quote","spans":[{"text":"인용"}]},
      {"type":"bulletList","items":[{"spans":[{"text":"a"}]},{"spans":[{"text":"b","style":"bold"}]}]},
      {"type":"orderedList","items":[{"spans":[{"text":"1"}]}]},
      {"type":"divider"},
      {"type":"spacer"},
      {"type":"timeline","fallback":"f"},
      {"type":"heading","spans":[]},
      3
    ]}
    """.trimIndent()

private const val ALL_KINDS_CACHE_V1 =
    """[{"type":"heading","level":2,"spans":[{"text":"제목"}]},{"type":"paragraph","spans":[{"text":"본문 "},{"text":"굵게","style":"bold"},{"text":" 와 "},{"text":"code","style":"code"},{"text":"\"q\" <a> & \\ é"}]},{"type":"code","code":"val x = 1\n  y"},{"type":"quote","spans":[{"text":"인용"}]},{"type":"bulletList","items":[{"spans":[{"text":"a"}]},{"spans":[{"text":"b","style":"bold"}]}]},{"type":"orderedList","items":[{"spans":[{"text":"1"}]}]},{"type":"divider"},{"type":"spacer"},{"type":"unsupported","reason":"unknown_type","original":"timeline","text":"f","raw":"{\n  \"type\": \"timeline\",\n  \"fallback\": \"f\"\n}"},{"type":"unsupported","original":"heading","raw":"{\n  \"type\": \"heading\",\n  \"spans\": []\n}"},{"type":"unsupported","raw":"3"}]"""

private val ALL_KINDS_BLOCKS =
    listOf(
        NoteBlock.Heading(2, listOf(NoteSpan("제목"))),
        NoteBlock.Paragraph(
            listOf(
                NoteSpan("본문 "),
                NoteSpan("굵게", SpanStyle.Bold),
                NoteSpan(" 와 "),
                NoteSpan("code", SpanStyle.Code),
                NoteSpan("\"q\" <a> & \\ é"),
            ),
        ),
        NoteBlock.Code("val x = 1\n  y"),
        NoteBlock.Quote(listOf(NoteSpan("인용"))),
        NoteBlock.BulletList(
            listOf(NoteListItem(listOf(NoteSpan("a"))), NoteListItem(listOf(NoteSpan("b", SpanStyle.Bold)))),
        ),
        NoteBlock.OrderedList(listOf(NoteListItem(listOf(NoteSpan("1"))))),
        NoteBlock.Divider,
        NoteBlock.Spacer,
        NoteBlock.Unsupported(
            UnsupportedReason.UnknownType,
            "timeline",
            "f",
            pretty(
                "type" to "\"timeline\"",
                "fallback" to "\"f\"",
            ),
        ),
        NoteBlock.Unsupported(
            UnsupportedReason.BrokenBlock,
            "heading",
            null,
            pretty(
                "type" to "\"heading\"",
                "spans" to "[]",
            ),
        ),
        NoteBlock.Unsupported(UnsupportedReason.BrokenBlock, null, null, "3"),
    )

private fun pretty(vararg fields: Pair<String, String>): String =
    fields.joinToString(",\n", "{\n", "\n}") { (key, value) -> "  \"$key\": $value" }

private fun note(block: String): String = """{"blocks":[$block]}"""

class CacheCompatTest {
    private val format = NoteFormat()
    private val json = MyNoteJsonFormat(BlockJson)

    @Test
    fun `NF-12 JSON 원문의 형식 1 밖 type 과 필드는 지금 판과 같게 읽힌다`() {
        val plainX = listOf(NoteSpanDto("x"))
        val cases =
            listOf(
                """{"type":"paragraph","spans":[{"text":"x","italic":true}]}""" to NoteBlockDto.Paragraph(plainX),
                """{"type":"paragraph","spans":[{"text":"x","italic":"yes"}]}""" to NoteBlockDto.Paragraph(plainX),
                """{"type":"bulletList","items":[{"spans":[{"text":"x"}],"children":3}]}""" to
                    NoteBlockDto.BulletList(listOf(NoteListItemDto(plainX))),
                """{"type":"orderedList","start":7,"items":[{"spans":[{"text":"x"}]}]}""" to
                    NoteBlockDto.OrderedList(listOf(NoteListItemDto(plainX))),
            )
        cases.forEach { (block, expected) ->
            assertEquals(block, listOf(expected), json.parse(note(block)))
            assertEquals(block, listOf(expected), format.blocks("notes/x.json", note(block)))
        }

        val table = """{"type":"table","header":[],"rows":[],"align":[]}"""
        val box = json.parse(note(table)).single() as NoteBlockDto.Unsupported
        assertEquals(UnsupportedReasonDto.UNKNOWN_TYPE, box.reason)
        assertEquals("table", box.original)
        assertEquals(listOf(box), format.blocks(null, note(table)))
    }

    @Test
    fun `NF-12 지금 블록의 캐시 바이트가 그대로다`() {
        assertEquals(ALL_KINDS_CACHE_V1, format.derive("notes/x.json", ALL_KINDS).blocksJson)
        assertEquals(ALL_KINDS_CACHE_V1, format.derive(null, ALL_KINDS).blocksJson)
        assertEquals("본문 굵게 와 code\"q\" <a> & \\ é", format.derive(null, ALL_KINDS).excerpt)
        assertEquals(
            """[{"type":"unsupported","reason":"broken_note","raw":"not json"}]""",
            format.derive("notes/x.json", "not json").blocksJson,
        )
    }

    @Test
    fun `NF-12 옛 캐시 행을 그대로 읽는다`() {
        assertEquals(ALL_KINDS_BLOCKS, format.read(ALL_KINDS_CACHE_V1, null, null))
        assertEquals(
            listOf(
                NoteBlock.Unsupported(UnsupportedReason.UnknownType, type = null, text = null, raw = ""),
                NoteBlock.Unsupported(UnsupportedReason.BrokenBlock, type = null, text = null, raw = ""),
                NoteBlock.Paragraph(listOf(NoteSpan("x"))),
            ),
            format.read(
                """[{"type":"unknown"},{"type":"unsupported","raw":""},{"type":"paragraph","spans":[{"text":"x","style":"zzz"}]}]""",
                null,
                null,
            ),
        )
    }
}
