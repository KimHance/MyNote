package com.hancekim.mynote.feature.notes.signal

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.feature.notes.FeatureFixture
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.CATEGORY
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.EVENT_ID
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.NOTE_ID
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.REPO
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.UNREADABLE_ID
import com.hancekim.mynote.feature.notes.element.LastSync
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.ReaderScrollRequest
import com.hancekim.mynote.feature.notes.element.SecretText
import com.hancekim.mynote.feature.notes.element.SyncFailureKind
import com.hancekim.mynote.feature.notes.element.quizCtaUi
import com.hancekim.mynote.feature.notes.quiz.QuizIntent
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import com.hancekim.mynote.feature.notes.settings.SettingsIntent
import com.hancekim.mynote.feature.notes.shell.CtaTarget
import com.hancekim.mynote.feature.notes.shell.ShellIntent
import com.hancekim.mynote.feature.notes.shell.ctaTarget
import com.hancekim.mynote.feature.notes.shell.scrollToTopVisible
import com.hancekim.mynote.feature.notes.stats.StatsIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 기능 사이 신호 열 갈래(FM-6) — ViewModel 들이 앱의 싱글턴과 같은 계약 · 홀더 · Navigator 를 나눠 쓴다. */
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
    fun `FM-6 퀴즈가 리더를 덮는 동안 읽기 세션이 멈춘다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val notes = fixture.notes()
            val quiz = fixture.quiz()
            fixture.subscribe(notes, quiz)
            notes.onIntent(NotesIntent.ForegroundChanged(true))
            notes.onIntent(NotesIntent.SelectNote(NOTE_ID))

            advanceTimeBy(40_000)
            quiz.onIntent(QuizIntent.OpenSetup(NOTE_ID, CATEGORY))
            quiz.onIntent(QuizIntent.Generate(NOTE_ID, 3))
            assertTrue(fixture.signals.quizCoversReader.value)
            advanceTimeBy(100_000)
            quiz.onIntent(QuizIntent.Dismiss)
            advanceTimeBy(10_000)
            notes.onIntent(NotesIntent.ForegroundChanged(false))
            runCurrent()

            assertEquals(
                50_000L,
                fixture.studyLog.sessions
                    .single()
                    .activeMillis,
            )
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
    fun `FM-6 새로 만든 노트 ViewModel 은 지난 리더 열기를 다시 처리하지 않는다`() =
        runTest {
            val fixture = FeatureFixture(this)
            fixture.signals.reportReaderOpened(NOTE_ID, resumeAt = 0.4f, quiz = true)

            val notes = fixture.notes()
            val quiz = fixture.quiz()
            fixture.subscribe(notes, quiz)

            assertNull(notes.state.value.readerScrollRequest)
            assertNull(fixture.signals.quizSetup.value)
            assertEquals(QuizUiState.Idle, quiz.state.value.quiz)
        }

    /** 메인이 바로 돌지 않으면 만들 때 띄운 수집기가 첫 인텐트 뒤에 돈다 — 그때 지난 신호를 처리하면 보인다. */
    @Test
    fun `FM-6 새로 만든 퀴즈 ViewModel 은 지난 설정 요청과 닫기를 다시 처리하지 않는다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val fixture = FeatureFixture(this)
            fixture.signals.requestQuizSetup(UNREADABLE_ID, CATEGORY)
            fixture.signals.closeQuiz()

            val quiz = fixture.quiz()
            quiz.onIntent(QuizIntent.OpenSetup(NOTE_ID, CATEGORY))
            runCurrent()

            assertEquals(QuizUiState.Setup(NOTE_ID), quiz.state.value.quiz)
        }

    @Test
    fun `FM-6 새로 만든 설정 ViewModel 은 지난 설정 열기로 편집을 정리하지 않는다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val fixture = FeatureFixture(this)
            fixture.signals.enterSettings()

            val settings = fixture.settings()
            settings.onIntent(SettingsIntent.StartTokenEdit)
            runCurrent()

            assertTrue(settings.state.value.tokenEditing)
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
