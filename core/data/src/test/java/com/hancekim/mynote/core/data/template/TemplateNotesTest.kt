package com.hancekim.mynote.core.data.template

import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.data.dto.IndexNoteDto
import com.hancekim.mynote.core.data.dto.NotesJson
import com.hancekim.mynote.core.data.repository.FakeAdapter
import com.hancekim.mynote.core.data.repository.FakeLocal
import com.hancekim.mynote.core.data.repository.NotesRepositoryImpl
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.TableAlign
import com.hancekim.mynote.core.format.NoteFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private val TEMPLATE_DIR = File("../../public-repos/notes-template")

// 글자로만 보이는 문법의 흔적(결정 28) — 원문 줄에서 찾는 것과 해석한 글자에서 찾는 것
private val RAW_TRACES = listOf(Regex("""\]\("""), Regex("""!\["""), Regex("""<[A-Za-z/]"""), Regex("""https?://"""))
private val TEXT_TRACES = listOf("[^", "[!", "$", "==")
private val CODE_SPAN = Regex("""(`+).*?\1""")
private val FENCE = Regex("""^\s*(```|~~~)""")

private data class TemplateNote(
    val group: String,
    val item: IndexNoteDto,
    val raw: String,
)

private fun templateNotes(): List<TemplateNote> {
    val index = File(TEMPLATE_DIR, "notes/index.json").readText()
    return NotesJson
        .decodeFromString<Map<String, List<IndexNoteDto>>>(index)
        .flatMap { (group, items) ->
            items.map { TemplateNote(group, it, File(TEMPLATE_DIR, it.path).readText()) }
        }
}

private fun NoteBlock.items(): List<NoteListItem> =
    when (this) {
        is NoteBlock.BulletList -> items
        is NoteBlock.OrderedList -> items
        else -> emptyList()
    }

/** 블록과 목록 항목 아래 하위 블록을 모두 펼친다. */
private fun List<NoteBlock>.allBlocks(): List<NoteBlock> =
    flatMap { block ->
        listOf(block) + block.items().flatMap { it.children }.allBlocks()
    }

/** 블록이 직접 든 글자 조각 — 코드 블록은 조각이 없다. */
private fun NoteBlock.spans(): List<NoteSpan> =
    when (this) {
        is NoteBlock.Heading -> spans
        is NoteBlock.Paragraph -> spans
        is NoteBlock.Quote -> spans
        is NoteBlock.Table -> (header + rows.flatten()).flatMap { it.spans }
        else -> items().flatMap { it.spans }
    }

/** 코드 모양(코드 블록 · 코드 구간)을 뺀 글자. */
private fun NoteBlock.plainText(): String = spans().filter { it.style != SpanStyle.Code }.joinToString("") { it.text }

/** 펜스 코드 블록 안의 줄을 빼고, 남은 줄에서 코드 구간을 지운다. */
private fun rawLinesOutsideCode(raw: String): List<String> {
    var fenced = false
    return raw.lines().mapNotNull { line ->
        val fence = FENCE.containsMatchIn(line)
        if (fence) fenced = !fenced
        if (fence || fenced) null else CODE_SPAN.replace(line, "")
    }
}

class TemplateNotesTest {
    private val format = NoteFormat()

    private fun parsed(note: TemplateNote): List<NoteBlock> = format.parse(note.item.path, note.raw)

    @Test
    fun `NT-6 템플릿 예시가 앱 파서에서 하나도 강등되지 않는다`() {
        val notes = templateNotes()
        assertTrue(notes.isNotEmpty())

        notes.forEach { note ->
            val path = note.item.path
            assertTrue(path, path.endsWith(".md"))
            val blocks = parsed(note)
            assertTrue(path, blocks.isNotEmpty())
            blocks.allBlocks().forEach { block ->
                assertTrue("$path: $block", block !is NoteBlock.Unsupported)
            }
            // 앞머리가 새면 경계가 구분선 · 밑줄 제목이 되고 키 줄이 글자로 남는다
            assertTrue(path, blocks.first() !is NoteBlock.Divider)
            blocks.allBlocks().forEach { block ->
                val text = block.plainText()
                assertFalse("$path: $text", "title:" in text || "date:" in text)
            }
        }
    }

    @Test
    fun `NT-6 템플릿 예시가 앱 동기화로 전부 받아진다`() =
        runTest {
            val notes = templateNotes()
            val adapter =
                FakeAdapter(
                    notes.map {
                        SourceEntry(it.item.id, it.item.path, it.item.hash, it.group, it.item.title, it.item.date)
                    },
                    docs = notes.associate { it.item.id to it.raw },
                )
            val repo = NotesRepositoryImpl(setOf(adapter), FakeLocal(), format, {}) { 1L }

            val outcome = repo.sync().getOrThrow()

            val synced = repo.observeCategories().first().flatMap { it.notes }
            assertEquals(notes.size, outcome.added)
            assertEquals(notes.size, synced.size)
            assertEquals(notes.map { "s0/${it.item.id}" }.toSet(), synced.map { it.id }.toSet())
            synced.forEach { note ->
                assertTrue(note.id, note.blocks.isNotEmpty())
                assertTrue(note.id, note.blocks.allBlocks().none { it is NoteBlock.Unsupported })
            }
        }

    @Test
    fun `NT-6 템플릿 예시가 앱이 그리는 모양을 다 쓴다`() {
        val blocks = templateNotes().flatMap { parsed(it).allBlocks() }
        val items = blocks.flatMap { it.items() }
        val spans = blocks.flatMap { it.spans() }

        assertEquals(setOf(2, 3), blocks.filterIsInstance<NoteBlock.Heading>().map { it.level }.toSet())
        assertTrue(blocks.any { it is NoteBlock.Paragraph })
        assertTrue(blocks.any { it is NoteBlock.Code })
        assertTrue(blocks.any { it is NoteBlock.Quote })
        assertTrue(blocks.any { it is NoteBlock.BulletList })
        assertTrue(blocks.filterIsInstance<NoteBlock.OrderedList>().any { it.start != 1 })
        val tables = blocks.filterIsInstance<NoteBlock.Table>()
        assertTrue(tables.any { table -> table.align.any { it != TableAlign.None } })
        assertTrue(blocks.any { it is NoteBlock.Divider })
        assertTrue(items.any { it.checked == true })
        assertTrue(items.any { it.checked == false })
        assertTrue(items.any { it.children.isNotEmpty() })
        assertTrue(spans.any { it.style == SpanStyle.Bold })
        assertTrue(spans.any { it.style == SpanStyle.Code })
        assertTrue(spans.any { it.italic })
        assertTrue(spans.any { it.strike })
    }

    @Test
    fun `NT-6 템플릿 예시에 글자로만 보이는 문법이 없다`() {
        templateNotes().forEach { note ->
            val path = note.item.path
            rawLinesOutsideCode(note.raw).forEach { line ->
                RAW_TRACES.forEach { trace -> assertFalse("$path: $line", trace.containsMatchIn(line)) }
            }
            parsed(note).allBlocks().forEach { block ->
                val text = block.plainText()
                TEXT_TRACES.forEach { trace -> assertFalse("$path: $text", trace in text) }
            }
        }
    }
}
