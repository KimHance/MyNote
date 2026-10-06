package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.GenerateQuizUseCase
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.QuizQuestion
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.model.TableCell
import com.hancekim.mynote.core.domain.model.firstSentence
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

private const val NOTE_COUNT = 22

private data class Text(
    val text: String,
    val marks: List<Pair<Char, String>>,
)

private fun List<NoteSpan>.canonical(): Text =
    Text(
        text = joinToString("") { it.text },
        marks =
            flatMap { span ->
                span.text.filterNot(Char::isWhitespace).map { it to "${span.style}/${span.italic}/${span.strike}" }
            },
    )

private fun List<NoteBlock>.canonical(): List<Any> = map { it.canonical() }

private fun NoteBlock.canonical(): Any =
    when (this) {
        is NoteBlock.Heading -> listOf("heading", level, spans.canonical())
        is NoteBlock.Paragraph -> listOf("paragraph", spans.canonical())
        is NoteBlock.Quote -> listOf("quote", spans.canonical())
        is NoteBlock.Code -> listOf("code", code)
        is NoteBlock.BulletList -> listOf("bullet", items.map { it.canonical() })
        is NoteBlock.OrderedList -> listOf("ordered", start, items.map { it.canonical() })
        is NoteBlock.Table -> listOf("table", header.cells(), rows.map { it.cells() }, align)
        NoteBlock.Divider -> "divider"
        NoteBlock.Spacer -> "spacer"
        is NoteBlock.Unsupported -> listOf("unsupported", reason, raw)
    }

private fun NoteListItem.canonical(): Any = listOf(spans.canonical(), checked, children.canonical())

private fun List<TableCell>.cells(): List<Text> = map { it.spans.canonical() }

private fun List<NoteBlock>.hasBox(): Boolean =
    any { block ->
        when (block) {
            is NoteBlock.Unsupported -> true
            is NoteBlock.BulletList -> block.items.any { it.children.hasBox() }
            is NoteBlock.OrderedList -> block.items.any { it.children.hasBox() }
            else -> false
        }
    }

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
        "%02x".format(it)
    }

private class CapturingGenerator : QuizGenerator {
    var text: String? = null

    override suspend fun isAvailable(): Boolean = true

    override suspend fun generate(
        noteTitle: String,
        noteText: String,
        count: Int,
    ): Result<List<QuizQuestion>> {
        text = noteText
        return Result.success(emptyList())
    }
}

private suspend fun quizText(note: Note): String? {
    val repository =
        object : NotesRepository {
            override fun observeCategories(): Flow<List<Category>> = flowOf(listOf(Category("c", listOf(note))))

            override suspend fun sync(): Result<SyncOutcome> = Result.success(SyncOutcome())
        }
    val generator = CapturingGenerator()
    GenerateQuizUseCase(repository, generator)(note.id)
    return generator.text
}

private fun repoNotes(): File =
    generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .map { File(it, "notes") }
        .first { File(it, "index.json").isFile }

class ParityTest {
    private val format = NoteFormat()
    private val notes = ParityNotes.all

    @Test
    fun `NF-11 JSON 사본은 기준 목록의 해시와 같다`() {
        assertEquals(NOTE_COUNT, notes.size)
        assertEquals(NOTE_COUNT, notes.map { it.id }.toSet().size)
        notes.forEach { assertEquals(it.jsonPath, it.hash, sha256(it.jsonBytes)) }
    }

    @Test
    fun `NF-11 노트 22개가 JSON 과 같은 블록이 된다`() {
        assertEquals(NOTE_COUNT, notes.size)
        var spacers = 0
        notes.forEach { note ->
            val json = format.parse(note.jsonPath, note.json)
            val markdown = format.parse(note.markdownPath, note.markdown)
            spacers += json.count { it == NoteBlock.Spacer }

            assertFalse(note.markdownPath, markdown.hasBox())
            assertEquals(note.markdownPath, json.filterNot { it == NoteBlock.Spacer }.canonical(), markdown.canonical())
        }
        assertEquals(1, spacers)
    }

    @Test
    fun `NF-11 노트 22개의 첫 문장과 퀴즈 평문이 같다`() =
        runTest {
            assertEquals(NOTE_COUNT, notes.size)
            notes.forEach { note ->
                val json = Note(note.id, note.title, note.date, format.parse(note.jsonPath, note.json))
                val markdown = Note(note.id, note.title, note.date, format.parse(note.markdownPath, note.markdown))

                assertEquals(note.markdownPath, json.firstSentence(), markdown.firstSentence())
                assertEquals(
                    note.markdownPath,
                    format.derive(note.jsonPath, note.json).excerpt,
                    format.derive(note.markdownPath, note.markdown).excerpt,
                )
                val quiz = quizText(markdown)
                assertTrue(note.markdownPath, !quiz.isNullOrEmpty())
                assertEquals(note.markdownPath, quizText(json), quiz)
            }
        }

    @Test
    fun `NF-11 레포 노트는 상자 없이 읽힌다`() {
        val root = repoNotes()
        val files =
            root
                .listFiles()
                .orEmpty()
                .filter { it.isDirectory }
                .flatMap { group ->
                    group.listFiles().orEmpty().filter {
                        it.isFile &&
                            it.extension in setOf("md", "json")
                    }
                }

        assertTrue("레포 노트가 없다", files.isNotEmpty())
        files.forEach { file ->
            val path = file.relativeTo(root.parentFile).invariantSeparatorsPath
            val blocks = format.parse(path, file.readText())

            assertTrue(path, blocks.isNotEmpty())
            assertFalse(path, blocks.hasBox())
        }
    }
}
