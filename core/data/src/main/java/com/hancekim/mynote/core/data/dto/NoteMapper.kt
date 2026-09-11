package com.hancekim.mynote.core.data.dto

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import kotlinx.serialization.json.Json

fun CategoryDto.toDomain(): Category = Category(name = name, notes = notes.map { it.toDomain() })

fun NoteDto.toDomain(): Note = Note(id = id, title = title, date = date, blocks = blocks.map { it.toDomain() })

fun NoteBlockDto.toDomain(): NoteBlock =
    when (this) {
        is NoteBlockDto.Heading -> NoteBlock.Heading(level, spans.toDomainSpans())
        is NoteBlockDto.Paragraph -> NoteBlock.Paragraph(spans.toDomainSpans())
        is NoteBlockDto.Code -> NoteBlock.Code(code)
        is NoteBlockDto.Quote -> NoteBlock.Quote(spans.toDomainSpans())
        is NoteBlockDto.BulletList -> NoteBlock.BulletList(items.map { NoteListItem(it.spans.toDomainSpans()) })
        is NoteBlockDto.OrderedList -> NoteBlock.OrderedList(items.map { NoteListItem(it.spans.toDomainSpans()) })
        NoteBlockDto.Divider -> NoteBlock.Divider
        NoteBlockDto.Spacer -> NoteBlock.Spacer
    }

private fun List<NoteSpanDto>.toDomainSpans(): List<NoteSpan> =
    map { NoteSpan(it.text, it.style.toDomain()) }

private fun SpanStyleDto.toDomain(): SpanStyle =
    when (this) {
        SpanStyleDto.TEXT -> SpanStyle.Text
        SpanStyleDto.BOLD -> SpanStyle.Bold
        SpanStyleDto.CODE -> SpanStyle.Code
    }

fun List<CachedNote>.toCategories(json: Json): List<Category> =
    groupBy { it.group }.mapNotNull { (group, cached) ->
        val notes = cached.mapNotNull { runCatching { it.toDomainNote(json) }.getOrNull() }
        if (notes.isEmpty()) null else Category(name = group, notes = notes)
    }

fun CachedNote.toDomainNote(json: Json): Note {
    val blocks = json.decodeFromString<List<NoteBlockDto>>(blocksJson).map { it.toDomain() }
    return Note(id = id, title = title, date = date, blocks = blocks)
}
