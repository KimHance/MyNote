package com.hancekim.mynote.feature.notes.quiz

import com.hancekim.mynote.feature.notes.QuizQuestionUi
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 채점·진행 규칙은 순수 함수라 화면도 코루틴도 없이 검증한다.
 *
 * 첫 구현은 고른 답을 `rememberSaveable` 에 뒀고, 그래서 **고르지도 않은 문제에 "오답" 이
 * 뜨는** 버그가 났다. 그 규칙이 상태로 내려온 지금은 여기서 못 박아 둔다.
 */
class QuizGradingTest {
    private fun question(answer: Int) =
        QuizQuestionUi(
            question = "질문",
            choices = persistentListOf("1", "2", "3", "4"),
            answerIndex = answer,
            explanation = "설명",
        )

    private fun solving(
        vararg answers: Int,
        index: Int = 0,
        picked: Int? = null,
        marks: List<Boolean> = emptyList(),
    ) = QuizUiState.Solving(
        questions = answers.map { question(it) }.let { persistentListOf(*it.toTypedArray()) },
        index = index,
        picked = picked,
        marks = persistentListOf(*marks.toTypedArray()),
    )

    @Test
    fun `고르기 전에는 아무것도 공개되지 않는다`() {
        val state = solving(1, 2)

        assertFalse(state.revealed)
        assertNull(state.picked)
        assertTrue(state.marks.isEmpty())
    }

    @Test
    fun `정답을 고르면 marks 에 true 가 쌓인다`() {
        val graded = solving(1, 2).pick(1)

        assertTrue(graded.revealed)
        assertTrue(graded.isCorrect)
        assertEquals(listOf(true), graded.marks)
    }

    @Test
    fun `오답을 고르면 marks 에 false 가 쌓인다`() {
        val graded = solving(1, 2).pick(3)

        assertFalse(graded.isCorrect)
        assertEquals(listOf(false), graded.marks)
    }

    @Test
    fun `이미 고른 문항은 다시 고를 수 없다`() {
        val graded = solving(1, 2).pick(3)

        val again = graded.pick(1)

        // 같은 인스턴스를 그대로 돌려줘야 ViewModel 이 "아무 일도 없었다"를 알아본다.
        assertSame(graded, again)
        assertEquals(listOf(false), again.marks)
    }

    @Test
    fun `없는 선택지를 고르면 아무 일도 일어나지 않는다`() {
        val state = solving(1, 2)

        assertSame(state, state.pick(9))
        assertSame(state, state.pick(-1))
    }

    @Test
    fun `고르기 전에는 다음으로 넘어가지 않는다`() {
        val state = solving(1, 2)

        assertSame(state, state.advance())
    }

    @Test
    fun `넘어가면 고른 답과 자동 진행이 초기화된다`() {
        val next = solving(1, 2).pick(1).copy(autoAdvancing = true).advance()

        val solving = next as QuizUiState.Solving
        assertEquals(1, solving.index)
        assertNull(solving.picked)
        assertFalse(solving.autoAdvancing)
        // 채점 결과는 남는다 — 진행 바가 계속 보여 줘야 한다.
        assertEquals(listOf(true), solving.marks)
    }

    @Test
    fun `마지막 문항에서 넘어가면 결과가 된다`() {
        val state = solving(1, 2, index = 1, marks = listOf(true)).pick(0)

        val result = state.advance()

        assertEquals(QuizUiState.Result(persistentListOf(true, false)), result)
    }

    @Test
    fun `점수는 맞힌 개수이고 정답률은 비율이다`() {
        val result = QuizUiState.Result(persistentListOf(true, false, true, true))

        assertEquals(3, result.score)
        assertEquals(4, result.total)
        assertEquals(0.75f, result.rate, 0.0001f)
    }

    @Test
    fun `문제가 0개면 정답률을 0으로 둔다`() {
        assertEquals(0f, QuizUiState.Result(persistentListOf()).rate, 0.0001f)
    }

    @Test
    fun `낼 내용이 없어 실패한 경우엔 문항 수를 줄여도 소용없다`() {
        val empty = QuizUiState.Failed(QuizUiState.Failed.Kind.Empty, maxCount = 5, noteId = "n")

        assertFalse(empty.canRetryWithFewer)
    }

    @Test
    fun `이미 최소 문항으로 실패했으면 더 줄일 수 없다`() {
        val atMin =
            QuizUiState.Failed(
                kind = QuizUiState.Failed.Kind.Parse,
                maxCount = QuizUiState.MIN_COUNT,
                noteId = "n",
            )

        assertFalse(atMin.canRetryWithFewer)
    }

    @Test
    fun `형식이 깨져 실패했고 문항이 남아 있으면 줄여서 다시 낼 수 있다`() {
        val parse = QuizUiState.Failed(QuizUiState.Failed.Kind.Parse, maxCount = 5, noteId = "n")

        assertTrue(parse.canRetryWithFewer)
    }

    @Test
    fun `picks 는 marks 와 같은 길이로 고른 번호를 쌓는다`() {
        val first = solving(1, 2).pick(3)
        val second = (first.advance() as QuizUiState.Solving).pick(2)

        assertEquals(listOf(3, 2), second.picks)
        assertEquals(second.marks.size, second.picks.size)
    }

    @Test
    fun `같은 문제를 두 번 탭해도 picks 는 하나다`() {
        val graded = solving(1, 2).pick(0).pick(1)

        assertEquals(listOf(0), graded.picks)
    }
}
