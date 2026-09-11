package com.hancekim.mynote.core.designsystem.block

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * 블록 렌더러([NoteContent])의 입력이 되는 **UI 모델**.
 *
 * 데이터 레이어의 직렬화 DTO(`:core:data` 의 `NoteBlock`)와 분리한다 —
 * design-system 은 데이터 레이어를 모르고, DTO→UI 매핑은 presentation(feature)에서 한다(단방향).
 * 모든 컬렉션이 `ImmutableList` 라 deeply immutable → `@Immutable`.
 */
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

    // object 는 본래 stable 이라 @Immutable 을 붙이지 않는다.

    /** 섹션 구분선(`---`). 필드 없음. */
    data object Divider : NoteContentBlock

    /** 단순 줄바꿈/여백. 필드 없음. */
    data object Spacer : NoteContentBlock
}

/** 인라인 조각(UI). */
@Immutable
data class NoteContentSpan(
    val text: String,
    val style: NoteSpanStyle,
)

enum class NoteSpanStyle { Normal, Bold, Code }
