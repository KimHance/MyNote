package com.hancekim.mynote.feature.notes

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.EVENT_ID
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.NOTE_ID
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.SETTLE
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.WALL_BASE
import com.hancekim.mynote.feature.notes.element.LastSync
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesSignal
import com.hancekim.mynote.feature.notes.shell.ShellIntent
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
    fun `FM-9 본 신호는 다시 나오지 않는다`() =
        runTest {
            val fixture = FeatureFixture(this, bookmarks = listOf(Bookmark(NOTE_ID, addedAt = 100L)))
            val shell = fixture.shell()
            val notes = fixture.notes()
            val first = fixture.subscribe(shell, notes)

            shell.onIntent(ShellIntent.Back)
            notes.onIntent(NotesIntent.RemoveBookmark(NOTE_ID))
            runCurrent()
            val hint = shell.state.value.exitHintId
            val notice = requireNotNull(notes.state.value.notice)
            assertEquals(1, hint)

            shell.onIntent(ShellIntent.SignalShown(NotesSignal.ExitHint, hint))
            notes.onIntent(NotesIntent.SignalShown(NotesSignal.Notice, notice.id))
            assertEquals(0, shell.state.value.exitHintId)
            assertNull(notes.state.value.notice)

            first.forEach { it.cancel() }
            advanceTimeBy(10_000)
            fixture.subscribe(shell, notes)
            assertEquals(0, shell.state.value.exitHintId)
            assertNull(notes.state.value.notice)
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
    fun `FM-7 동기화는 구독이 여럿이어도 한 번 돈다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val notes = fixture.notes()
            val settings = fixture.settings()
            val shell = fixture.shell()

            fixture.subscribe(notes, notes, settings, shell)
            val again = fixture.subscribe(notes, settings)
            again.forEach { it.cancel() }
            advanceTimeBy(10_000)
            fixture.subscribe(notes, settings)

            assertEquals(1, fixture.notesRepo.syncs)
            assertEquals(LastSync.Succeeded, settings.state.value.lastSync)
        }
}
