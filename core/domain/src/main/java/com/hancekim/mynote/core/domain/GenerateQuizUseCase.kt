package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.QuizFailure
import com.hancekim.mynote.core.domain.model.QuizQuestion
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

private fun Note.toPlainText(): String =
    blocks
        .mapNotNull { block ->
            when (block) {
                is NoteBlock.Heading -> block.spans.joinText()
                is NoteBlock.Paragraph -> block.spans.joinText()
                is NoteBlock.Quote -> block.spans.joinText()
                is NoteBlock.Code -> block.code
                is NoteBlock.BulletList -> block.items.joinItems()
                is NoteBlock.OrderedList -> block.items.joinItems()
                is NoteBlock.Unsupported -> block.text
                NoteBlock.Divider, NoteBlock.Spacer -> null
            }.takeIf { !it.isNullOrBlank() }
        }.joinToString("\n")

private fun List<NoteSpan>.joinText(): String = joinToString("") { it.text }

private fun List<NoteListItem>.joinItems(): String = joinToString("\n") { "- " + it.spans.joinText() }
