package com.hancekim.mynote.core.domain.model

sealed interface NoteBlock {
    data class Heading(
        val level: Int,
        val spans: List<NoteSpan>,
    ) : NoteBlock

    data class Paragraph(
        val spans: List<NoteSpan>,
    ) : NoteBlock

    data class Code(
        val code: String,
    ) : NoteBlock

    data class Quote(
        val spans: List<NoteSpan>,
    ) : NoteBlock

    data class BulletList(
        val items: List<NoteListItem>,
    ) : NoteBlock

    data class OrderedList(
        val items: List<NoteListItem>,
    ) : NoteBlock

    data object Divider : NoteBlock

    data object Spacer : NoteBlock

    data class Unsupported(
        val reason: UnsupportedReason,
        val type: String?,
        val text: String?,
        val raw: String,
    ) : NoteBlock
}

enum class UnsupportedReason { UnknownType, BrokenBlock, BrokenNote }

data class NoteListItem(
    val spans: List<NoteSpan>,
)

data class NoteSpan(
    val text: String,
    val style: SpanStyle = SpanStyle.Text,
)

enum class SpanStyle { Text, Bold, Code }
