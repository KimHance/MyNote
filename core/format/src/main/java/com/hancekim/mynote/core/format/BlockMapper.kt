package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.TableAlign
import com.hancekim.mynote.core.domain.model.TableCell
import com.hancekim.mynote.core.domain.model.UnsupportedReason

internal fun WireBlockDto.toCache(): NoteBlockDto =
    when (this) {
        is WireBlockDto.Heading -> NoteBlockDto.Heading(level, spans.wireToCache())
        is WireBlockDto.Paragraph -> NoteBlockDto.Paragraph(spans.wireToCache())
        is WireBlockDto.Code -> NoteBlockDto.Code(code)
        is WireBlockDto.Quote -> NoteBlockDto.Quote(spans.wireToCache())
        is WireBlockDto.BulletList -> NoteBlockDto.BulletList(items.map { NoteListItemDto(it.spans.wireToCache()) })
        is WireBlockDto.OrderedList -> NoteBlockDto.OrderedList(items.map { NoteListItemDto(it.spans.wireToCache()) })
        WireBlockDto.Divider -> NoteBlockDto.Divider
        WireBlockDto.Spacer -> NoteBlockDto.Spacer
    }

private fun List<WireSpanDto>.wireToCache(): List<NoteSpanDto> =
    map {
        NoteSpanDto(
            it.text,
            when (it.style) {
                WireStyleDto.TEXT -> SpanStyleDto.TEXT
                WireStyleDto.BOLD -> SpanStyleDto.BOLD
                WireStyleDto.CODE -> SpanStyleDto.CODE
            },
        )
    }

internal fun NoteBlockDto.toDomain(): NoteBlock =
    when (this) {
        NoteBlockDto.Unknown -> NoteBlock.Unsupported(UnsupportedReason.UnknownType, type = null, text = null, raw = "")
        is NoteBlockDto.Unsupported -> NoteBlock.Unsupported(reason.toDomain(), original, text, raw)
        is NoteBlockDto.Heading -> NoteBlock.Heading(level, spans.toDomainSpans())
        is NoteBlockDto.Paragraph -> NoteBlock.Paragraph(spans.toDomainSpans())
        is NoteBlockDto.Code -> NoteBlock.Code(code)
        is NoteBlockDto.Quote -> NoteBlock.Quote(spans.toDomainSpans())
        is NoteBlockDto.BulletList -> NoteBlock.BulletList(items.map { it.toDomain() })
        is NoteBlockDto.OrderedList -> NoteBlock.OrderedList(items.map { it.toDomain() }, start)
        is NoteBlockDto.Table -> toDomainTable()
        NoteBlockDto.Divider -> NoteBlock.Divider
        NoteBlockDto.Spacer -> NoteBlock.Spacer
    }

private fun NoteBlockDto.Table.toDomainTable(): NoteBlock.Table =
    NoteBlock.Table(
        header = header.map { it.toDomain() },
        rows = rows.map { row -> row.map { it.toDomain() } },
        align = align.map { it.toDomain() },
    )

private fun NoteListItemDto.toDomain(): NoteListItem =
    NoteListItem(spans.toDomainSpans(), checked, children.map { it.toDomain() })

private fun TableCellDto.toDomain(): TableCell = TableCell(spans.toDomainSpans())

private fun TableAlignDto.toDomain(): TableAlign =
    when (this) {
        TableAlignDto.NONE -> TableAlign.None
        TableAlignDto.LEFT -> TableAlign.Left
        TableAlignDto.CENTER -> TableAlign.Center
        TableAlignDto.RIGHT -> TableAlign.Right
    }

internal val NoteBlockDto.coversNote: Boolean
    get() = this is NoteBlockDto.Unsupported && reason.toDomain().wholeNote

private fun UnsupportedReasonDto.toDomain(): UnsupportedReason =
    when (this) {
        UnsupportedReasonDto.UNKNOWN_TYPE -> UnsupportedReason.UnknownType
        UnsupportedReasonDto.BROKEN_BLOCK -> UnsupportedReason.BrokenBlock
        UnsupportedReasonDto.BROKEN_NOTE -> UnsupportedReason.BrokenNote
        UnsupportedReasonDto.LIMIT_EXCEEDED -> UnsupportedReason.LimitExceeded
    }

private fun List<NoteSpanDto>.toDomainSpans(): List<NoteSpan> =
    map { NoteSpan(it.text, it.style.toDomain(), it.italic, it.strike) }

private fun SpanStyleDto.toDomain(): SpanStyle =
    when (this) {
        SpanStyleDto.TEXT -> SpanStyle.Text
        SpanStyleDto.BOLD -> SpanStyle.Bold
        SpanStyleDto.CODE -> SpanStyle.Code
    }

internal fun NoteBlock.toCache(): NoteBlockDto =
    when (this) {
        is NoteBlock.Heading -> NoteBlockDto.Heading(level, spans.toCacheSpans())
        is NoteBlock.Paragraph -> NoteBlockDto.Paragraph(spans.toCacheSpans())
        is NoteBlock.Code -> NoteBlockDto.Code(code)
        is NoteBlock.Quote -> NoteBlockDto.Quote(spans.toCacheSpans())
        is NoteBlock.BulletList -> NoteBlockDto.BulletList(items.map { it.toCache() })
        is NoteBlock.OrderedList -> NoteBlockDto.OrderedList(items.map { it.toCache() }, start)
        is NoteBlock.Table -> toCacheTable()
        NoteBlock.Divider -> NoteBlockDto.Divider
        NoteBlock.Spacer -> NoteBlockDto.Spacer
        is NoteBlock.Unsupported -> NoteBlockDto.Unsupported(reason.toCache(), type, text, raw)
    }

private fun NoteBlock.Table.toCacheTable(): NoteBlockDto.Table =
    NoteBlockDto.Table(
        header = header.map { it.toCache() },
        rows = rows.map { row -> row.map { it.toCache() } },
        align = align.map { it.toCache() },
    )

private fun NoteListItem.toCache(): NoteListItemDto =
    NoteListItemDto(spans.toCacheSpans(), checked, children.map { it.toCache() })

private fun TableCell.toCache(): TableCellDto = TableCellDto(spans.toCacheSpans())

private fun TableAlign.toCache(): TableAlignDto =
    when (this) {
        TableAlign.None -> TableAlignDto.NONE
        TableAlign.Left -> TableAlignDto.LEFT
        TableAlign.Center -> TableAlignDto.CENTER
        TableAlign.Right -> TableAlignDto.RIGHT
    }

private fun UnsupportedReason.toCache(): UnsupportedReasonDto =
    when (this) {
        UnsupportedReason.UnknownType -> UnsupportedReasonDto.UNKNOWN_TYPE
        UnsupportedReason.BrokenBlock -> UnsupportedReasonDto.BROKEN_BLOCK
        UnsupportedReason.BrokenNote -> UnsupportedReasonDto.BROKEN_NOTE
        UnsupportedReason.LimitExceeded -> UnsupportedReasonDto.LIMIT_EXCEEDED
    }

private fun List<NoteSpan>.toCacheSpans(): List<NoteSpanDto> =
    map {
        NoteSpanDto(
            it.text,
            when (it.style) {
                SpanStyle.Text -> SpanStyleDto.TEXT
                SpanStyle.Bold -> SpanStyleDto.BOLD
                SpanStyle.Code -> SpanStyleDto.CODE
            },
            it.italic,
            it.strike,
        )
    }
