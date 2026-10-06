package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.QuizFailure
import com.hancekim.mynote.core.domain.model.QuizQuestion
import com.hancekim.mynote.core.domain.model.TableCell
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GenerateQuizUseCase
    @Inject
    constructor(
        private val repository: NotesRepository,
        private val generator: QuizGenerator,
    ) {
        suspend operator fun invoke(
            noteId: String,
            count: Int = DEFAULT_COUNT,
        ): Result<List<QuizQuestion>> {
            val note =
                repository
                    .observeCategories()
                    .first()
                    .firstNotNullOfOrNull { category -> category.notes.firstOrNull { it.id == noteId } }
                    ?: return Result.failure(QuizFailure.Empty("note not found: $noteId"))

            val text = note.toPlainText().take(MAX_CHARS)
            if (text.isBlank()) {
                return Result.failure(QuizFailure.Empty("note body is blank"))
            }
            return generator.generate(noteTitle = note.title, noteText = text, count = count)
        }

        private companion object {
            const val DEFAULT_COUNT = 3

            const val MAX_CHARS = 2_000
        }
    }

private const val INDENT = "  "
private const val STRIKE = "~~"

private fun Note.toPlainText(): String = blocks.plainLines(depth = 0).joinToString("\n")

private fun List<NoteBlock>.plainLines(depth: Int): List<String> = flatMap { it.plainLines(depth) }

private fun NoteBlock.plainLines(depth: Int): List<String> =
    when (this) {
        is NoteBlock.Heading -> spans.plain().indented(depth)
        is NoteBlock.Paragraph -> spans.plain().indented(depth)
        is NoteBlock.Quote -> spans.plain().indented(depth)
        is NoteBlock.Code -> code.indented(depth)
        is NoteBlock.BulletList -> items.flatMap { it.plainLines(depth) }
        is NoteBlock.OrderedList -> items.flatMap { it.plainLines(depth) }
        is NoteBlock.Table -> (listOf(header) + rows).flatMap { row -> row.plainRow().indented(depth) }
        is NoteBlock.Unsupported -> text.indented(depth)
        NoteBlock.Divider, NoteBlock.Spacer -> emptyList()
    }

private fun NoteListItem.plainLines(depth: Int): List<String> {
    val mark =
        when (checked) {
            true -> "- [x] "
            false -> "- [ ] "
            null -> "- "
        }
    val text = spans.plain()
    val line = if (text.isBlank()) emptyList() else (mark + text).indented(depth)
    return line + children.plainLines(depth + 1)
}

private fun List<TableCell>.plainRow(): String = joinToString(" | ") { it.spans.plain() }

private fun List<NoteSpan>.plain(): String =
    buildString {
        var striking = false
        for (span in this@plain) {
            if (span.text.isEmpty()) continue
            if (span.strike != striking) {
                append(STRIKE)
                striking = span.strike
            }
            append(span.text)
        }
        if (striking) append(STRIKE)
    }

private fun String?.indented(depth: Int): List<String> {
    if (isNullOrBlank()) return emptyList()
    val prefix = INDENT.repeat(depth)
    return split("\n").map { prefix + it }
}
