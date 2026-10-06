package com.hancekim.mynote.core.data.template

import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.data.dto.IndexNoteDto
import com.hancekim.mynote.core.data.dto.NotesJson
import com.hancekim.mynote.core.data.repository.FakeAdapter
import com.hancekim.mynote.core.data.repository.FakeLocal
import com.hancekim.mynote.core.data.repository.NotesRepositoryImpl
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.format.NoteFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private val TEMPLATE_DIR = File("../../public-repos/notes-template")

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

private fun JsonObject.spanStyles(): List<String> {
    val direct = (this["spans"] as? JsonArray).orEmpty()
    val nested = (this["items"] as? JsonArray).orEmpty().flatMap { (it.jsonObject["spans"] as JsonArray) }
    return (direct + nested).map { (it.jsonObject["style"] as? JsonPrimitive)?.content ?: "text" }
}

class TemplateNotesTest {
    private val format = NoteFormat()

    @Test
    fun `NT-6 템플릿 예시가 앱 파서에서 하나도 강등되지 않는다`() {
        val notes = templateNotes()
        assertTrue(notes.isNotEmpty())

        notes.forEach { note ->
            val wire = NotesJson.parseToJsonElement(note.raw).jsonObject["blocks"]!!.jsonArray
            val parsed = format.parse(note.item.path, note.raw)

            assertEquals(note.item.path, wire.size, parsed.size)
            parsed.forEach { block ->
                assertTrue("${note.item.path}: $block", block !is NoteBlock.Unsupported)
            }
            val written = wire.flatMap { it.jsonObject.spanStyles() }.count { it != "text" }
            val read = parsed.flatMap(::spanStylesOf).count { it != SpanStyle.Text }
            assertEquals(note.item.path, written, read)
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
                assertTrue(note.id, note.blocks.none { it is NoteBlock.Unsupported })
            }
        }

    private fun spanStylesOf(block: NoteBlock): List<SpanStyle> =
        when (block) {
            is NoteBlock.Heading -> block.spans.map { it.style }
            is NoteBlock.Paragraph -> block.spans.map { it.style }
            is NoteBlock.Quote -> block.spans.map { it.style }
            is NoteBlock.BulletList -> block.items.flatMap { item -> item.spans.map { it.style } }
            is NoteBlock.OrderedList -> block.items.flatMap { item -> item.spans.map { it.style } }
            else -> emptyList()
        }
}
