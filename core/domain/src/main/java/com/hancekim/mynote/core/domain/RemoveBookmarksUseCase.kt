package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import javax.inject.Inject

class RemoveBookmarksUseCase @Inject constructor(
    private val repository: BookmarkRepository,
) {
    suspend operator fun invoke(noteIds: Collection<String>) = repository.remove(noteIds)
}
