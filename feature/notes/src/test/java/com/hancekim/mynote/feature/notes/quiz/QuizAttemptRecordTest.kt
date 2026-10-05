package com.hancekim.mynote.feature.notes.quiz

import com.hancekim.mynote.core.domain.model.QuizAnswer
import com.hancekim.mynote.feature.notes.QuizQuestionUi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private val meta = QuizMeta(noteId = "http-cache", startedAt = 1_000L)

private fun question(
    text: String,
    answer: Int,
    choices: Int = 4,
) = QuizQuestionUi(
    question = text,
    choices = (0 until choices).map { "$text-보기$it" }.toImmutableList(),
    answerIndex = answer,
    explanation = "설명",
)

private val threeQuestions =
    persistentListOf(question("캐시란", 1), question("ETag 는", 0, choices = 3), question("304 는", 2))

private fun solveAll(vararg picks: Int): Pair<QuizUiState.Solving, QuizUiState> {
    var state = QuizUiState.Solving(questions = threeQuestions)
    picks.forEachIndexed { index, pick ->
        state = state.pick(pick)
        if (index < picks.lastIndex) state = state.advance() as QuizUiState.Solving
    }
    return state to state.advance()
}

class QuizAttemptRecordTest {
    @Test
    fun `마지막 문항에서 결과로 넘어가면 시도 하나가 된다`() {
        val (before, after) = solveAll(1, 2, 2)

        val attempt = completedAttempt(before, after, meta, "cs", finishedAt = 9_000L)

        assertNotNull(attempt)
        assertEquals("http-cache", attempt?.noteId)
        assertEquals("cs", attempt?.category)
        assertEquals(1_000L, attempt?.startedAt)
        assertEquals(9_000L, attempt?.finishedAt)
        assertEquals(3, attempt?.asked)
        assertEquals(2, attempt?.correct)
    }

    @Test
    fun `문항 텍스트와 선택지와 정답과 고른 번호를 그대로 남긴다`() {
        val (before, after) = solveAll(1, 2, 2)

        val answers = completedAttempt(before, after, meta, "cs", 9_000L)?.answers

        assertEquals(
            QuizAnswer("ETag 는", listOf("ETag 는-보기0", "ETag 는-보기1", "ETag 는-보기2"), 0, 2),
            answers?.get(1),
        )
        assertEquals(listOf(1, 2, 2), answers?.map { it.pickedIndex })
        assertEquals(before.marks.size, before.picks.size)
    }

    @Test
    fun `결과 전 문항 넘김은 기록하지 않는다`() {
        val solving = QuizUiState.Solving(questions = threeQuestions).pick(1)

        assertNull(completedAttempt(solving, solving.advance(), meta, "cs", 9_000L))
    }

    @Test
    fun `그만두기는 기록하지 않는다`() {
        val solving = QuizUiState.Solving(questions = threeQuestions).pick(1)

        assertNull(completedAttempt(solving, QuizUiState.Idle, meta, "cs", 9_000L))
    }

    @Test
    fun `생성 실패나 준비 상태에서는 기록하지 않는다`() {
        val failed = QuizUiState.Failed(QuizUiState.Failed.Kind.Timeout, 3, "http-cache")

        assertNull(completedAttempt(QuizUiState.Loading("http-cache", 3), failed, meta, "cs", 9_000L))
        assertNull(completedAttempt(QuizUiState.Setup("http-cache"), QuizUiState.Idle, meta, "cs", 9_000L))
    }

    @Test
    fun `시작 정보가 없으면 기록하지 않는다`() {
        val (before, after) = solveAll(1, 2, 2)

        assertNull(completedAttempt(before, after, null, "cs", 9_000L))
    }

    @Test
    fun `picks 가 문항 수와 어긋나면 기록하지 않는다`() {
        val broken =
            QuizUiState.Solving(
                questions = threeQuestions,
                index = 2,
                picked = 2,
                marks = persistentListOf(true, false, true),
                picks = persistentListOf(1, 2),
            )

        assertNull(completedAttempt(broken, broken.advance(), meta, "cs", 9_000L))
    }
}
