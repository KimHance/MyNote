package com.hancekim.mynote.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * 노트 캐시 1행. 메타 + 본문(blocksJson) 한 테이블. 본문은 정규화하지 않는다.
 * 이 타입은 :core:database 밖으로 나가지 않는다(경계는 CachedNote).
 */
@Entity(tableName = "notes")
internal data class NoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_name") val group: String,
    val title: String,
    val date: String,
    val contentHash: String,
    val blocksJson: String,
)

/** reconcile 비교용 경량 projection(blob 미포함). */
internal data class NoteHashRow(
    val id: String,
    val contentHash: String,
)
