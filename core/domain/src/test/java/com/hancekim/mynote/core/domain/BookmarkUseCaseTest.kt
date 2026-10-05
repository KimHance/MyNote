package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeBookmarkRepository(
    initial: List<Bookmark> = emptyList(),
) : BookmarkRepository {
    val store = MutableStateFlow(initial.associateBy { it.noteId })

    override fun observeBookmarks(): Flow<List<Bookmark>> = MutableStateFlow(store.value.values.toList())

    override suspend fun add(bookmark: Bookmark) {
        store.update { it + (bookmark.noteId to bookmark) }
    }

    override suspend fun remove(noteIds: Collection<String>) {
        store.update { it - noteIds.toSet() }
    }
}

class BookmarkUseCaseTest {
    @Test
    fun `추가하면 주입한 시계의 값이 addedAt 이 된다`() =
        runTest {
            val repository = FakeBookmarkRepository()
            val add = AddBookmarkUseCase(repository) { 1_700_000_000_000L }

            add("tcp")

            assertEquals(Bookmark("tcp", 1_700_000_000_000L), repository.store.value["tcp"])
        }

    @Test
    fun `addedAt 을 직접 주면 시계 대신 그 값을 쓴다 - 복원 경로`() =
        runTest {
            val repository = FakeBookmarkRepository()
            val add = AddBookmarkUseCase(repository) { 999L }

            add("tcp", addedAt = 42L)

            assertEquals(
                42L,
                repository.store.value
                    .getValue("tcp")
                    .addedAt,
            )
        }

    @Test
    fun `같은 id 를 다시 추가하면 addedAt 이 덮어써진다`() =
        runTest {
            val repository = FakeBookmarkRepository(listOf(Bookmark("tcp", 1L)))
            val add = AddBookmarkUseCase(repository) { 2L }

            add("tcp")

            assertEquals(
                2L,
                repository.store.value
                    .getValue("tcp")
                    .addedAt,
            )
        }

    @Test
    fun `여러 id 를 한 번에 지우고 없는 id 는 무시한다`() =
        runTest {
            val repository = FakeBookmarkRepository(listOf(Bookmark("a", 1L), Bookmark("b", 2L), Bookmark("c", 3L)))
            val remove = RemoveBookmarksUseCase(repository)

            remove(listOf("a", "c", "ghost"))

            assertEquals(setOf("b"), repository.store.value.keys)
        }
}
