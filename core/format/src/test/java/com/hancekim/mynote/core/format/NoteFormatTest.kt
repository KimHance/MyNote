package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val JSON_NOTE = """{"blocks":[{"type":"paragraph","spans":[{"text":"json"}]}]}"""

private val JSON_BLOCKS = listOf(NoteBlock.Paragraph(listOf(NoteSpan("json"))))

private class CountingMarkdown : MarkdownConverter {
    val seen = mutableListOf<String>()

    override fun convert(raw: String): List<NoteBlock> {
        seen += raw
        return listOf(NoteBlock.Paragraph(listOf(NoteSpan("md"))))
    }
}

private fun brokenNote(raw: String) =
    listOf(NoteBlock.Unsupported(UnsupportedReason.BrokenNote, type = null, text = null, raw = raw))

private fun limitBox(raw: String) = listOf(NoteBlockDto.Unsupported(UnsupportedReasonDto.LIMIT_EXCEEDED, raw = raw))

class NoteFormatTest {
    private val markdown = CountingMarkdown()
    private val format = NoteFormat(markdown)

    @Test
    fun `NF-2 경로 확장자로 형식을 고른다`() {
        listOf("x.md", "x.MD", "notes/kotlin/x.Md").forEach { path ->
            assertEquals(path, listOf(NoteBlock.Paragraph(listOf(NoteSpan("md")))), format.parse(path, JSON_NOTE))
        }
        listOf("x.json", "x", "notes/x.markdown", "notes/md/x.json", "x.md.json").forEach { path ->
            assertEquals(path, JSON_BLOCKS, format.parse(path, JSON_NOTE))
        }

        assertEquals(listOf(JSON_NOTE), markdown.seen.distinct())
        assertEquals(3, markdown.seen.size)
    }

    @Test
    fun `NF-2 경로가 없는 옛 행은 JSON 이다`() {
        assertEquals(JSON_BLOCKS, format.parse(null, JSON_NOTE))
        assertEquals(format.derive("x.json", JSON_NOTE), format.derive(null, JSON_NOTE))
        assertEquals(JSON_BLOCKS, format.read("{깨짐", null, JSON_NOTE))

        assertTrue(markdown.seen.isEmpty())
    }

    @Test
    fun `NF-2 진입점은 어떤 입력에도 던지지 않는다`() {
        val failures =
            listOf<() -> Nothing>(
                { error("변환 실패") },
                { throw StackOverflowError() },
                { throw IndexOutOfBoundsException() },
            )
        failures.forEach { failure ->
            val failing = NoteFormat { failure() }
            val raw = "# 제목\n본문"

            assertEquals(brokenNote(raw), failing.parse("x.md", raw))
            assertEquals(
                DerivedNote("""[{"type":"unsupported","reason":"broken_note","raw":"# 제목\n본문"}]""", null),
                failing.derive("x.md", raw),
            )
            assertEquals(brokenNote(raw), failing.read("{깨짐", "x.md", raw))
        }

        val recursing =
            NoteFormat(
                object : MarkdownConverter {
                    override fun convert(raw: String): List<NoteBlock> = convert(raw)
                },
            )
        assertEquals(brokenNote("x"), recursing.parse("x.md", "x"))

        val deep = """{"blocks":[{"type":"x","a":${"[".repeat(100_000)}${"]".repeat(100_000)}}]}"""
        assertEquals(1, format.parse("x.json", deep).size)
        assertEquals(1, format.read(format.derive("x.json", deep).blocksJson, null, null).size)
    }

    @Test
    fun `NF-2 캐시를 못 읽으면 원문을 경로로 다시 읽고 원문도 없으면 캐시 글자를 든다`() {
        assertEquals(listOf(NoteBlock.Paragraph(listOf(NoteSpan("md")))), format.read("{깨짐", "x.md", "# a"))
        assertEquals(JSON_BLOCKS, format.read("{깨짐", "x.json", JSON_NOTE))
        assertEquals(brokenNote("{깨짐"), format.read("{깨짐", "x.md", null))
        assertEquals(JSON_BLOCKS, format.read(format.derive("x.json", JSON_NOTE).blocksJson, "x.md", "# 무시"))
    }

    @Test
    fun `NF-8 한도를 넘는 원문은 엔진을 부르지 않는다`() {
        val over =
            listOf(
                "a".repeat(524_289),
                "> ".repeat(33) + "x",
                "> - ".repeat(5_000),
                ">".repeat(20_000),
                "- ".repeat(1_000) + "x",
                (0 until 1_000).joinToString("\n") { "  ".repeat(it) + "- x" },
                (0 until 1_000).joinToString("\n") { "\t".repeat(it / 2) + (if (it % 2 == 1) "  " else "") + "- x" },
            )

        over.forEach { raw ->
            assertEquals(raw.take(20), limitBox(raw), format.blocks("x.md", raw))
            assertEquals(raw.take(20), brokenNote(raw), format.parse("x.md", raw))
            assertTrue(format.derive("x.md", raw).blocksJson.contains("\"reason\":\"limit_exceeded\""))
        }
        assertTrue(markdown.seen.isEmpty())

        format.parse("x.md", "> ".repeat(32) + "x")
        assertEquals(1, markdown.seen.size)
    }

    @Test
    fun `NF-8 해석 실패는 원문 전체를 든 노트 전체 상자다`() {
        val raw = "- a\n- b"
        val failing = NoteFormat { error("변환 실패") }

        assertEquals(
            listOf(NoteBlockDto.Unsupported(UnsupportedReasonDto.BROKEN_NOTE, raw = raw)),
            failing.blocks("x.md", raw),
        )
    }

    @Test
    fun `NF-2 마크다운 자리 표시는 지금 판의 JSON 해석과 같은 상자를 만든다`() {
        val placeholder = NoteFormat()
        val raw = "# 제목\n\n- 항목"

        assertEquals(placeholder.derive("x.json", raw), placeholder.derive("x.md", raw))
    }
}
