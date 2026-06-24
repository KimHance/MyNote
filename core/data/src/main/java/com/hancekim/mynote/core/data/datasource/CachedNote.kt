package com.hancekim.mynote.core.data.datasource

/**
 * 로컬 캐시(Room)와 data 레이어 사이의 경계 교차 타입.
 * Room `Entity` 는 `:core:database` 밖으로 안 나가므로, database 가 Entity↔CachedNote 로 매핑한다.
 * 본문은 정규화하지 않고 [blocksJson] 원본 문자열로 보관한다(파싱은 data Mapper).
 */
data class CachedNote(
    val id: String,
    val group: String,
    val title: String,
    val date: String,
    val contentHash: String,
    val blocksJson: String,
)
