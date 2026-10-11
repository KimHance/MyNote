package com.hancekim.mynote.feature.quiz.signal

import com.hancekim.mynote.feature.quiz.FeatureFixture
import com.hancekim.mynote.feature.quiz.FeatureFixture.Companion.CATEGORY
import com.hancekim.mynote.feature.quiz.FeatureFixture.Companion.NOTE_ID
import com.hancekim.mynote.feature.quiz.FeatureFixture.Companion.UNREADABLE_ID
import com.hancekim.mynote.feature.quiz.QuizIntent
import com.hancekim.mynote.feature.quiz.QuizUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 기능 사이 신호(FM-6) 가운데 퀴즈 ViewModel 하나만 보는 것 — 노트 ViewModel 을 같이 보는 것은 `:app` 의 같은 이름 클래스에 있다(FM-13).
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
}
