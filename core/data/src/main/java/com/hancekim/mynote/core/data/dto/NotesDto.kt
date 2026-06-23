package com.hancekim.mynote.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 직렬화 DTO(데이터 레이어 형식). 원격 JSON 스키마와 1:1.
 * 도메인 모델(`:core:domain`)로 매핑되어 바깥으로 나간다([toDomain]).
 */
@Serializable
data class CategoryDto(
    val name: String,
    val notes: List<NoteDto>,
)

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    val date: String,
    val blocks: List<NoteBlockDto>,
)

@Serializable
sealed interface NoteBlockDto {
    @Serializable
    @SerialName("heading")
    data class Heading(val level: Int, val spans: List<NoteSpanDto>) : NoteBlockDto

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(val spans: List<NoteSpanDto>) : NoteBlockDto

    @Serializable
    @SerialName("code")
    data class Code(val code: String) : NoteBlockDto

    @Serializable
    @SerialName("quote")
    data class Quote(val spans: List<NoteSpanDto>) : NoteBlockDto

    @Serializable
    @SerialName("bulletList")
    data class BulletList(val items: List<NoteListItemDto>) : NoteBlockDto

    @Serializable
    @SerialName("orderedList")
    data class OrderedList(val items: List<NoteListItemDto>) : NoteBlockDto
}

@Serializable
data class NoteListItemDto(val spans: List<NoteSpanDto>)

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
