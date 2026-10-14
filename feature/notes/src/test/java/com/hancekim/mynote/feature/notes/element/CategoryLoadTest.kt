package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.feature.notes.RecentNoteUi
import com.hancekim.mynote.feature.notes.mapper.toUi
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
    private val notes = listOf(Category("android", listOf(Note("s1/a", "제목", "2026.06.12", emptyList()))))
    private val notesUi = notes.toUi()

    private fun home(load: CategoryLoad) =
        NotesUiState(
            connection = connected,
            notesLoaded = load.loaded,
            categories = load.list,
            isLoading = false,
            awaitingNotes = load.settling,
        ).homeBody

    private class Harness(
        val connections: MutableStateFlow<ConnectionState>,
    ) {
        val sources = mutableListOf<MutableSharedFlow<List<Category>>>()
        val loads = mutableListOf<CategoryLoad>()

        fun latest() = loads.last()
    }

    private fun kotlinx.coroutines.test.TestScope.harness(initial: ConnectionState): Harness {
        val harness = Harness(MutableStateFlow(initial))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            settledCategories(harness.connections) {
                MutableSharedFlow<List<Category>>().also { harness.sources += it }
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
                    recentNotes = persistentListOf(RecentNoteUi(notesUi[0].notes[0], "android")),
                ).homeBody,
            )
        }

    @Test
    fun `RC-21 빈 저장소는 첫 방출 즉시 노트 없음이다`() =
        runTest {
            val h = harness(connected)
            h.sources.last().emit(emptyList())

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
            assertEquals(notesUi, h.latest().list)
        }

    @Test
    fun `RC-21 노트를 담은 목록이 왔는데 recentNotes 가 아직 비어도 노트 없음이 아니다`() {
        val state =
            NotesUiState(
                connection = connected,
                notesLoaded = true,
                isLoading = false,
                categories = notesUi,
                recentNotes = persistentListOf(),
            )

        assertEquals(HomeBody.Notes, state.homeBody)
    }

    @Test
    fun `RC-1 연결 안내는 연결 정보 없는 저장소 노트가 있을 때만이다`() =
        runTest {
            val legacy = Note("s0/graph", "그래프", "2026.04.01", emptyList())
            val device = Note("local/7f3a", "코루틴", "2026.10.14", emptyList())

            fun band(load: CategoryLoad, connection: ConnectionState) =
                NotesUiState(
                    connection = connection,
                    notesLoaded = load.loaded,
                    categories = load.list,
                    connectBandCount = load.connectBandCount,
                ).showConnectBand

            val h = harness(ConnectionState.None)
            h.sources.last().emit(listOf(Category("기본", listOf(device))))
            assertEquals(0, h.latest().connectBandCount)
            assertEquals(false, band(h.latest(), ConnectionState.None))

            h.sources.last().emit(listOf(Category("cs", listOf(legacy)), Category("기본", listOf(device))))
            assertEquals(1, h.latest().connectBandCount)
            assertEquals(true, band(h.latest(), ConnectionState.None))

            h.connections.value = connected
            h.sources.last().emit(listOf(Category("android", listOf(Note("s1/a", "제목", "2026.06.12", emptyList())))))
            assertEquals(0, h.latest().connectBandCount)
            assertEquals(false, band(h.latest(), connected))
        }
}
