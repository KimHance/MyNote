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

/** 새 기기 노트 행. 키는 기기 소스 id 와 [docId] 로 만든다. */
data class DeviceNoteInsert(
    val docId: String,
    val path: String,
    val group: String,
    val title: String,
    val date: String,
    val epochDay: Long,
    val firstSeenAt: Long,
    val parserVersion: Int,
    val raw: String,
    val blocksJson: String,
    val excerpt: String?,
)

/** 기기 노트를 고쳐 바꿀 열. */
data class DeviceNoteEdit(
    val key: String,
    val group: String,
    val title: String,
    val raw: String,
    val blocksJson: String,
    val excerpt: String?,
    val parserVersion: Int,
)

/** 고치기용으로 읽은 기기 노트. */
data class DeviceNoteRow(
    val key: String,
    val group: String,
    val title: String,
    val date: String?,
    val raw: String,
)
