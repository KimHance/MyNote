package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.QuizQuestion
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.model.TableAlign
import com.hancekim.mynote.core.domain.model.TableCell
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import com.hancekim.mynote.core.domain.model.firstSentence
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FixedNotes(
    private val note: Note,
) : NotesRepository {
    override fun observeCategories(): Flow<List<Category>> = flowOf(listOf(Category("g", listOf(note))))

    override suspend fun sync(): Result<SyncOutcome> = Result.success(SyncOutcome())
}

private class RecordingGenerator : QuizGenerator {
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

private fun spans(text: String) = listOf(NoteSpan(text))

private fun item(
    text: String,
    checked: Boolean? = null,
    vararg children: NoteBlock,
) = NoteListItem(spans(text), checked, children.toList())

private fun cell(text: String) = TableCell(spans(text))

class GenerateQuizUseCaseTest {
    private suspend fun plainTextOf(vararg blocks: NoteBlock): String? {
        val generator = RecordingGenerator()
        GenerateQuizUseCase(FixedNotes(Note("s0/a", "t", "2026.01.01", blocks.toList())), generator)("s0/a")
        return generator.text
    }

    @Test
    fun `NF-10 새 블록의 퀴즈 평문`() =
        runTest {
            val table =
                NoteBlock.Table(
                    header = listOf(cell("h1"), cell("h2")),
                    rows = listOf(listOf(cell("a"), cell("b"))),
                    align = listOf(TableAlign.Left, TableAlign.None),
                )
            assertEquals("h1 | h2\na | b", plainTextOf(table))

            val checks =
                NoteBlock.BulletList(listOf(item("a", checked = true), item("b", checked = false), item("c")))
            assertEquals("- [x] a\n- [ ] b\n- c", plainTextOf(checks))

            val ordered = NoteBlock.OrderedList(listOf(item("a", checked = true), item("b")), start = 3)
            assertEquals("- [x] a\n- b", plainTextOf(ordered))

            val blankItem = NoteBlock.BulletList(listOf(item(""), item(" ", checked = true), item("x")))
            assertEquals("- x", plainTextOf(blankItem))
        }

    @Test
    fun `NF-10 하위 블록 평문 들여쓰기`() =
        runTest {
            val nested =
                NoteBlock.BulletList(
                    listOf(
                        item(
                            "a",
                            null,
                            NoteBlock.BulletList(listOf(item("b", null, NoteBlock.OrderedList(listOf(item("c")))))),
                            NoteBlock.Code("x\ny"),
                            NoteBlock.Paragraph(spans("p")),
                            NoteBlock.Table(listOf(cell("h")), listOf(listOf(cell("v"))), listOf(TableAlign.None)),
                        ),
                        item("d"),
                    ),
                )
            assertEquals(
                listOf("- a", "  - b", "    - c", "  x", "  y", "  p", "  h", "  v", "- d").joinToString("\n"),
                plainTextOf(nested),
            )

            val headless = NoteBlock.BulletList(listOf(item("", null, NoteBlock.Heading(1, spans("h")))))
            assertEquals("  h", plainTextOf(headless))
        }

    @Test
    fun `NF-10 취소선은 퀴즈 평문에만 물결로 남는다`() =
        runTest {
            val paragraph = NoteBlock.Paragraph(listOf(NoteSpan("a "), NoteSpan("b", strike = true)))
            assertEquals("a ~~b~~", plainTextOf(paragraph))
            assertEquals("a b", Note("n", "t", "d", listOf(paragraph)).firstSentence())

            val joined =
                NoteBlock.Paragraph(
                    listOf(
                        NoteSpan("x", SpanStyle.Bold, strike = true),
                        NoteSpan("y", SpanStyle.Code, strike = true),
                        NoteSpan("", strike = false),
                        NoteSpan("z", italic = true, strike = true),
                        NoteSpan(" w"),
                    ),
                )
            assertEquals("~~xyz~~ w", plainTextOf(joined))

            val inTable =
                NoteBlock.Table(
                    listOf(TableCell(listOf(NoteSpan("o", strike = true)))),
                    emptyList(),
                    listOf(TableAlign.None),
                )
            val inItem =
                NoteBlock.BulletList(
                    listOf(NoteListItem(listOf(NoteSpan("i", strike = true)), checked = true)),
                )
            assertEquals("~~o~~\n- [x] ~~i~~", plainTextOf(inTable, inItem))
        }

    @Test
    fun `Unsupported 는 건진 글자만 퀴즈 평문에 들어가고 원문은 들어가지 않는다`() =
        runTest {
            val note =
                Note(
                    id = "s0/a",
                    title = "t",
                    date = "2026.01.01",
                    blocks =
                        listOf(
                            NoteBlock.Paragraph(listOf(NoteSpan("앞"))),
                            NoteBlock.Unsupported(
                                UnsupportedReason.UnknownType,
                                "timeline",
                                "건진 글자",
                                "{\"type\":\"timeline\"}",
                            ),
                            NoteBlock.Unsupported(
                                UnsupportedReason.BrokenBlock,
                                "heading",
                                null,
                                "{\"type\":\"heading\"}",
                            ),
                        ),
                )
            val generator = RecordingGenerator()

            GenerateQuizUseCase(FixedNotes(note), generator)("s0/a")

            assertEquals("앞\n건진 글자", generator.text)
        }
}
