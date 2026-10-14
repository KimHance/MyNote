package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeBodyTest {
    @Test
    fun `RC-21 홈 빈 상태 판정`() {
        assertEquals(HomeBody.Receiving, homeBodyOf(connected = true, noteCount = 0, syncing = true))
        assertEquals(HomeBody.NoNotes, homeBodyOf(connected = true, noteCount = 0, syncing = false))
        assertEquals(HomeBody.Notes, homeBodyOf(connected = true, noteCount = 3, syncing = true))
        assertEquals(HomeBody.Notes, homeBodyOf(connected = true, noteCount = 3, syncing = false))
    }

    @Test
    fun `RC-21 두 빈 상태는 서로 다르다`() {
        val receiving = homeBodyOf(connected = true, noteCount = 0, syncing = true)
        val none = homeBodyOf(connected = true, noteCount = 0, syncing = false)

        assertEquals(false, receiving == none)
    }

    @Test
    fun `RC-21 연결 없음의 빈 홈은 받는 중이 아니다`() {
        assertEquals(HomeBody.FirstRun, homeBodyOf(connected = false, noteCount = 0, syncing = true))
        assertEquals(HomeBody.Notes, homeBodyOf(connected = false, noteCount = 4, syncing = false))
    }

    @Test
    fun `RC-21 확정 직후 노트가 아직 안 왔으면 노트 없음이 아니라 받는 중이다`() {
        val connected =
            NotesUiState(
                connection = ConnectionState.Connected(SourceId("s1"), repo),
                notesLoaded = true,
                isLoading = false,
            )

        assertEquals(HomeBody.NoNotes, connected.homeBody)
        assertEquals(HomeBody.Receiving, connected.copy(awaitingNotes = true).homeBody)
        assertEquals(HomeBody.Receiving, connected.copy(isLoading = true).homeBody)
    }

    @Test
    fun `LN-1 연결 없음 노트 0 은 첫 실행 빈 상태다`() {
        assertEquals(HomeBody.FirstRun, homeBodyOf(connected = false, noteCount = 0, syncing = false))
        assertEquals(HomeBody.FirstRun, homeBodyOf(connected = false, noteCount = 0, syncing = true))
        assertEquals(
            HomeBody.FirstRun,
            NotesUiState(connection = ConnectionState.None, notesLoaded = true, isLoading = false).homeBody,
        )
    }

    @Test
    fun `LN-1 첫 실행 빈 상태는 받는 중 노트 없음과 다르다`() {
        val firstRun = homeBodyOf(connected = false, noteCount = 0, syncing = false)
        val receiving = homeBodyOf(connected = true, noteCount = 0, syncing = true)
        val none = homeBodyOf(connected = true, noteCount = 0, syncing = false)

        assertEquals(3, setOf(firstRun, receiving, none).size)
        assertEquals(HomeBody.Receiving, receiving)
        assertEquals(HomeBody.NoNotes, none)
    }

    @Test
    fun `LN-1 강조 버튼은 새 노트 하나다`() {
        val entries = homeEntriesOf(HomeBody.FirstRun)

        assertEquals(
            listOf(
                HomeEntry(HomeAction.NewNote, EntryWeight.Primary),
                HomeEntry(HomeAction.Import, EntryWeight.Secondary),
                HomeEntry(HomeAction.Connect, EntryWeight.Quiet),
            ),
            entries,
        )
        assertEquals(listOf(HomeAction.NewNote), entries.filter { it.weight == EntryWeight.Primary }.map { it.action })
        assertEquals(entries.size, entries.map { it.weight }.toSet().size)
    }

    @Test
    fun `LN-2 두 입구는 홈 상태별 표대로 보인다`() {
        val table =
            mapOf(
                HomeBody.FirstRun to setOf(HomeAction.NewNote, HomeAction.Import, HomeAction.Connect),
                HomeBody.Notes to setOf(HomeAction.NewNote, HomeAction.Import),
                HomeBody.Receiving to setOf(HomeAction.NewNote, HomeAction.Import),
                HomeBody.NoNotes to setOf(HomeAction.NewNote, HomeAction.Import),
            )
        assertEquals(HomeBody.entries.toSet(), table.keys)
        table.forEach { (body, actions) ->
            val entries = homeEntriesOf(body)
            assertEquals("$body", actions, entries.map { it.action }.toSet())
            if (body != HomeBody.FirstRun) {
                assertEquals("$body 는 보조 버튼 둘", setOf(EntryWeight.Secondary), entries.map { it.weight }.toSet())
            }
        }
    }

    private val repo = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)
}
