package com.hancekim.mynote.feature.notes

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.element.LastSync
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.EVENT_ID
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.NOTE_ID
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.SETTLE
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.WALL_BASE
import com.hancekim.mynote.feature.notes.element.HomeAction
import com.hancekim.mynote.feature.notes.element.HomeBody
import com.hancekim.mynote.feature.notes.element.NotesIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FM-9 해제한 북마크를 되돌린다`() =
        runTest {
            val fixture = FeatureFixture(this, bookmarks = listOf(Bookmark(NOTE_ID, addedAt = 100L)))
            val vm = fixture.notes()
            fixture.subscribe(vm)
            advanceTimeBy(5_000)

            vm.onIntent(NotesIntent.RemoveBookmark(NOTE_ID))
            runCurrent()
            assertTrue(
                vm.state.value.bookmarks
                    .isEmpty(),
            )
            assertEquals(
                NotesIntent.UndoRemoveBookmark,
                vm.state.value.notice
                    ?.undo,
            )
            assertEquals(
                WALL_BASE + SETTLE + 5_000,
                fixture.studyLog.events
                    .single()
                    .at,
            )

            vm.onIntent(NotesIntent.UndoRemoveBookmark)
            runCurrent()

            assertEquals(listOf(Bookmark(NOTE_ID, addedAt = 100L)), fixture.bookmarks.entries.value)
            assertEquals(listOf(EVENT_ID), fixture.studyLog.cancelledEvents)
            assertEquals(1, vm.state.value.bookmarks.size)
        }

    @Test
    fun `FM-8 읽기 세션은 주입한 시계로 잰다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val vm = fixture.notes()
            fixture.subscribe(vm)
            vm.onIntent(NotesIntent.ForegroundChanged(true))

            vm.onIntent(NotesIntent.SelectNote(NOTE_ID))
            advanceTimeBy(40_000)
            vm.onIntent(NotesIntent.ForegroundChanged(false))
            runCurrent()

            val session = fixture.studyLog.sessions.single()
            assertEquals(NOTE_ID, session.noteId)
            assertEquals(WALL_BASE + SETTLE, session.startedAt)
            assertEquals(WALL_BASE + SETTLE + 40_000, session.endedAt)
            assertEquals(40_000L, session.activeMillis)
        }

    @Test
    fun `LN-1 저장소 연결은 쌓은 연결 화면을 열고 뒤로는 홈이다`() =
        runTest {
            val fixture = FeatureFixture(this)
            fixture.connections.connection.value = ConnectionState.None
            fixture.notesRepo.categories.value = emptyList()
            val vm = fixture.notes()
            fixture.subscribe(vm)

            assertEquals(HomeBody.FirstRun, vm.state.value.homeBody)
            assertTrue(
                vm.state.value.homeEntries
                    .any { it.action == HomeAction.Connect },
            )

            vm.onIntent(NotesIntent.OpenConnect)
            fixture.settle()
            assertEquals(listOf(NotesNavKey.Home, NotesNavKey.Connect), fixture.navigator.stack.value)

            fixture.navigator.pop()
            fixture.settle()
            assertEquals(listOf(NotesNavKey.Home), fixture.navigator.stack.value)
        }

    @Test
    fun `LN-1 마지막 기기 노트를 지우면 첫 실행 빈 상태로 돌아간다`() =
        runTest {
            val fixture = FeatureFixture(this)
            fixture.connections.connection.value = ConnectionState.None
            fixture.notesRepo.categories.value =
                listOf(Category("그룹 없음", listOf(Note("local/7f3a", "코루틴", "2026.10.14", emptyList()))))
            val vm = fixture.notes()
            fixture.subscribe(vm)

            assertEquals(HomeBody.Notes, vm.state.value.homeBody)
            assertEquals(false, vm.state.value.showConnectBand)

            fixture.notesRepo.categories.value = emptyList()
            runCurrent()

            assertEquals(HomeBody.FirstRun, vm.state.value.homeBody)
        }

    @Test
    fun `LN-2 새 노트는 지금 스택 위에 편집기를 열고 가져오기는 그릇을 연다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val vm = fixture.notes()
            fixture.subscribe(vm)
            vm.onIntent(NotesIntent.SelectNote(NOTE_ID))
            fixture.settle()

            vm.onIntent(NotesIntent.NewNote)
            fixture.settle()
            assertEquals(
                listOf(NotesNavKey.Home, NotesNavKey.Reader(NOTE_ID), NotesNavKey.Editor()),
                fixture.navigator.stack.value,
            )
            fixture.navigator.pop()
            fixture.settle()
            assertEquals(listOf(NotesNavKey.Home, NotesNavKey.Reader(NOTE_ID)), fixture.navigator.stack.value)

            val before = fixture.signals.importRequests.value
            vm.onIntent(NotesIntent.Import)
            assertEquals(before + 1, fixture.signals.importRequests.value)
            assertEquals(listOf(NotesNavKey.Home, NotesNavKey.Reader(NOTE_ID)), fixture.navigator.stack.value)
        }
}
