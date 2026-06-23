package com.hancekim.mynote.core.data.dto

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle

/** DTO → 도메인 모델 매핑. 데이터 레이어가 도메인을 의존(DIP)하므로 여기서 변환한다. */
fun CategoryDto.toDomain(): Category = Category(name = name, notes = notes.map { it.toDomain() })

fun NoteDto.toDomain(): Note = Note(id = id, title = title, date = date, blocks = blocks.map { it.toDomain() })

fun NoteBlockDto.toDomain(): NoteBlock = when (this) {
    is NoteBlockDto.Heading -> NoteBlock.Heading(level, spans.toDomainSpans())
    is NoteBlockDto.Paragraph -> NoteBlock.Paragraph(spans.toDomainSpans())
    is NoteBlockDto.Code -> NoteBlock.Code(code)
    is NoteBlockDto.Quote -> NoteBlock.Quote(spans.toDomainSpans())
    is NoteBlockDto.BulletList -> NoteBlock.BulletList(items.map { NoteListItem(it.spans.toDomainSpans()) })
    is NoteBlockDto.OrderedList -> NoteBlock.OrderedList(items.map { NoteListItem(it.spans.toDomainSpans()) })
}

private fun List<NoteSpanDto>.toDomainSpans(): List<NoteSpan> =
    map { NoteSpan(it.text, it.style.toDomain()) }

private fun SpanStyleDto.toDomain(): SpanStyle = when (this) {
    SpanStyleDto.TEXT -> SpanStyle.Text
    SpanStyleDto.BOLD -> SpanStyle.Bold
    SpanStyleDto.CODE -> SpanStyle.Code
}
