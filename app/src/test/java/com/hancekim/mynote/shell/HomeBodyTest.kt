package com.hancekim.mynote.shell

import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.feature.notes.element.NotesUiState
import org.junit.Assert.assertEquals
import org.junit.Test

/** 홈 판정 시험 가운데 시작 분기(`startOf`)를 지나는 것 — 시작 분기가 `:app` 에 있어 여기 산다(FM-13 같은 클래스 이름). */
class HomeBodyTest {
    @Test
    fun `RC-1 확정 직후에도 연결 있음 홈은 연결 화면이 아니다`() {
        val connected =
            NotesUiState(
                connection = ConnectionState.Connected(SourceId("s1"), repo),
                notesLoaded = true,
                awaitingNotes = true,
            )
        val shell = ShellUiState(connection = connected.connection, seedPending = false)

        assertEquals(StartDestination.Shell, startOf(shell, connected))
    }

    private val repo = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)
}
