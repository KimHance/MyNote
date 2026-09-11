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

/**
 * notes/index.json 의 한 노트 항목(와이어 포맷). 본문 없이 메타 + GET 경로 + 비교용 해시.
 * index.json 은 group 키 → 이 항목들의 배열인 JSON 오브젝트다:
 *   { "android": [ {id,title,date,path,hash}, … ], "kotlin": [ … ] }
 * group 은 상위 키이므로 항목 자체엔 group 필드가 없다.
 */
@Serializable
data class IndexNoteDto(
    val id: String,
    val title: String,
    val date: String,
    val path: String, // 예: "notes/android/compose-state.json"
    val hash: String, // 데스크탑 write-note 도구가 생성, 앱은 비교만
)

/**
 * 그룹을 항목에 펼쳐 넣은 평탄화 형태(reconcile 내부용). 원격 구현체가 [IndexNoteDto] 맵을
 * group 별로 펼쳐 만든다. RemoteNotesDataSource 인터페이스/reconcile 은 이 평탄형만 본다.
 */
@Serializable
data class IndexEntryDto(
    val id: String,
    val group: String,
    val title: String,
    val date: String,
    val path: String,
    val hash: String,
)
