package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.BookmarkStore
import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookmarkRepositoryImpl @Inject constructor(
    private val store: BookmarkStore,
) : BookmarkRepository {
    override fun observeBookmarks(): Flow<List<Bookmark>> =
        store.observe().map { entries -> entries.map { (id, addedAt) -> Bookmark(id, addedAt) } }

    override suspend fun add(bookmark: Bookmark) = store.put(bookmark.noteId, bookmark.addedAt)

    override suspend fun remove(noteIds: Collection<String>) = store.remove(noteIds)
}
