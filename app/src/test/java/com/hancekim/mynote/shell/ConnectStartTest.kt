package com.hancekim.mynote.shell

import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.element.NotesUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectStartTest {
    @Test
    fun `RC-1 시작 화면 판정 9칸`() {
        val expected =
            mapOf(
                (ConnectionKnown.Unknown to NotesKnown.Unknown) to StartDestination.Reading,
                (ConnectionKnown.Unknown to NotesKnown.Empty) to StartDestination.Reading,
                (ConnectionKnown.Unknown to NotesKnown.Some) to StartDestination.Reading,
                (ConnectionKnown.None to NotesKnown.Unknown) to StartDestination.Reading,
                (ConnectionKnown.None to NotesKnown.Empty) to StartDestination.ConnectRoot,
                (ConnectionKnown.None to NotesKnown.Some) to StartDestination.Shell,
                (ConnectionKnown.Connected to NotesKnown.Unknown) to StartDestination.Reading,
                (ConnectionKnown.Connected to NotesKnown.Empty) to StartDestination.Shell,
                (ConnectionKnown.Connected to NotesKnown.Some) to StartDestination.Shell,
            )

        assertEquals(9, expected.size)
        expected.forEach { (cell, destination) ->
            assertEquals(
                "연결 ${cell.first} · 노트 ${cell.second}",
                destination,
                startDestination(cell.first, cell.second, seedPending = false),
            )
        }
    }

    @Test
    fun `RC-1 시드 소비 전에는 읽는 중이다`() {
        assertEquals(
            StartDestination.Reading,
            startDestination(ConnectionKnown.None, NotesKnown.Empty, seedPending = true),
        )
        assertEquals(
            StartDestination.Reading,
            startDestination(ConnectionKnown.None, NotesKnown.Some, seedPending = true),
        )
        assertEquals(
            StartDestination.ConnectRoot,
            startDestination(ConnectionKnown.None, NotesKnown.Empty, seedPending = false),
        )
    }

    @Test
    fun `RC-1 연결 있음은 시드 소비 전이어도 셸이다`() {
        assertEquals(
            StartDestination.Shell,
            startDestination(ConnectionKnown.Connected, NotesKnown.Some, seedPending = true),
        )
    }

    @Test
    fun `RC-23 끊으면 연결 화면 루트다`() {
        val connected = connected("s1", "octo-learner", "android-notes")

        val cleanup = screenCleanupOf(connected, ConnectionState.None)

        assertEquals(ScreenCleanup(resetStack = true, dismissQuiz = true), cleanup)
        assertEquals(
            StartDestination.ConnectRoot,
            startDestination(ConnectionKnown.None, NotesKnown.Empty, seedPending = false),
        )
    }

    @Test
    fun `RC-23 확정되면 종류와 무관하게 스택은 홈 하나다`() {
        val old = connected("s1", "octo-learner", "android-notes")
        val replaced = connected("s2", "octo-learner", "kotlin-notes")
        val sameSource = connected("s1", "octo-learner", "android-notes", branch = "dev")

        assertEquals(true, screenCleanupOf(old, replaced)?.resetStack)
        assertEquals(true, screenCleanupOf(ConnectionState.None, old)?.resetStack)
        assertEquals(true, screenCleanupOf(old, sameSource)?.resetStack)
        assertEquals(true, screenCleanupOf(old, ConnectionState.None)?.resetStack)
    }

    @Test
    fun `RC-23 노트가 지워지면 열린 노트와 퀴즈가 닫힌다`() {
        val old = connected("s1", "octo-learner", "android-notes")
        val replaced = connected("s2", "octo-learner", "kotlin-notes")

        assertEquals(ScreenCleanup(resetStack = true, dismissQuiz = true), screenCleanupOf(old, replaced))
        assertEquals(
            ScreenCleanup(resetStack = true, dismissQuiz = true),
            screenCleanupOf(ConnectionState.None, old),
        )
    }

    @Test
    fun `RC-23 같은 저장소 변경에서는 노트가 지워지지 않아 퀴즈를 닫지 않는다`() {
        val old = connected("s1", "octo-learner", "android-notes")
        val sameSource = connected("s1", "octo-learner", "android-notes", branch = "dev")

        assertEquals(ScreenCleanup(resetStack = true, dismissQuiz = false), screenCleanupOf(old, sameSource))
    }

    @Test
    fun `RC-23 앱을 켠 첫 연결 상태와 같은 연결의 되풀이는 화면을 정리하지 않는다`() {
        val old = connected("s1", "octo-learner", "android-notes")

        assertEquals(null, screenCleanupOf(null, old))
        assertEquals(null, screenCleanupOf(old, old))
    }

    @Test
    fun `RC-1 로딩 직후 recentNotes 가 비어 있어도 노트 있음은 categories 와 같은 원천에서 낸다`() {
        val shell = ShellUiState(connection = ConnectionState.None, seedPending = false)
        val withNotes =
            NotesUiState(
                connection = ConnectionState.None,
                notesLoaded = true,
                categories = persistentListOf(CategoryUi("android", persistentListOf(NoteUi("a", "제목", "2026.06.12")))),
                recentNotes = persistentListOf(),
            )
        val notLoaded = withNotes.copy(notesLoaded = false, categories = persistentListOf())
        val loadedEmpty = withNotes.copy(categories = persistentListOf())

        assertEquals(StartDestination.Shell, startOf(shell, withNotes))
        assertEquals(StartDestination.Reading, startOf(shell, notLoaded))
        assertEquals(StartDestination.ConnectRoot, startOf(shell, loadedEmpty))
    }

    private fun connected(
        source: String,
        owner: String,
        repo: String,
        branch: String = "main",
    ) = ConnectionState.Connected(
        sourceId = SourceId(source),
        connection = RepoConnection(owner, repo, branch, "notes/index.json", isPublic = true),
    )
}
