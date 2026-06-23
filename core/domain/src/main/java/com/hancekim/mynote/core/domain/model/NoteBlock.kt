package com.hancekim.mynote.core.domain.model

/**
 * 노트 본문 블록(도메인 모델). 디자인 스펙이 정의한 타입만.
 * 직렬화 형식(DTO)·UI 모델과 분리된 순수 표현이다.
 */
sealed interface NoteBlock {
    data class Heading(val level: Int, val spans: List<NoteSpan>) : NoteBlock
    data class Paragraph(val spans: List<NoteSpan>) : NoteBlock
    data class Code(val code: String) : NoteBlock
    data class Quote(val spans: List<NoteSpan>) : NoteBlock
    data class BulletList(val items: List<NoteListItem>) : NoteBlock
    data class OrderedList(val items: List<NoteListItem>) : NoteBlock
}

data class NoteListItem(val spans: List<NoteSpan>)

data class NoteSpan(val text: String, val style: SpanStyle = SpanStyle.Text)

enum class SpanStyle { Text, Bold, Code }
