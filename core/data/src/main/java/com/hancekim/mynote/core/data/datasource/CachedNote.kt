package com.hancekim.mynote.core.data.datasource

data class CachedNote(
    val key: String,
    val sourceId: String,
    val docId: String,
    val path: String?,
    val group: String,
    val title: String,
    val date: String?,
    val epochDay: Long?,
    val firstSeenAt: Long,
    val revision: String?,
    val fetchedAt: Long?,
    val parserVersion: Int,
    val raw: String?,
    val blocksJson: String,
    val excerpt: String?,
)

data class CachedNoteState(
    val revision: String?,
    val hasRaw: Boolean,
    val firstSeenAt: Long,
)

/** 목록에 쓰는 캐시 행. 원문은 [LocalNotesDataSource.loadRaw] 로 따로 읽는다. */
data class ListedNote(
    val key: String,
    val sourceId: String,
    val path: String?,
    val group: String,
    val title: String,
    val date: String?,
    val blocksJson: String,
)

/** 다시 해석할 캐시 행. */
data class StaleNote(
    val key: String,
    val path: String?,
    val raw: String,
)

/** 다시 해석해 바꿀 파생 열. */
data class RederivedNote(
    val key: String,
    val blocksJson: String,
    val excerpt: String?,
    val parserVersion: Int,
)
