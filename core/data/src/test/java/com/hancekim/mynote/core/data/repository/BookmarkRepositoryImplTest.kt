package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.BookmarkStore
import com.hancekim.mynote.core.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkRepositoryImplTest {
    @Test
    fun `저장소의 맵을 정렬 없이 Bookmark 목록으로 옮긴다`() =
        runTest {
            val store = FakeBookmarkStore(mapOf("a" to 10L, "b" to 5L))
            val repository = BookmarkRepositoryImpl(store)

            val bookmarks = repository.observeBookmarks().first()

            assertEquals(setOf(Bookmark("a", 10L), Bookmark("b", 5L)), bookmarks.toSet())
        }

    @Test
    fun `추가는 id 를 키로 addedAt 을 쓴다`() =
        runTest {
            val store = FakeBookmarkStore()
            val repository = BookmarkRepositoryImpl(store)

            repository.add(Bookmark("tcp", 77L))

            assertEquals(mapOf("tcp" to 77L), store.entries.value)
        }

    @Test
    fun `동기화가 지운 id 목록을 넘기면 그 항목만 빠진다`() =
        runTest {
            val store = FakeBookmarkStore(mapOf("a" to 1L, "b" to 2L, "c" to 3L))
            val repository = BookmarkRepositoryImpl(store)

            repository.remove(listOf("a", "c"))

            assertEquals(listOf(Bookmark("b", 2L)), repository.observeBookmarks().first())
        }
}
