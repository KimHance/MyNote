package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBookmarksUseCase @Inject constructor(
    private val repository: BookmarkRepository,
) {
    operator fun invoke(): Flow<List<Bookmark>> = repository.observeBookmarks()
}
