package com.hancekim.mynote.feature.notes.mapper

import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import com.hancekim.mynote.core.designsystem.block.NoteContentSpan
import com.hancekim.mynote.core.designsystem.block.NoteSpanStyle
import com.hancekim.mynote.core.designsystem.block.UnsupportedKind
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun List<NoteBlock>.toContentBlocks(): ImmutableList<NoteContentBlock> =
    map { it.toContentBlock() }.toImmutableList()

private fun NoteBlock.toContentBlock(): NoteContentBlock =
    when (this) {
        is NoteBlock.Heading -> NoteContentBlock.Heading(level, spans.toContentSpans())
        is NoteBlock.Paragraph -> NoteContentBlock.Paragraph(spans.toContentSpans())
        is NoteBlock.Code -> NoteContentBlock.Code(code)
        is NoteBlock.Quote -> NoteContentBlock.Quote(spans.toContentSpans())
        is NoteBlock.BulletList -> NoteContentBlock.BulletList(items.toItemSpans())
        is NoteBlock.OrderedList -> NoteContentBlock.OrderedList(items.toItemSpans())
        NoteBlock.Divider -> NoteContentBlock.Divider
        NoteBlock.Spacer -> NoteContentBlock.Spacer
        is NoteBlock.Unsupported -> toContentUnsupported()
    }

private fun NoteBlock.Unsupported.toContentUnsupported(): NoteContentBlock.Unsupported =
    NoteContentBlock.Unsupported(
        kind = reason.toUi(),
        salvaged = text?.takeIf { it.isNotBlank() },
        raw = raw.takeIf { it.isNotBlank() },
    )

private fun UnsupportedReason.toUi(): UnsupportedKind =
    when (this) {
        UnsupportedReason.UnknownType -> UnsupportedKind.UnknownType
        UnsupportedReason.BrokenBlock -> UnsupportedKind.BrokenBlock
        UnsupportedReason.BrokenNote -> UnsupportedKind.BrokenNote
    }

private fun List<NoteListItem>.toItemSpans(): ImmutableList<ImmutableList<NoteContentSpan>> =
    map { it.spans.toContentSpans() }.toImmutableList()

private fun List<NoteSpan>.toContentSpans(): ImmutableList<NoteContentSpan> =
    map { NoteContentSpan(it.text, it.style.toUi()) }.toImmutableList()

private fun SpanStyle.toUi(): NoteSpanStyle =
    when (this) {
        SpanStyle.Text -> NoteSpanStyle.Normal
        SpanStyle.Bold -> NoteSpanStyle.Bold
        SpanStyle.Code -> NoteSpanStyle.Code
    }
