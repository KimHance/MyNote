package com.hancekim.mynote.core.designsystem.block

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
        val items: ImmutableList<NoteContentListItem>,
    ) : NoteContentBlock

    /** @property start 첫 항목의 번호 */
    @Immutable
    data class OrderedList(
        val items: ImmutableList<NoteContentListItem>,
        val start: Int = 1,
    ) : NoteContentBlock

    /**
     * @property header 머리 행의 칸
     * @property rows 본문 행마다의 칸
     * @property align 열마다의 정렬
     */
    @Immutable
    data class Table(
        val header: ImmutableList<NoteContentCell>,
        val rows: ImmutableList<ImmutableList<NoteContentCell>>,
        val align: ImmutableList<NoteColumnAlign>,
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

@Immutable
sealed interface UnsupportedKind {
    data object UnknownType : UnsupportedKind

    data object BrokenBlock : UnsupportedKind

    data object BrokenNote : UnsupportedKind

    /**
     * 한도를 넘어 읽지 않은 노트.
     *
     * @property maxDepth 중첩 단수 한도
     * @property maxKilobytes 원문 크기 한도(KB)
     * @property maxTableCells 노트 하나의 표 칸 수 한도
     */
    @Immutable
    data class LimitExceeded(
        val maxDepth: Int,
        val maxKilobytes: Int,
        val maxTableCells: Int,
    ) : UnsupportedKind
}

/**
 * @property checked 체크 항목이면 켬 · 끔, 아니면 null
 * @property children 항목 글 아래에 딸린 블록. 상자는 오지 않는다
 */
@Immutable
data class NoteContentListItem(
    val spans: ImmutableList<NoteContentSpan>,
    val checked: Boolean? = null,
    val children: ImmutableList<NoteContentBlock> = persistentListOf(),
)

@Immutable
data class NoteContentCell(
    val spans: ImmutableList<NoteContentSpan>,
)

enum class NoteColumnAlign { None, Left, Center, Right }

@Immutable
data class NoteContentSpan(
    val text: String,
    val style: NoteSpanStyle,
    val italic: Boolean = false,
    val strike: Boolean = false,
)

enum class NoteSpanStyle { Normal, Bold, Code }
