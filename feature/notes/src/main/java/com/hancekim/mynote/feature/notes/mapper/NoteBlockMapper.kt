package com.hancekim.mynote.feature.notes.mapper

import com.hancekim.mynote.core.designsystem.block.NoteColumnAlign
import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import com.hancekim.mynote.core.designsystem.block.NoteContentCell
import com.hancekim.mynote.core.designsystem.block.NoteContentListItem
import com.hancekim.mynote.core.designsystem.block.NoteContentSpan
import com.hancekim.mynote.core.designsystem.block.NoteSpanStyle
import com.hancekim.mynote.core.designsystem.block.UnsupportedKind
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.TableAlign
import com.hancekim.mynote.core.domain.model.TableCell
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal fun List<NoteBlock>.toContentBlocks(limits: NoteLimits = NoteLimits.Default): ImmutableList<NoteContentBlock> =
    map { it.toContentBlock(limits) }.toImmutableList()

private fun NoteBlock.toContentBlock(limits: NoteLimits): NoteContentBlock =
    when (this) {
        is NoteBlock.Heading -> NoteContentBlock.Heading(level, spans.toContentSpans())
        is NoteBlock.Paragraph -> NoteContentBlock.Paragraph(spans.toContentSpans())
        is NoteBlock.Code -> NoteContentBlock.Code(code)
        is NoteBlock.Quote -> NoteContentBlock.Quote(spans.toContentSpans())
        is NoteBlock.BulletList -> NoteContentBlock.BulletList(items.toContentItems(limits))
        is NoteBlock.OrderedList -> NoteContentBlock.OrderedList(items.toContentItems(limits), start)
        is NoteBlock.Table -> toContentTable()
        NoteBlock.Divider -> NoteContentBlock.Divider
        NoteBlock.Spacer -> NoteContentBlock.Spacer
        is NoteBlock.Unsupported -> toContentUnsupported(limits)
    }

private fun NoteBlock.Table.toContentTable(): NoteContentBlock.Table =
    NoteContentBlock.Table(
        header = header.toContentCells(),
        rows = rows.map { it.toContentCells() }.toImmutableList(),
        align = align.map { it.toUi() }.toImmutableList(),
    )

private fun NoteBlock.Unsupported.toContentUnsupported(limits: NoteLimits): NoteContentBlock.Unsupported =
    NoteContentBlock.Unsupported(
        kind = reason.toUi(limits),
        salvaged = text?.takeIf { it.isNotBlank() },
        raw = raw.takeIf { it.isNotBlank() },
    )

private fun UnsupportedReason.toUi(limits: NoteLimits): UnsupportedKind =
    when (this) {
        UnsupportedReason.UnknownType -> {
            UnsupportedKind.UnknownType
        }

        UnsupportedReason.BrokenBlock -> {
            UnsupportedKind.BrokenBlock
        }

        UnsupportedReason.BrokenNote -> {
            UnsupportedKind.BrokenNote
        }

        UnsupportedReason.LimitExceeded -> {
            UnsupportedKind.LimitExceeded(limits.maxDepth, limits.maxKilobytes, limits.maxTableCells)
        }
    }

private fun List<NoteListItem>.toContentItems(limits: NoteLimits): ImmutableList<NoteContentListItem> =
    map { item ->
        NoteContentListItem(
            spans = item.spans.toContentSpans(),
            checked = item.checked,
            children = item.children.toChildBlocks(limits),
        )
    }.toImmutableList()

private fun List<NoteBlock>.toChildBlocks(limits: NoteLimits): ImmutableList<NoteContentBlock> =
    mapNotNull { block ->
        if (block is NoteBlock.Unsupported) {
            block.text
                ?.takeIf { it.isNotBlank() }
                ?.let { NoteContentBlock.Paragraph(persistentListOf(NoteContentSpan(it, NoteSpanStyle.Normal))) }
        } else {
            block.toContentBlock(limits)
        }
    }.toImmutableList()

private fun List<TableCell>.toContentCells(): ImmutableList<NoteContentCell> =
    map { NoteContentCell(it.spans.toContentSpans()) }.toImmutableList()

private fun TableAlign.toUi(): NoteColumnAlign =
    when (this) {
        TableAlign.None -> NoteColumnAlign.None
        TableAlign.Left -> NoteColumnAlign.Left
        TableAlign.Center -> NoteColumnAlign.Center
        TableAlign.Right -> NoteColumnAlign.Right
    }

private fun List<NoteSpan>.toContentSpans(): ImmutableList<NoteContentSpan> =
    map { NoteContentSpan(it.text, it.style.toUi(), it.italic, it.strike) }.toImmutableList()

private fun SpanStyle.toUi(): NoteSpanStyle =
    when (this) {
        SpanStyle.Text -> NoteSpanStyle.Normal
        SpanStyle.Bold -> NoteSpanStyle.Bold
        SpanStyle.Code -> NoteSpanStyle.Code
    }
