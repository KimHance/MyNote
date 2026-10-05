package com.hancekim.mynote.core.ai.insight

import com.hancekim.mynote.core.domain.model.InsightFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightJsonParserTest {
    private val categories = listOf("android", "cs", "kotlin")

    private fun summary(raw: String) = InsightJsonParser.summary(raw, categories)

    @Test
    fun `코드펜스와 앞말이 붙어도 읽는다`() {
        val draft =
            summary(
                "네, 정리했습니다.\n```json\n{\"headline\":\"한 줄\",\"detail\":\"다음 단계\",\"focus\":\"cs\"}\n```",
            ).getOrThrow()

        assertEquals(SummaryDraft("한 줄", "다음 단계", "cs"), draft)
    }

    @Test
    fun `필드가 없으면 한 번 다시 시킬 수 있는 MissingField`() {
        assertTrue(summary("{\"headline\":\"한 줄\"}").exceptionOrNull() is InsightFailure.MissingField)
        assertTrue(summary("{\"detail\":\"x\",\"headline\":\"  \"}").exceptionOrNull() is InsightFailure.MissingField)
    }

    @Test
    fun `48 자 headline 은 통과하고 49 자는 자르지 않고 버린다`() {
        val ok = "가".repeat(48)
        val over = "가".repeat(49)

        assertEquals(ok, summary("{\"headline\":\"$ok\",\"detail\":\"d\"}").getOrThrow().headline)
        assertTrue(summary("{\"headline\":\"$over\",\"detail\":\"d\"}").exceptionOrNull() is InsightFailure.TooLong)
    }

    @Test
    fun `detail 100 자 초과도 TooLong`() {
        assertTrue(
            summary(
                "{\"headline\":\"h\",\"detail\":\"${"나".repeat(101)}\"}",
            ).exceptionOrNull() is InsightFailure.TooLong,
        )
    }

    @Test
    fun `모르는 focus 는 null 이고 대문자는 목록 표기로`() {
        assertNull(summary("{\"headline\":\"h\",\"detail\":\"d\",\"focus\":\"swift\"}").getOrThrow().focus)
        assertNull(summary("{\"headline\":\"h\",\"detail\":\"d\",\"focus\":null}").getOrThrow().focus)
        assertEquals("cs", summary("{\"headline\":\"h\",\"detail\":\"d\",\"focus\":\"CS\"}").getOrThrow().focus)
    }

    @Test
    fun `줄바꿈과 연속 공백은 공백 하나로`() {
        assertEquals("첫 줄 둘째 줄", summary("{\"headline\":\"첫 줄\\n  둘째   줄\",\"detail\":\"d\"}").getOrThrow().headline)
    }

    @Test
    fun `닫는 괄호가 없는 잘린 JSON 은 Parse`() {
        assertTrue(summary("{\"headline\":\"h\",\"detail\":\"다음 단계를").exceptionOrNull() is InsightFailure.Parse)
    }

    @Test
    fun `중첩 객체는 무시한다`() {
        assertEquals(
            "h",
            summary(
                "{\"headline\":\"h\",\"detail\":\"d\",\"meta\":{\"a\":1},\"focus\":\"kotlin\"}",
            ).getOrThrow().headline,
        )
    }

    @Test
    fun `tags 5 개는 앞 3 개만`() {
        val draft =
            InsightJsonParser
                .pattern(
                    "{\"tags\":[\"a\",\"b\",\"c\",\"d\",\"e\"],\"sentence\":\"문장\"}",
                ).getOrThrow()

        assertEquals(listOf("a", "b", "c"), draft.tags)
    }

    @Test
    fun `tags 나 sentence 가 비면 MissingField`() {
        assertTrue(
            InsightJsonParser
                .pattern(
                    "{\"tags\":[],\"sentence\":\"문장\"}",
                ).exceptionOrNull() is InsightFailure.MissingField,
        )
        assertTrue(InsightJsonParser.pattern("{\"sentence\":\"문장\"}").exceptionOrNull() is InsightFailure.MissingField)
        assertTrue(InsightJsonParser.pattern("{\"tags\":[\"a\"]}").exceptionOrNull() is InsightFailure.MissingField)
    }

    @Test
    fun `sentence 120 자 초과와 tag 12 자 초과는 TooLong`() {
        assertTrue(
            InsightJsonParser
                .pattern(
                    "{\"tags\":[\"a\"],\"sentence\":\"${"다".repeat(121)}\"}",
                ).exceptionOrNull() is InsightFailure.TooLong,
        )
        assertTrue(
            InsightJsonParser
                .pattern(
                    "{\"tags\":[\"${"라".repeat(13)}\"],\"sentence\":\"s\"}",
                ).exceptionOrNull() is InsightFailure.TooLong,
        )
    }

    @Test
    fun `12 자 넘는 태그 하나는 자르지 않고 빼고 나머지를 살린다`() {
        val draft =
            InsightJsonParser.pattern("{\"tags\":[\"If-Modified-Since\",\"ETag\"],\"sentence\":\"문장\"}").getOrThrow()

        assertEquals(listOf("ETag"), draft.tags)
    }

    @Test
    fun `잘림은 벤치마크 없이 닫는 괄호로 판정한다`() {
        assertTrue(InsightJsonParser.truncated("{\"headline\":\"h\",\"detail\":\"다음 단계를"))
        assertTrue(InsightJsonParser.truncated("네 {\"tags\":[\"a\"],\"sentence\":\"중괄호 } 는 문자열 안"))
        assertFalse(InsightJsonParser.truncated("{\"headline\":\"h\",\"detail\":\"d\"} 끝"))
        assertFalse(InsightJsonParser.truncated("JSON 이 없는 답"))
    }
}
