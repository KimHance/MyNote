package com.hancekim.mynote.core.data.dto

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.format.NoteFormat

internal fun List<CachedNote>.toCategories(format: NoteFormat): List<Category> =
    groupBy { it.group }.map { (group, cached) ->
        Category(name = group, notes = cached.map { it.toDomainNote(format) })
    }

internal fun CachedNote.toDomainNote(format: NoteFormat): Note =
    Note(id = key, title = title, date = date.orEmpty(), blocks = format.read(blocksJson, path, raw))
