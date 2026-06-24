package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.RemoteNotesDataSource
import com.hancekim.mynote.core.data.dto.IndexEntryDto
import com.hancekim.mynote.core.data.dto.NoteBlockDto
import com.hancekim.mynote.core.data.dto.NoteDto
import com.hancekim.mynote.core.data.dto.NoteSpanDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun entry(id: String, hash: String) =
    IndexEntryDto(id = id, group = "android", title = id, date = "2026.01.01", path = "notes/$id.json", hash = hash)

private fun noteDto(id: String) =
    NoteDto(id = id, title = id, date = "2026.01.01", blocks = listOf(NoteBlockDto.Paragraph(listOf(NoteSpanDto("body-$id")))))

private class FakeRemote(
    var index: List<IndexEntryDto>,
    val notes: Map<String, NoteDto>,
) : RemoteNotesDataSource {
    val fetched = mutableListOf<String>()
    override suspend fun getIndex(): List<IndexEntryDto> = index
    override suspend fun getNote(path: String): NoteDto {
        fetched += path
        val id = path.removePrefix("notes/").removeSuffix(".json")
        return notes.getValue(id)
    }
}

private class FakeLocal(initial: List<CachedNote> = emptyList()) : LocalNotesDataSource {
    val store = MutableStateFlow(initial.associateBy { it.id })
    override fun observeNotes(): Flow<List<CachedNote>> = throw UnsupportedOperationException("not used")
    override suspend fun loadHashes(): Map<String, String> = store.value.mapValues { it.value.contentHash }
    override suspend fun upsert(note: CachedNote) = store.update { it + (note.id to note) }
    override suspend fun deleteByIds(ids: List<String>) = store.update { it - ids.toSet() }
}

class NotesReconcileTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun repo(remote: RemoteNotesDataSource, local: LocalNotesDataSource) =
        NotesRepositoryImpl(remote = remote, local = local, json = json)

    @Test
    fun `로컬에 없으면 본문까지 받아 upsert 한다`() = runTest {
        val remote = FakeRemote(index = listOf(entry("a", "h1"), entry("b", "h1")), notes = mapOf("a" to noteDto("a"), "b" to noteDto("b")))
        val local = FakeLocal()

        val result = repo(remote, local).sync()

        assertTrue(result.isSuccess)
        assertEquals(setOf("a", "b"), local.store.value.keys)
        assertEquals(listOf("notes/a.json", "notes/b.json"), remote.fetched)
    }

    @Test
    fun `해시가 같으면 본문을 다시 받지 않는다`() = runTest {
        val cached = CachedNote("a", "android", "a", "2026.01.01", "h1", "[]")
        val remote = FakeRemote(index = listOf(entry("a", "h1")), notes = mapOf("a" to noteDto("a")))
        val local = FakeLocal(listOf(cached))

        repo(remote, local).sync()

        assertTrue(remote.fetched.isEmpty())
    }

    @Test
    fun `해시가 다르면 본문을 다시 받아 갱신한다`() = runTest {
        val cached = CachedNote("a", "android", "a", "2026.01.01", "OLD", "[]")
        val remote = FakeRemote(index = listOf(entry("a", "NEW")), notes = mapOf("a" to noteDto("a")))
        val local = FakeLocal(listOf(cached))

        repo(remote, local).sync()

        assertEquals(listOf("notes/a.json"), remote.fetched)
        assertEquals("NEW", local.store.value.getValue("a").contentHash)
    }

    @Test
    fun `index 에 없는 로컬 노트는 삭제한다`() = runTest {
        val cached = CachedNote("stale", "android", "stale", "2026.01.01", "h1", "[]")
        val remote = FakeRemote(index = listOf(entry("a", "h1")), notes = mapOf("a" to noteDto("a")))
        val local = FakeLocal(listOf(cached))

        repo(remote, local).sync()

        assertEquals(setOf("a"), local.store.value.keys)
    }
}
