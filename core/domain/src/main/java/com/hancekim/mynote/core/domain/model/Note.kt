package com.hancekim.mynote.core.domain.model

/**
 * 도메인 모델(순수 Kotlin). 직렬화/Compose 의존 없음 — 가장 안쪽 레이어.
 * data 레이어의 DTO 가 이 모델로 매핑되고, feature 는 이 모델만 본다.
 */
data class Category(
    val name: String,
    val notes: List<Note>,
)

data class Note(
    val id: String,
    val title: String,
    val date: String,
    val blocks: List<NoteBlock>,
)
