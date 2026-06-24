package com.hancekim.mynote.core.data.dto

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import kotlinx.serialization.json.Json

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

/** 캐시 목록 → 카테고리. group 으로 묶고 각 노트의 blocksJson 을 파싱한다. */
fun List<CachedNote>.toCategories(json: Json): List<Category> =
    groupBy { it.group }.map { (group, cached) ->
        Category(name = group, notes = cached.map { it.toDomainNote(json) })
    }

/** 캐시 1건 → 도메인 Note. blocksJson(원본) → List<NoteBlockDto> → 도메인 블록. */
fun CachedNote.toDomainNote(json: Json): Note {
    val blocks = json.decodeFromString<List<NoteBlockDto>>(blocksJson).map { it.toDomain() }
    return Note(id = id, title = title, date = date, blocks = blocks)
}
