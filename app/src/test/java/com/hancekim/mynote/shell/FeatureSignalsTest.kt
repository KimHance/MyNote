package com.hancekim.mynote.shell

import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.element.LastSync
import com.hancekim.mynote.core.ui.element.SyncFailureKind
import com.hancekim.mynote.core.ui.element.quizCtaUi
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.quiz.QuizIntent
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import com.hancekim.mynote.feature.notes.settings.SettingsIntent
import com.hancekim.mynote.shell.FeatureFixture.Companion.CATEGORY
import com.hancekim.mynote.shell.FeatureFixture.Companion.NOTE_ID
import com.hancekim.mynote.shell.FeatureFixture.Companion.REPO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 기능 사이 신호(FM-6) 가운데 셸 ViewModel · 셸 판정을 지나는 것 — 셸이 `:app` 에 있어 여기 산다(FM-13 같은 클래스 이름). */
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
}
