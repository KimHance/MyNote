package com.hancekim.mynote.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NoteBlockDto {
    @Serializable
    @SerialName("unknown")
    data object Unknown : NoteBlockDto

    @Serializable
    @SerialName("unsupported")
    data class Unsupported(
        val reason: UnsupportedReasonDto = UnsupportedReasonDto.BROKEN_BLOCK,
        val original: String? = null,
        val text: String? = null,
        val raw: String,
    ) : NoteBlockDto

    @Serializable
    @SerialName("heading")
    data class Heading(
        val level: Int,
        val spans: List<NoteSpanDto>,
    ) : NoteBlockDto

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        val spans: List<NoteSpanDto>,
    ) : NoteBlockDto

    @Serializable
    @SerialName("code")
    data class Code(
        val code: String,
    ) : NoteBlockDto

    @Serializable
    @SerialName("quote")
    data class Quote(
        val spans: List<NoteSpanDto>,
    ) : NoteBlockDto

    @Serializable
    @SerialName("bulletList")
    data class BulletList(
        val items: List<NoteListItemDto>,
    ) : NoteBlockDto

    @Serializable
    @SerialName("orderedList")
    data class OrderedList(
        val items: List<NoteListItemDto>,
    ) : NoteBlockDto

    /** 섹션 구분선(`---`). 필드 없음. */
    @Serializable
    @SerialName("divider")
    data object Divider : NoteBlockDto

    /** 단순 줄바꿈/여백. 필드 없음. */
    @Serializable
    @SerialName("spacer")
    data object Spacer : NoteBlockDto
}

@Serializable
enum class UnsupportedReasonDto {
    @SerialName("unknown_type")
    UNKNOWN_TYPE,

    @SerialName("broken_block")
    BROKEN_BLOCK,

    @SerialName("broken_note")
    BROKEN_NOTE,
}

@Serializable
data class NoteListItemDto(
    val spans: List<NoteSpanDto>,
)

@Serializable
data class NoteSpanDto(
    val text: String,
    val style: SpanStyleDto = SpanStyleDto.TEXT,
)

@Serializable
enum class SpanStyleDto {
    @SerialName("text")
    TEXT,

    @SerialName("bold")
    BOLD,

    @SerialName("code")
    CODE,
}

@Serializable
data class IndexNoteDto(
    val id: String,
    val title: String,
    val date: String,
    val path: String,
    val hash: String,
)
