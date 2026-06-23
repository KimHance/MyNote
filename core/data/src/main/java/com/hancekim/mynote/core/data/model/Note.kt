package com.hancekim.mynote.core.data.model

import kotlinx.serialization.Serializable

/**
 * 카테고리 한 묶음. 원격 JSON 의 최상위 배열 원소.
 *
 * 스키마: `[{ "name": ..., "notes": [{ id, title, date, md }] }]`
 * 직렬화 경계라 순수 모델로 유지한다(Compose 의존 없음). stability 는 UI 레이어에서 보증.
 */
@Serializable
data class Category(
    val name: String,
    val notes: List<Note>,
)

/** 노트 한 건. 본문은 서버드리븐 [NoteBlock] 배열(블록 조립). */
@Serializable
data class Note(
    val id: String,
    val title: String,
    /** "2026.05.18" 형태. 정렬·표시에 문자열 그대로 사용(읽기 전용). */
    val date: String,
    val blocks: List<NoteBlock>,
)
