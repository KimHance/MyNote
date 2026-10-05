package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import javax.inject.Inject

class AddBookmarkUseCase(
    private val repository: BookmarkRepository,
    private val now: () -> Long,
) {
    @Inject
    constructor(repository: BookmarkRepository) : this(repository, System::currentTimeMillis)

    suspend operator fun invoke(
        noteId: String,
        addedAt: Long = now(),
    ) = repository.add(Bookmark(noteId, addedAt))
}
