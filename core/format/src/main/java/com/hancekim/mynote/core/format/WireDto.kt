package com.hancekim.mynote.core.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface WireBlockDto {
    @Serializable
    @SerialName("heading")
    data class Heading(
        val level: Int,
        val spans: List<WireSpanDto>,
    ) : WireBlockDto

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        val spans: List<WireSpanDto>,
    ) : WireBlockDto

    @Serializable
    @SerialName("code")
    data class Code(
        val code: String,
    ) : WireBlockDto

    @Serializable
    @SerialName("quote")
    data class Quote(
        val spans: List<WireSpanDto>,
    ) : WireBlockDto

    @Serializable
    @SerialName("bulletList")
    data class BulletList(
        val items: List<WireListItemDto>,
    ) : WireBlockDto

    @Serializable
    @SerialName("orderedList")
    data class OrderedList(
        val items: List<WireListItemDto>,
    ) : WireBlockDto

    @Serializable
    @SerialName("divider")
    data object Divider : WireBlockDto

    @Serializable
    @SerialName("spacer")
    data object Spacer : WireBlockDto
}

@Serializable
internal data class WireListItemDto(
    val spans: List<WireSpanDto>,
)

@Serializable
internal data class WireSpanDto(
    val text: String,
    val style: WireStyleDto = WireStyleDto.TEXT,
)

@Serializable
internal enum class WireStyleDto {
    @SerialName("text")
    TEXT,

    @SerialName("bold")
    BOLD,

    @SerialName("code")
    CODE,
}
