package com.hancekim.mynote.core.designsystem.block

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface NoteContentBlock {
    @Immutable
    data class Heading(
        val level: Int,
        val spans: ImmutableList<NoteContentSpan>,
    ) : NoteContentBlock

    @Immutable
    data class Paragraph(
        val spans: ImmutableList<NoteContentSpan>,
    ) : NoteContentBlock

    @Immutable
    data class Code(
        val code: String,
    ) : NoteContentBlock

    @Immutable
    data class Quote(
        val spans: ImmutableList<NoteContentSpan>,
    ) : NoteContentBlock

    @Immutable
    data class BulletList(
        val items: ImmutableList<ImmutableList<NoteContentSpan>>,
    ) : NoteContentBlock

    @Immutable
    data class OrderedList(
        val items: ImmutableList<ImmutableList<NoteContentSpan>>,
    ) : NoteContentBlock

    data object Divider : NoteContentBlock

    data object Spacer : NoteContentBlock

    @Immutable
    data class Unsupported(
        val kind: UnsupportedKind,
        val salvaged: String?,
        val raw: String?,
    ) : NoteContentBlock
}

enum class UnsupportedKind { UnknownType, BrokenBlock, BrokenNote }

@Immutable
data class NoteContentSpan(
    val text: String,
    val style: NoteSpanStyle,
)

enum class NoteSpanStyle { Normal, Bold, Code }
