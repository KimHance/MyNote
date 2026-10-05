package com.hancekim.mynote.core.data.datasource

import kotlinx.coroutines.flow.Flow

interface BookmarkStore {
    fun observe(): Flow<Map<String, Long>>

    suspend fun put(
        noteId: String,
        addedAt: Long,
    )

    suspend fun remove(noteIds: Collection<String>)

    suspend fun countByPrefix(prefix: String): Int

    suspend fun removeByPrefix(prefix: String)
}
