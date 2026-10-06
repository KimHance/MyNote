package com.hancekim.mynote.core.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

@Serializable
internal sealed interface NoteBlockDto {
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

    @Serializable
    @SerialName("divider")
    data object Divider : NoteBlockDto

    @Serializable
    @SerialName("spacer")
    data object Spacer : NoteBlockDto
}

@Serializable
internal enum class UnsupportedReasonDto {
    @SerialName("unknown_type")
    UNKNOWN_TYPE,

    @SerialName("broken_block")
    BROKEN_BLOCK,

    @SerialName("broken_note")
    BROKEN_NOTE,

    @SerialName("limit_exceeded")
    LIMIT_EXCEEDED,
}

@Serializable
internal data class NoteListItemDto(
    val spans: List<NoteSpanDto>,
)

@Serializable
internal data class NoteSpanDto(
    val text: String,
    val style: SpanStyleDto = SpanStyleDto.TEXT,
)

@Serializable
internal enum class SpanStyleDto {
    @SerialName("text")
    TEXT,

    @SerialName("bold")
    BOLD,

    @SerialName("code")
    CODE,
}

internal val BlockJson: Json =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        serializersModule =
            SerializersModule {
                polymorphicDefaultDeserializer(NoteBlockDto::class) { NoteBlockDto.Unknown.serializer() }
            }
    }
