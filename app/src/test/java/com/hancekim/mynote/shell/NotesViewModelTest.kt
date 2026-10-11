package com.hancekim.mynote.shell

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.ui.element.LastSync
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.shell.FeatureFixture.Companion.NOTE_ID
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
import org.junit.Before
import org.junit.Test

/** 노트 ViewModel 시험 가운데 셸 ViewModel 과 같이 세우는 것 — 셸이 `:app` 에 있어 여기 산다(FM-13 같은 클래스 이름). */
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
