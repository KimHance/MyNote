package com.hancekim.mynote.core.data.dto

import com.hancekim.mynote.core.data.datasource.ListedNote
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.format.NoteFormat

internal suspend fun List<ListedNote>.toCategories(
    format: NoteFormat,
    raw: suspend (key: String) -> String?,
): List<Category> =
    groupBy { it.group }.map { (group, listed) ->
        Category(name = group, notes = listed.map { it.toDomainNote(format, raw) })
    }

internal suspend fun ListedNote.toDomainNote(
    format: NoteFormat,
    raw: suspend (key: String) -> String?,
): Note = Note(id = key, title = title, date = date.orEmpty(), blocks = format.read(blocksJson, path) { raw(key) })
