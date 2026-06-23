package com.hancekim.mynote.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 서버드리븐 노트 본문 = "블록(섹션) 배열". 디자인 스펙이 정의한 타입만 허용한다.
 * JSON 은 `type` discriminator 로 구분된다(예: `{ "type": "heading", "level": 2, "spans": [...] }`).
 */
@Serializable
sealed interface NoteBlock {

    /** 본문 헤딩. level 은 2(H2) 또는 3(H3). */
    @Serializable
    @SerialName("heading")
    data class Heading(
        val level: Int,
        val spans: List<NoteSpan>,
    ) : NoteBlock

    /** 일반 단락. */
    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        val spans: List<NoteSpan>,
    ) : NoteBlock

    /** 코드블록(언어 라벨/복사 버튼 없이 코드만). */
    @Serializable
    @SerialName("code")
    data class Code(
        val code: String,
    ) : NoteBlock

    /** 인용. */
    @Serializable
    @SerialName("quote")
    data class Quote(
        val spans: List<NoteSpan>,
    ) : NoteBlock

    /** 불릿 리스트. */
    @Serializable
    @SerialName("bulletList")
    data class BulletList(
        val items: List<NoteListItem>,
    ) : NoteBlock

    /** 번호 리스트. */
    @Serializable
    @SerialName("orderedList")
    data class OrderedList(
        val items: List<NoteListItem>,
    ) : NoteBlock
}

/** 리스트 한 항목(인라인 스팬들). */
@Serializable
data class NoteListItem(
    val spans: List<NoteSpan>,
)

/**
 * 인라인 조각. 디자인 스펙이 준 인라인만: 일반/굵게/인라인코드.
 * 예: `{ "text": "상태", "style": "bold" }`. style 생략 시 [SpanStyle.TEXT].
 */
@Serializable
data class NoteSpan(
    val text: String,
    val style: SpanStyle = SpanStyle.TEXT,
)

@Serializable
enum class SpanStyle {
    @SerialName("text")
    TEXT,

    @SerialName("bold")
    BOLD,

    @SerialName("code")
    CODE,
}
