package com.hancekim.mynote.shell

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.element.LastSync
import com.hancekim.mynote.core.ui.element.SecretText
import com.hancekim.mynote.core.ui.element.SyncFailureKind
import com.hancekim.mynote.core.ui.element.quizCtaUi
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.ReaderScrollRequest
import com.hancekim.mynote.feature.notes.quiz.QuizIntent
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import com.hancekim.mynote.feature.settings.SettingsIntent
import com.hancekim.mynote.feature.stats.StatsIntent
import com.hancekim.mynote.shell.FeatureFixture.Companion.CATEGORY
import com.hancekim.mynote.shell.FeatureFixture.Companion.EVENT_ID
import com.hancekim.mynote.shell.FeatureFixture.Companion.NOTE_ID
import com.hancekim.mynote.shell.FeatureFixture.Companion.REPO
import com.hancekim.mynote.shell.FeatureFixture.Companion.UNREADABLE_ID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 기능 사이 신호(FM-6) 가운데 셸 ViewModel · 셸 판정을 지나는 것과 통계에서 노트 · 퀴즈로 가는 것 · 노트와 설정 ViewModel 이 같이 보는 것 —
 * 셸 · 여러 기능 모듈을 같이 보는 자리가 `:app` 이라 여기 산다(FM-13 같은 클래스 이름).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureSignalsTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FM-6 통계에서 노트를 열면 리더가 열린다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val notes = fixture.notes()
            val stats = fixture.stats()
            fixture.subscribe(notes, stats)

            stats.onIntent(StatsIntent.OpenNote(NOTE_ID, resumeAt = 0.4f))

            assertEquals(
                NotesNavKey.Reader(NOTE_ID),
                fixture.navigator.stack.value
                    .last(),
            )
            assertEquals(ReaderScrollRequest(NOTE_ID, 0.4f, id = 1), notes.state.value.readerScrollRequest)
        }

    @Test
    fun `FM-6 통계 다시 풀기는 리더를 열고 퀴즈 설정을 띄운다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val notes = fixture.notes()
            val quiz = fixture.quiz()
            val stats = fixture.stats()
            fixture.subscribe(notes, quiz, stats)

            stats.onIntent(StatsIntent.RetryQuiz(NOTE_ID))

            assertEquals(
                NotesNavKey.Reader(NOTE_ID),
                fixture.navigator.stack.value
                    .last(),
            )
            assertEquals(QuizUiState.Setup(NOTE_ID), quiz.state.value.quiz)

            quiz.onIntent(QuizIntent.Dismiss)
            fixture.settle()
            stats.onIntent(StatsIntent.RetryQuiz(UNREADABLE_ID))

            assertEquals(
                NotesNavKey.Reader(UNREADABLE_ID),
                fixture.navigator.stack.value
                    .last(),
            )
            assertEquals(QuizUiState.Idle, quiz.state.value.quiz)
        }

    @Test
    fun `FM-6 설정을 열면 토큰 편집을 정리한다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val notes = fixture.notes()
            val shell = fixture.shell()
            val settings = fixture.settings()
            fixture.subscribe(notes, shell, settings)

            settings.onIntent(SettingsIntent.StartTokenEdit)
            assertTrue(settings.state.value.tokenEditing)
            notes.onIntent(NotesIntent.OpenSettings)

            assertFalse(settings.state.value.tokenEditing)
            assertEquals(
                NotesNavKey.Settings,
                fixture.navigator.stack.value
                    .last(),
            )

            settings.onIntent(SettingsIntent.StartTokenEdit)
            shell.onIntent(ShellIntent.OpenSettings)

            assertFalse(settings.state.value.tokenEditing)
        }

    @Test
    fun `FM-6 모델이 없을 때 CTA 는 받기 확인을 띄운다`() {
        val missing = ctaTarget(NOTE_ID, CATEGORY, quizCtaUi(ModelStatus.Missing).action)
        val ready = ctaTarget(NOTE_ID, CATEGORY, quizCtaUi(ModelStatus.Ready).action)
        val paused = ctaTarget(NOTE_ID, CATEGORY, quizCtaUi(ModelStatus.Paused(received = 1L, total = 2L)).action)

        assertEquals(CtaTarget.ConfirmDownload, missing)
        assertEquals(CtaTarget.Quiz(QuizIntent.OpenSetup(NOTE_ID, CATEGORY)), ready)
        assertEquals(CtaTarget.Settings(SettingsIntent.StartModelDownload), paused)
    }

    @Test
    fun `FM-6 연결이 바뀌면 스택과 퀴즈와 마지막 동기화를 같이 정리한다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val shell = fixture.shell()
            val notes = fixture.notes()
            val settings = fixture.settings()
            val quiz = fixture.quiz()
            fixture.subscribe(shell, notes, settings, quiz)
            notes.onIntent(NotesIntent.SelectNote(NOTE_ID))
            quiz.onIntent(QuizIntent.OpenSetup(NOTE_ID, CATEGORY))
            quiz.onIntent(QuizIntent.Generate(NOTE_ID, 3))
            fixture.lastSync.set(LastSync.Failed(SyncFailureKind.Offline))
            fixture.settle()
            assertTrue(quiz.state.value.quiz is QuizUiState.Solving)

            fixture.connections.connection.value =
                ConnectionState.Connected(SourceId("s2"), REPO.copy(repo = "kotlin-notes"))

            assertEquals(listOf(NotesNavKey.Home), fixture.navigator.stack.value)
            assertEquals(QuizUiState.Idle, quiz.state.value.quiz)
            assertFalse(fixture.signals.quizCoversReader.value)
            assertEquals(LastSync.Succeeded, fixture.lastSync.value.value)
            assertEquals(LastSync.Succeeded, settings.state.value.lastSync)
            assertEquals(listOf(NotesNavKey.Home), shell.state.value.backStack)
        }

    @Test
    fun `FM-6 토큰 편집 중에는 맨 위로를 숨긴다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val settings = fixture.settings()
            fixture.subscribe(settings)
            assertTrue(scrollToTopVisible(true, false, false, settings.state.value.tokenEditing))

            settings.onIntent(SettingsIntent.StartTokenEdit)

            assertFalse(scrollToTopVisible(true, false, false, settings.state.value.tokenEditing))
        }

    @Test
    fun `FM-6 연결 확인 중에는 연결 화면을 열지 않는다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val fixture = FeatureFixture(this, probeGate = gate)
            val notes = fixture.notes()
            val settings = fixture.settings()
            fixture.subscribe(notes, settings)

            settings.onIntent(SettingsIntent.StartTokenEdit)
            settings.onIntent(SettingsIntent.SaveToken(SecretText("ghp_checking0000")))
            runCurrent()
            assertTrue(settings.state.value.tokenEdit.verifying)

            notes.onIntent(NotesIntent.OpenConnect)
            settings.onIntent(SettingsIntent.OpenConnect)
            fixture.settle()
            assertEquals(listOf(NotesNavKey.Home), fixture.navigator.stack.value)

            gate.complete(Unit)
            runCurrent()
            notes.onIntent(NotesIntent.OpenConnect)
            assertEquals(
                NotesNavKey.Connect,
                fixture.navigator.stack.value
                    .last(),
            )
        }

    @Test
    fun `FM-6 학습 기록을 지운 뒤 북마크 되돌리기는 기록을 건드리지 않는다`() =
        runTest {
            val fixture = FeatureFixture(this, bookmarks = listOf(Bookmark(NOTE_ID, addedAt = 100L)))
            fixture.signals.studyLogCleared()
            val notes = fixture.notes()
            val settings = fixture.settings()
            fixture.subscribe(notes, settings)

            notes.onIntent(NotesIntent.RemoveBookmark(NOTE_ID))
            runCurrent()
            notes.onIntent(NotesIntent.UndoRemoveBookmark)
            runCurrent()
            assertEquals(listOf(EVENT_ID), fixture.studyLog.cancelledEvents)

            notes.onIntent(NotesIntent.RemoveBookmark(NOTE_ID))
            runCurrent()
            settings.onIntent(SettingsIntent.ClearStudyLog)
            notes.onIntent(NotesIntent.UndoRemoveBookmark)
            runCurrent()

            assertEquals(listOf(EVENT_ID), fixture.studyLog.cancelledEvents)
            assertEquals(listOf(Bookmark(NOTE_ID, addedAt = 100L)), fixture.bookmarks.entries.value)
        }
}
