package com.hancekim.mynote.feature.notes

import com.hancekim.mynote.core.data.model.NoteBlock
import com.hancekim.mynote.core.data.model.NoteListItem
import com.hancekim.mynote.core.data.model.NoteSpan
import com.hancekim.mynote.core.data.model.SpanStyle
import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import com.hancekim.mynote.core.designsystem.block.NoteContentSpan
import com.hancekim.mynote.core.designsystem.block.NoteSpanStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * 데이터 DTO(`:core:data` 의 [NoteBlock]) → design-system UI 모델([NoteContentBlock]) 매핑.
 * presentation(feature) 경계에서 단방향으로 변환한다 — design-system 은 데이터 레이어를 모른다.
 */
fun List<NoteBlock>.toContentBlocks(): ImmutableList<NoteContentBlock> =
    map { it.toContentBlock() }.toImmutableList()

private fun NoteBlock.toContentBlock(): NoteContentBlock = when (this) {
    is NoteBlock.Heading -> NoteContentBlock.Heading(level, spans.toContentSpans())
    is NoteBlock.Paragraph -> NoteContentBlock.Paragraph(spans.toContentSpans())
    is NoteBlock.Code -> NoteContentBlock.Code(code)
    is NoteBlock.Quote -> NoteContentBlock.Quote(spans.toContentSpans())
    is NoteBlock.BulletList -> NoteContentBlock.BulletList(items.toItemSpans())
    is NoteBlock.OrderedList -> NoteContentBlock.OrderedList(items.toItemSpans())
}

private fun List<NoteListItem>.toItemSpans(): ImmutableList<ImmutableList<NoteContentSpan>> =
    map { it.spans.toContentSpans() }.toImmutableList()

private fun List<NoteSpan>.toContentSpans(): ImmutableList<NoteContentSpan> =
    map { NoteContentSpan(it.text, it.style.toUi()) }.toImmutableList()

private fun SpanStyle.toUi(): NoteSpanStyle = when (this) {
    SpanStyle.TEXT -> NoteSpanStyle.Normal
    SpanStyle.BOLD -> NoteSpanStyle.Bold
    SpanStyle.CODE -> NoteSpanStyle.Code
}
