package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun observeBookmarks(): Flow<List<Bookmark>>

    suspend fun add(bookmark: Bookmark)

    suspend fun remove(noteIds: Collection<String>)
}
