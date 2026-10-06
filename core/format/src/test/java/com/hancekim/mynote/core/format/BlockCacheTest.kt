package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.TableAlign
import com.hancekim.mynote.core.domain.model.TableCell
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun spans(text: String) = listOf(NoteSpan(text))

private fun cell(text: String) = TableCell(spans(text))

private fun converting(blocks: List<NoteBlock>) = NoteFormat { blocks }

private fun cacheOf(vararg blocks: NoteBlock): String = converting(blocks.toList()).derive("x.md", "r").blocksJson

private fun roundTrip(vararg blocks: NoteBlock): List<NoteBlock> = NoteFormat().read(cacheOf(*blocks), "x.md", null)

private fun nested(depth: Int): NoteBlock =
    (depth - 1 downTo 1).fold(NoteBlock.BulletList(listOf(NoteListItem(spans("$depth"))))) { inner, level ->
        NoteBlock.BulletList(listOf(NoteListItem(spans("$level"), children = listOf(inner))))
    }

private fun depthOf(block: NoteBlock): Int {
    val items =
        when (block) {
            is NoteBlock.BulletList -> block.items
            is NoteBlock.OrderedList -> block.items
            else -> emptyList()
        }
    return 1 + (items.flatMap { it.children }.maxOfOrNull(::depthOf) ?: 0)
}

class BlockCacheTest {
    @Test
    fun `NF-12 지금 블록은 변환기에서 와도 캐시 바이트가 JSON 과 같다`() {
        val wire =
            """
            {"blocks":[
              {"type":"heading","level":2,"spans":[{"text":"제목"}]},
              {"type":"paragraph","spans":[{"text":"a"},{"text":"b","style":"bold"},{"text":"c","style":"code"}]},
              {"type":"code","code":"x"},
              {"type":"quote","spans":[{"text":"q"}]},
              {"type":"bulletList","items":[{"spans":[{"text":"a"}]}]},
              {"type":"orderedList","items":[{"spans":[{"text":"1"}]}]},
              {"type":"divider"},
              {"type":"spacer"}
            ]}
            """.trimIndent()
        val blocks =
            listOf(
                NoteBlock.Heading(2, spans("제목")),
                NoteBlock.Paragraph(
                    listOf(NoteSpan("a"), NoteSpan("b", SpanStyle.Bold), NoteSpan("c", SpanStyle.Code)),
                ),
                NoteBlock.Code("x"),
                NoteBlock.Quote(spans("q")),
                NoteBlock.BulletList(listOf(NoteListItem(spans("a")))),
                NoteBlock.OrderedList(listOf(NoteListItem(spans("1")))),
                NoteBlock.Divider,
                NoteBlock.Spacer,
            )
        val expected =
            """[{"type":"heading","level":2,"spans":[{"text":"제목"}]},""" +
                """{"type":"paragraph","spans":[{"text":"a"},{"text":"b","style":"bold"},""" +
                """{"text":"c","style":"code"}]},""" +
                """{"type":"code","code":"x"},{"type":"quote","spans":[{"text":"q"}]},""" +
                """{"type":"bulletList","items":[{"spans":[{"text":"a"}]}]},""" +
                """{"type":"orderedList","items":[{"spans":[{"text":"1"}]}]},{"type":"divider"},{"type":"spacer"}]"""

        assertEquals(expected, NoteFormat().derive("notes/x.json", wire).blocksJson)
        assertEquals(expected, converting(blocks).derive("notes/x.md", "r").blocksJson)
    }

    @Test
    fun `NF-12 JSON 원문의 취소선과 체크 필드도 지금 판과 같게 읽힌다`() {
        val raw =
            """{"blocks":[{"type":"bulletList","items":[{"spans":[{"text":"x","strike":true}],"checked":true}]}]}"""

        assertEquals(
            listOf(NoteBlock.BulletList(listOf(NoteListItem(spans("x"))))),
            NoteFormat().parse("notes/x.json", raw),
        )
    }

    @Test
    fun `NF-4 겹친 모양이 캐시를 왕복한다`() {
        val paragraph =
            NoteBlock.Paragraph(
                listOf(
                    NoteSpan("a", SpanStyle.Bold, italic = true),
                    NoteSpan("b", SpanStyle.Bold, strike = true),
                    NoteSpan("c", SpanStyle.Code, strike = true),
                    NoteSpan("d", italic = true, strike = true),
                    NoteSpan("e"),
                ),
            )
        val quote = NoteBlock.Quote(listOf(NoteSpan("q "), NoteSpan("i", italic = true)))

        assertEquals(listOf(paragraph, quote), roundTrip(paragraph, quote))
        assertEquals(
            """[{"type":"paragraph","spans":[{"text":"a","style":"bold","italic":true},""" +
                """{"text":"b","style":"bold","strike":true},{"text":"c","style":"code","strike":true},""" +
                """{"text":"d","italic":true,"strike":true},{"text":"e"}]}]""",
            cacheOf(paragraph),
        )
    }

    @Test
    fun `NF-5 표가 캐시를 왕복한다`() {
        val table =
            NoteBlock.Table(
                header = listOf(cell("h1"), TableCell(listOf(NoteSpan("h2", SpanStyle.Bold))), cell("h3")),
                rows = listOf(listOf(cell("a"), cell("b|c"), cell("")), listOf(cell("d"), cell("e"), cell("f"))),
                align = listOf(TableAlign.Left, TableAlign.Center, TableAlign.Right),
            )
        val headOnly = NoteBlock.Table(listOf(cell("h")), emptyList(), listOf(TableAlign.None))

        assertEquals(listOf(table, headOnly), roundTrip(table, headOnly))
        assertEquals(
            """[{"type":"table","header":[{"spans":[{"text":"h"}]}],"rows":[],"align":["none"]}]""",
            cacheOf(headOnly),
        )
    }

    @Test
    fun `NF-6 하위 블록과 체크와 시작 번호가 캐시를 왕복한다`() {
        val list =
            NoteBlock.OrderedList(
                items =
                    listOf(
                        NoteListItem(
                            spans("a"),
                            checked = true,
                            children =
                                listOf(
                                    NoteBlock.BulletList(listOf(NoteListItem(spans("b"), checked = false))),
                                    NoteBlock.Paragraph(spans("p")),
                                    NoteBlock.Code("x"),
                                    NoteBlock.Quote(spans("q")),
                                    NoteBlock.Table(
                                        listOf(cell("h")),
                                        listOf(listOf(cell("v"))),
                                        listOf(TableAlign.None),
                                    ),
                                ),
                        ),
                        NoteListItem(emptyList(), children = listOf(NoteBlock.Heading(1, spans("h")))),
                        NoteListItem(emptyList()),
                    ),
                start = 9,
            )

        assertEquals(listOf(list), roundTrip(list))
        assertEquals(
            """[{"type":"orderedList","items":[{"spans":[{"text":"a"}],"checked":true,""" +
                """"children":[{"type":"code","code":"x"}]}],"start":3}]""",
            cacheOf(
                NoteBlock.OrderedList(listOf(NoteListItem(spans("a"), true, listOf(NoteBlock.Code("x")))), start = 3),
            ),
        )
        assertEquals(
            listOf(
                NoteBlock.BulletList(
                    listOf(
                        NoteListItem(
                            spans("a"),
                            children = listOf(NoteBlock.Unsupported(UnsupportedReason.UnknownType, null, null, "")),
                        ),
                    ),
                ),
            ),
            NoteFormat().read(
                """[{"type":"bulletList","items":[{"spans":[{"text":"a"}],"children":[{"type":"later"}]}]}]""",
                "x.md",
                null,
            ),
        )
    }

    @Test
    fun `NF-6 32단 하위 블록은 캐시 왕복을 지난다`() {
        val deep = nested(32)

        assertEquals(32, depthOf(deep))
        assertEquals(listOf(deep), roundTrip(deep))
    }

    @Test
    fun `NF-8 한도 넘음은 도메인의 한도 넘음 종류로 읽힌다`() {
        val raw = "> ".repeat(33) + "x"
        val box = listOf(NoteBlock.Unsupported(UnsupportedReason.LimitExceeded, type = null, text = null, raw = raw))
        val format = NoteFormat()

        assertEquals(box, format.parse("x.md", raw))
        assertEquals(box, format.read(format.derive("x.md", raw).blocksJson, "x.md", raw))
        assertEquals(
            """[{"type":"unsupported","reason":"limit_exceeded","raw":""}]""",
            cacheOf(NoteBlock.Unsupported(UnsupportedReason.LimitExceeded, type = null, text = null, raw = "r")),
        )
    }

    @Test
    fun `NF-8 거르기는 도메인 한도 값을 쓴다`() {
        val small = NoteLimits(maxDepth = 2, maxKilobytes = 1)

        assertFalse(exceedsMarkdownLimits("> - x", small))
        assertTrue(exceedsMarkdownLimits("> - > x", small))
        assertFalse(exceedsMarkdownLimits("a".repeat(1_024), small))
        assertTrue(exceedsMarkdownLimits("a".repeat(1_025), small))
    }
}
