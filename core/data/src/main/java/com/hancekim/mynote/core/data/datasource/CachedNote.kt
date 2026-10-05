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
