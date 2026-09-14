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

/**
 * 노트 하나로 퀴즈를 만든다.
 *
 * **본문을 자르는 자리가 여기다.** 프롬프트 길이가 곧 첫 응답까지의 지연이라(모델이 질문을 다
 * 읽어야 답을 시작한다) 통제가 필요한데, 구현마다 제각각 자르면 기준이 흩어진다.
 */
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
                    ?: return Result.failure(QuizFailure.Empty("노트를 찾을 수 없습니다: $noteId"))

            val text = note.toPlainText().take(MAX_CHARS)
            if (text.isBlank()) {
                return Result.failure(QuizFailure.Empty("본문이 비어 있어 문제를 만들 수 없습니다"))
            }
            return generator.generate(noteTitle = note.title, noteText = text, count = count)
        }

        private companion object {
            const val DEFAULT_COUNT = 3

            /**
             * 본문 상한.
             *
             * 넉넉히 잡을수록 문제가 풍부해지지만 그만큼 기다린다. 노트 대부분이 이 안에 들어오고,
             * 넘는 노트는 앞부분만으로도 출제가 되는 선으로 잡았다.
             */
            const val MAX_CHARS = 2_000
        }
    }

/** 블록 구조를 모델이 읽을 평문으로 편다. 서식은 버리고 글자만 남긴다. */
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
                NoteBlock.Divider, NoteBlock.Spacer -> null
            }.takeIf { !it.isNullOrBlank() }
        }.joinToString("\n")

private fun List<NoteSpan>.joinText(): String = joinToString("") { it.text }

private fun List<NoteListItem>.joinItems(): String = joinToString("\n") { "- " + it.spans.joinText() }
