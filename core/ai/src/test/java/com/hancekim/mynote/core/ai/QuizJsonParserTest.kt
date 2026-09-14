package com.hancekim.mynote.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizJsonParserTest {
    @Test
    fun `깨끗한 JSON 을 그대로 읽는다`() {
        val result = QuizJsonParser.parse(CLEAN)

        val questions = result.getOrThrow()
        assertEquals(1, questions.size)
        assertEquals("LRU 캐시의 축출 기준은?", questions[0].question)
        assertEquals(4, questions[0].choices.size)
        assertEquals(2, questions[0].answerIndex)
    }

    @Test
    fun `앞뒤 군말과 코드펜스가 붙어도 읽는다`() {
        // 2B 모델이 실제로 이렇게 뱉는다. 엄격한 역직렬화면 여기서 전부 실패한다.
        val messy = "네, 퀴즈를 만들어 드릴게요!\n\n```json\n$CLEAN\n```\n\n도움이 되셨나요?"

        assertEquals(1, QuizJsonParser.parse(messy).getOrThrow().size)
    }

    @Test
    fun `answerIndex 를 1부터 센 경우 보정한다`() {
        val oneBased = CLEAN.replace("\"answerIndex\": 2", "\"answerIndex\": 4")

        assertEquals(3, QuizJsonParser.parse(oneBased).getOrThrow()[0].answerIndex)
    }

    @Test
    fun `망가진 문제는 버리고 멀쩡한 것만 남긴다`() {
        val mixed =
            """
            [
              { "question": "", "choices": ["a","b"], "answerIndex": 0, "explanation": "" },
              { "question": "정상", "choices": ["a","b"], "answerIndex": 1, "explanation": "설명" }
            ]
            """.trimIndent()

        val questions = QuizJsonParser.parse(mixed).getOrThrow()
        assertEquals(1, questions.size)
        assertEquals("정상", questions[0].question)
    }

    @Test
    fun `정답 번호가 범위를 벗어나면 그 문제를 버린다`() {
        val broken = CLEAN.replace("\"answerIndex\": 2", "\"answerIndex\": 9")

        assertTrue(QuizJsonParser.parse(broken).isFailure)
    }

    @Test
    fun `선택지가 하나뿐이면 문제로 치지 않는다`() {
        val single =
            """[{ "question": "q", "choices": ["only"], "answerIndex": 0, "explanation": "" }]"""

        assertTrue(QuizJsonParser.parse(single).isFailure)
    }

    @Test
    fun `JSON 배열이 아예 없으면 실패한다`() {
        assertTrue(QuizJsonParser.parse("죄송하지만 만들 수 없습니다.").isFailure)
    }

    @Test
    fun `JSON 이 깨져 있으면 실패한다`() {
        assertTrue(QuizJsonParser.parse("[{ \"question\": \"q\", ").isFailure)
    }

    private companion object {
        val CLEAN =
            """
            [
              {
                "question": "LRU 캐시의 축출 기준은?",
                "choices": ["가장 큰 항목", "가장 자주 쓴 항목", "가장 오래 안 쓴 항목", "무작위"],
                "answerIndex": 2,
                "explanation": "Least Recently Used 는 사용된 지 가장 오래된 항목을 버린다."
              }
            ]
            """.trimIndent()
    }
}
