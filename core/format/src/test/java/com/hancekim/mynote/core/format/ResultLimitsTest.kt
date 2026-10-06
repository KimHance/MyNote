package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LIMIT_BOX_CACHE = """[{"type":"unsupported","reason":"limit_exceeded","raw":""}]"""

private const val BROKEN_BOX_CACHE = """[{"type":"unsupported","reason":"broken_note","raw":""}]"""

private fun limitNote(raw: String) =
    listOf(NoteBlock.Unsupported(UnsupportedReason.LimitExceeded, type = null, text = null, raw = raw))

private fun brokenNote(raw: String) =
    listOf(NoteBlock.Unsupported(UnsupportedReason.BrokenNote, type = null, text = null, raw = raw))

private fun wideTable(
    columns: Int,
    rows: Int,
): String = "|" + "h|".repeat(columns) + "\n|" + "-|".repeat(columns) + "\n" + "|a\n".repeat(rows)

private fun utf8(text: String): Int = text.toByteArray(Charsets.UTF_8).size

private fun repeatTo(
    unit: String,
    bytes: Int,
): String = unit.repeat(bytes / utf8(unit))

class ResultLimitsTest {
    private val format = NoteFormat()

    @Test
    fun `NF-8 넓은 표로 부푼 노트는 던지지 않고 한도 넘음이다`() {
        listOf(
            wideTable(columns = 1_000, rows = 2_000),
            wideTable(columns = 2_000, rows = 10_000),
            wideTable(columns = 1_000, rows = 150_000),
        ).forEach { raw ->
            assertTrue(raw.length <= NoteLimits.Default.maxBytes)
            assertEquals(raw.take(20), limitNote(raw), format.parse("x.md", raw))
            assertEquals(raw.take(20), DerivedNote(LIMIT_BOX_CACHE, null), format.derive("x.md", raw))
        }
    }

    @Test
    fun `NF-8 표 칸 수는 노트 전체로 센다`() {
        val limits = NoteLimits(maxTableCells = 12)
        val converter = GfmMarkdownConverter(limits)
        val table = "|a|b|\n|-|-|\n|1|2|\n|3|4|\n"

        assertEquals(2, converter.convert(table + "\n" + table).count { it is NoteBlock.Table })
        val over = table + "\n" + table + "\n|c|\n|-|\n"
        assertEquals(limitNote(over), converter.convert(over))
        val padded = "|a|b|c|d|\n|-|-|-|-|\n|1\n|2\n"
        assertEquals(1, converter.convert(padded).size)
        assertEquals(limitNote(padded + "|3\n"), converter.convert(padded + "|3\n"))
    }

    @Test
    fun `NF-8 캐시가 상한을 넘으면 노트 전체가 한도 넘음이다`() {
        val small = NoteFormat(GfmMarkdownConverter(), NoteLimits(maxCacheKilobytes = 1))
        val fits = "x".repeat(1_024 - utf8("""[{"type":"code","code":""}]"""))

        assertEquals(1_024, utf8(small.derive("x.json", wire(fits)).blocksJson))
        assertEquals(listOf(NoteBlock.Code(fits)), small.parse("x.json", wire(fits)))
        assertEquals(DerivedNote(LIMIT_BOX_CACHE, null), small.derive("x.json", wire(fits + "x")))
        assertEquals(limitNote(wire(fits + "x")), small.parse("x.json", wire(fits + "x")))

        val markdown = "```\n${fits}x\n```"
        assertEquals(DerivedNote(LIMIT_BOX_CACHE, null), small.derive("x.md", markdown))
        assertEquals(limitNote(markdown), small.parse("x.md", markdown))
    }

    @Test
    fun `NF-8 한도 안 어떤 입력도 캐시가 상한 안이다`() {
        val max = NoteLimits.Default.maxBytes
        listOf(
            repeatTo("# a\n", max),
            repeatTo("a\n\n", max),
            repeatTo("- a\n", max),
            repeatTo("**a** ", max),
            repeatTo("`a` ", max),
            repeatTo("~a~ ", max),
            repeatTo("가\n\n", max),
            repeatTo("- a\n  - b\n", max),
            "|a|\n|-|\n" + repeatTo("|a|\n", max - 8),
            "|" + "a|".repeat(100) + "\n|" + "-|".repeat(100) + "\n" +
                repeatTo("|" + "a|".repeat(100) + "\n", max - 500),
        ).forEach { raw ->
            assertTrue(utf8(raw) <= max)
            val derived = format.derive("x.md", raw)

            assertTrue(raw.take(20), utf8(derived.blocksJson) <= NoteLimits.Default.maxCacheBytes)
        }
    }

    @Test
    fun `NF-8 노트 전체 상자는 캐시에 원문을 두지 않고 원문 열에서 읽는다`() =
        runTest {
            val over = "> ".repeat(33) + "x"
            val failing = NoteFormat { error("변환 실패") }
            var reads = 0

            assertEquals(LIMIT_BOX_CACHE, format.derive("x.md", over).blocksJson)
            assertEquals(BROKEN_BOX_CACHE, failing.derive("x.md", "# a").blocksJson)
            assertEquals(BROKEN_BOX_CACHE, format.derive("x.json", "not json").blocksJson)
            assertEquals(limitNote(over), format.read(LIMIT_BOX_CACHE, "x.md") { over })
            assertEquals(brokenNote("not json"), format.read(BROKEN_BOX_CACHE, "x.json") { "not json" })
            assertEquals(limitNote(""), format.read(LIMIT_BOX_CACHE, "x.md") { null })

            val legacy = """[{"type":"unsupported","reason":"broken_note","raw":"옛 원문"}]"""
            assertEquals(brokenNote("옛 원문"), format.read(legacy, "x.json") { "새 원문".also { reads++ } })
            val paragraph = format.derive("x.md", "본문").blocksJson
            assertEquals(
                listOf(NoteBlock.Paragraph(listOf(NoteSpan("본문")))),
                format.read(paragraph, "x.md") { "본문".also { reads++ } },
            )
            assertEquals(0, reads)
        }
}

private fun wire(code: String): String =
    """{"blocks":[{"type":"code","code":"$code"}]}"""
