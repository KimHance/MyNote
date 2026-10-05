package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.RecentNoteUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryLoadTest {
    private val repo = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)
    private val connected = ConnectionState.Connected(SourceId("s1"), repo)
    private val other = ConnectionState.Connected(SourceId("s2"), repo.copy(repo = "kotlin-notes"))
    private val notes = persistentListOf(CategoryUi("android", persistentListOf(NoteUi("a", "제목", "2026.06.12"))))

    private fun home(load: CategoryLoad) =
        NotesUiState(
            connection = connected,
            notesLoaded = load.loaded,
            seedPending = false,
            categories = load.list,
            isLoading = false,
            awaitingNotes = load.settling,
        ).homeBody

    private class Harness(
        val connections: MutableStateFlow<ConnectionState>,
    ) {
        val sources = mutableListOf<MutableSharedFlow<ImmutableList<CategoryUi>>>()
        val loads = mutableListOf<CategoryLoad>()

        fun latest() = loads.last()
    }

    private fun kotlinx.coroutines.test.TestScope.harness(initial: ConnectionState): Harness {
        val harness = Harness(MutableStateFlow(initial))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            settledCategories(harness.connections) {
                MutableSharedFlow<ImmutableList<CategoryUi>>().also { harness.sources += it }
            }.collect { harness.loads += it }
        }
        return harness
    }

    @Test
    fun `RC-21 확정 직후 노트가 늦게 와도 노트 없음이 비치지 않는다`() =
        runTest {
            val h = harness(connected)

            assertEquals(HomeBody.Receiving, home(h.latest()))
            assertEquals(true, h.latest().settling)

            h.connections.value = other
            assertEquals(HomeBody.Receiving, home(h.latest()))
            assertEquals(true, h.loads.filter { it.loaded || it.settling }.none { home(it) == HomeBody.NoNotes })

            h.sources.last().emit(notes)

            assertEquals(false, h.latest().settling)
            assertEquals(
                HomeBody.Notes,
                NotesUiState(
                    connection = other,
                    notesLoaded = true,
                    categories = h.latest().list,
                    recentNotes = persistentListOf(RecentNoteUi(notes[0].notes[0], "android")),
                ).homeBody,
            )
        }

    @Test
    fun `RC-21 빈 저장소는 첫 방출 즉시 노트 없음이다`() =
        runTest {
            val h = harness(connected)
            h.sources.last().emit(persistentListOf())

            assertEquals(false, h.latest().settling)
            assertEquals(true, h.latest().loaded)
            assertEquals(HomeBody.NoNotes, home(h.latest()))
        }

    @Test
    fun `RC-21 끊으면 기다림이 풀린다`() =
        runTest {
            val h = harness(connected)
            assertEquals(true, h.latest().settling)

            h.connections.value = ConnectionState.None

            assertEquals(false, h.latest().settling)
        }

    @Test
    fun `RC-21 연결이 다시 바뀌면 기다림을 새로 시작한다`() =
        runTest {
            val h = harness(connected)
            h.sources.last().emit(notes)
            assertEquals(false, h.latest().settling)

            h.connections.value = other

            assertEquals(true, h.latest().settling)
            assertEquals(2, h.sources.size)
            assertEquals(notes, h.latest().list)
        }

    @Test
    fun `RC-21 노트를 담은 목록이 왔는데 recentNotes 가 아직 비어도 노트 없음이 아니다`() {
        val state =
            NotesUiState(
                connection = connected,
                notesLoaded = true,
                seedPending = false,
                isLoading = false,
                categories = notes,
                recentNotes = persistentListOf(),
            )

        assertEquals(HomeBody.Notes, state.homeBody)
        assertEquals(
            true,
            state.copy(connection = ConnectionState.None).showConnectBand,
        )
    }
}
