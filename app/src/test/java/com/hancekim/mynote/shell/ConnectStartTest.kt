package com.hancekim.mynote.shell

import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.element.NotesUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectStartTest {
    @Test
    fun `RC-1 시작 화면 판정 표`() {
        // (연결) × (저장소 노트) × (기기 노트) — 시작 판정은 노트 수만 본다. 저장소 노트 · 기기 노트 어느 쪽이든 하나면 "노트 있음"
        val connections = listOf(ConnectionKnown.Unknown, ConnectionKnown.None, ConnectionKnown.Connected)
        val repoNotes = listOf<Int?>(null, 0, 22)
        val deviceNotes = listOf(0, 3)
        var cells = 0
        for (connection in connections) {
            for (repo in repoNotes) {
                for (device in deviceNotes) {
                    val notes =
                        when {
                            repo == null -> NotesKnown.Unknown
                            repo + device == 0 -> NotesKnown.Empty
                            else -> NotesKnown.Some
                        }
                    val expected =
                        if (connection == ConnectionKnown.Unknown ||
                            repo == null
                        ) {
                            StartDestination.Reading
                        } else {
                            StartDestination.Shell
                        }
                    assertEquals(
                        "연결 $connection · 저장소 노트 $repo · 기기 노트 $device",
                        expected,
                        startDestination(connection, notes, seedPending = false),
                    )
                    cells++
                }
            }
        }
        assertEquals(18, cells)
    }

    @Test
    fun `RC-1 시작 결과에 연결 화면이 없다`() {
        assertEquals(listOf(StartDestination.Reading, StartDestination.Shell), StartDestination.entries)
        assertEquals(
            StartDestination.Shell,
            startDestination(ConnectionKnown.None, NotesKnown.Empty, seedPending = false),
        )
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
            StartDestination.Shell,
            startDestination(ConnectionKnown.None, NotesKnown.Empty, seedPending = false),
        )
    }

    @Test
    fun `LN-13 이전 판 사용자의 첫 화면은 그대로다`() {
        // 연결 있음 + 노트 22 → 홈 · 연결 없음 + 옛 캐시 22 → 홈(띠는 홈이 정한다)
        assertEquals(
            StartDestination.Shell,
            startDestination(ConnectionKnown.Connected, NotesKnown.Some, seedPending = false),
        )
        assertEquals(
            StartDestination.Shell,
            startDestination(ConnectionKnown.None, NotesKnown.Some, seedPending = false),
        )
    }

    @Test
    fun `LN-8 읽는 중에는 편집기를 미룬다`() {
        // 공유가 먼저 와서 스택에 편집기가 쌓여도 시작 판정이 읽는 중이면 셸(편집기)을 그리지 않는다
        val shell =
            ShellUiState(
                backStack = persistentListOf(NotesNavKey.Home, NotesNavKey.Editor(draftId = 1)),
                connection = null,
            )
        val notes = NotesUiState(notesLoaded = false)

        assertEquals(StartDestination.Reading, startOf(shell, notes))
        assertEquals(
            StartDestination.Shell,
            startOf(shell.copy(connection = ConnectionState.None, seedPending = false), notes.copy(notesLoaded = true)),
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
    fun `RC-23 끊으면 스택은 홈 하나다`() {
        val connected = connected("s1", "octo-learner", "android-notes")

        val cleanup = screenCleanupOf(connected, ConnectionState.None)

        assertEquals(ScreenCleanup(resetStack = true, dismissQuiz = true), cleanup)
        // 끊은 뒤 노트가 0 이어도 시작 판정은 셸(홈 첫 실행 빈 상태)이다 — RC-17 끊은 뒤 화면은 홈이다
        assertEquals(
            StartDestination.Shell,
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
        assertEquals(StartDestination.Shell, startOf(shell, loadedEmpty))
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
