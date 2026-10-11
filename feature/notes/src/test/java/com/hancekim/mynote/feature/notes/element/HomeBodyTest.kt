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
        assertEquals(HomeBody.NoNotes, homeBodyOf(connected = false, noteCount = 0, syncing = true))
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

    private val repo = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)
}
