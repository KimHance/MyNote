package com.hancekim.mynote.core.data.dto

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.format.MyNoteJsonFormat
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.UnsupportedReason

fun NoteBlockDto.toDomain(): NoteBlock =
    when (this) {
        NoteBlockDto.Unknown -> NoteBlock.Unsupported(UnsupportedReason.UnknownType, type = null, text = null, raw = "")
        is NoteBlockDto.Unsupported -> NoteBlock.Unsupported(reason.toDomain(), original, text, raw)
        is NoteBlockDto.Heading -> NoteBlock.Heading(level, spans.toDomainSpans())
        is NoteBlockDto.Paragraph -> NoteBlock.Paragraph(spans.toDomainSpans())
        is NoteBlockDto.Code -> NoteBlock.Code(code)
        is NoteBlockDto.Quote -> NoteBlock.Quote(spans.toDomainSpans())
        is NoteBlockDto.BulletList -> NoteBlock.BulletList(items.map { NoteListItem(it.spans.toDomainSpans()) })
        is NoteBlockDto.OrderedList -> NoteBlock.OrderedList(items.map { NoteListItem(it.spans.toDomainSpans()) })
        NoteBlockDto.Divider -> NoteBlock.Divider
        NoteBlockDto.Spacer -> NoteBlock.Spacer
    }

private fun UnsupportedReasonDto.toDomain(): UnsupportedReason =
    when (this) {
        UnsupportedReasonDto.UNKNOWN_TYPE -> UnsupportedReason.UnknownType
        UnsupportedReasonDto.BROKEN_BLOCK -> UnsupportedReason.BrokenBlock
        UnsupportedReasonDto.BROKEN_NOTE -> UnsupportedReason.BrokenNote
    }

private fun List<NoteSpanDto>.toDomainSpans(): List<NoteSpan> = map { NoteSpan(it.text, it.style.toDomain()) }

private fun SpanStyleDto.toDomain(): SpanStyle =
    when (this) {
        SpanStyleDto.TEXT -> SpanStyle.Text
        SpanStyleDto.BOLD -> SpanStyle.Bold
        SpanStyleDto.CODE -> SpanStyle.Code
    }

fun List<CachedNote>.toCategories(format: MyNoteJsonFormat): List<Category> =
    groupBy { it.group }.map { (group, cached) ->
        Category(name = group, notes = cached.map { it.toDomainNote(format) })
    }

fun CachedNote.toDomainNote(format: MyNoteJsonFormat): Note =
    Note(id = key, title = title, date = date.orEmpty(), blocks = cachedBlocks(format).map { it.toDomain() })

private fun CachedNote.cachedBlocks(format: MyNoteJsonFormat): List<NoteBlockDto> =
    try {
        format.decode(blocksJson)
    } catch (_: IllegalArgumentException) {
        raw?.let(format::parse)
            ?: listOf(NoteBlockDto.Unsupported(UnsupportedReasonDto.BROKEN_NOTE, raw = blocksJson))
    }
