package com.hancekim.mynote.feature.notes

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

fun List<Category>.toUi(): ImmutableList<CategoryUi> = map { it.toUi() }.toImmutableList()

fun Category.toUi(): CategoryUi =
    CategoryUi(
        name = name,
        notes = notes.map { it.toUi() }.toImmutableList(),
    )

fun Note.toUi(): NoteUi =
    NoteUi(
        id = id,
        title = title,
        date = date,
        blocks = blocks.toContentBlocks(),
    )
