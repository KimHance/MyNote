package com.hancekim.mynote.feature.notes.mapper

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.QuizQuestion
import com.hancekim.mynote.core.domain.model.firstSentence
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.QuizQuestionUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun List<Category>.toUi(): ImmutableList<CategoryUi> = map { it.toUi() }.toImmutableList()

internal fun Category.toUi(): CategoryUi =
    CategoryUi(
        name = name,
        notes = notes.map { it.toUi() }.toImmutableList(),
    )

internal fun Note.toUi(): NoteUi =
    NoteUi(
        id = id,
        title = title,
        date = date,
        blocks = blocks.toContentBlocks(),
        excerpt = firstSentence(),
        readable = blocks.none { it is NoteBlock.Unsupported && it.reason.wholeNote },
    )

internal fun QuizQuestion.toUi(): QuizQuestionUi =
    QuizQuestionUi(
        question = question,
        choices = choices.toImmutableList(),
        answerIndex = answerIndex,
        explanation = explanation,
    )
