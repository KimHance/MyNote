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

    /** @property start 첫 항목의 번호 */
    data class OrderedList(
        val items: List<NoteListItem>,
        val start: Int = 1,
    ) : NoteBlock

    /**
     * @property header 머리 행의 칸
     * @property rows 본문 행마다의 칸
     * @property align 열마다의 정렬
     */
    data class Table(
        val header: List<TableCell>,
        val rows: List<List<TableCell>>,
        val align: List<TableAlign>,
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

/** @property wholeNote 노트 전체를 대신하는 상자의 사유인지 */
enum class UnsupportedReason(
    val wholeNote: Boolean,
) {
    UnknownType(wholeNote = false),
    BrokenBlock(wholeNote = false),
    BrokenNote(wholeNote = true),
    LimitExceeded(wholeNote = true),
}

/**
 * @property checked 체크 항목이면 켬 · 끔, 아니면 null
 * @property children 항목 글 아래에 딸린 블록
 */
data class NoteListItem(
    val spans: List<NoteSpan>,
    val checked: Boolean? = null,
    val children: List<NoteBlock> = emptyList(),
)

data class TableCell(
    val spans: List<NoteSpan>,
)

enum class TableAlign { None, Left, Center, Right }

data class NoteSpan(
    val text: String,
    val style: SpanStyle = SpanStyle.Text,
    val italic: Boolean = false,
    val strike: Boolean = false,
)

enum class SpanStyle { Text, Bold, Code }
