package com.hancekim.mynote.feature.notes.quiz

import com.hancekim.mynote.feature.notes.FeatureFixture
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.CATEGORY
import com.hancekim.mynote.feature.notes.FeatureFixture.Companion.NOTE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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
class QuizViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FM-9 늦게 온 자동 진행은 문항을 건너뛰지 않는다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val vm = fixture.quiz()
            fixture.subscribe(vm)
            startSolving(vm)

            vm.onIntent(QuizIntent.PickChoice(0))
            assertTrue(solving(vm).autoAdvancing)
            advanceTimeBy(AUTO_ADVANCE - 1)
            assertEquals(0, solving(vm).index)
            advanceTimeBy(2)
            assertEquals(1, solving(vm).index)

            vm.onIntent(QuizIntent.PickChoice(0))
            vm.onIntent(QuizIntent.NextQuestion())
            assertEquals(2, solving(vm).index)
            advanceTimeBy(AUTO_ADVANCE + 1)
            assertEquals(2, solving(vm).index)

            vm.onIntent(QuizIntent.PickChoice(0))
            vm.onIntent(QuizIntent.NextQuestion(fromIndex = 1))
            assertEquals(2, solving(vm).index)
        }

    @Test
    fun `FM-9 그만둔 퀴즈는 창 안에서만 되돌린다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val vm = fixture.quiz()
            fixture.subscribe(vm)
            startSolving(vm)
            vm.onIntent(QuizIntent.PickChoice(0))
            vm.onIntent(QuizIntent.NextQuestion())

            vm.onIntent(QuizIntent.Dismiss)
            assertEquals(QuizUiState.Idle, vm.state.value.quiz)
            assertEquals(1, vm.state.value.quizUndoId)
            advanceTimeBy(UNDO_WINDOW - 1)
            vm.onIntent(QuizIntent.UndoExit)
            assertEquals(1, solving(vm).index)

            vm.onIntent(QuizIntent.Dismiss)
            assertEquals(2, vm.state.value.quizUndoId)
            advanceTimeBy(UNDO_WINDOW + 1)
            assertNull(vm.state.value.quizUndoMessage)
            assertEquals(0, vm.state.value.quizUndoId)
            vm.onIntent(QuizIntent.UndoExit)
            assertEquals(QuizUiState.Idle, vm.state.value.quiz)
        }

    private fun TestScope.startSolving(vm: QuizViewModel) {
        vm.onIntent(QuizIntent.OpenSetup(NOTE_ID, CATEGORY))
        vm.onIntent(QuizIntent.Generate(NOTE_ID, 3))
        runCurrent()
        assertEquals(0, solving(vm).index)
    }

    private fun solving(vm: QuizViewModel): QuizUiState.Solving = vm.state.value.quiz as QuizUiState.Solving

    private companion object {
        const val UNDO_WINDOW = 3_200L
        const val AUTO_ADVANCE = QuizUiState.AUTO_ADVANCE_MILLIS.toLong()
    }
}
