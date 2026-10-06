package com.hancekim.mynote.core.designsystem.block

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteSpansTest {
    private fun spoken(vararg spans: NoteContentSpan): String? = spans.toList().spokenWithStrike("취소선", "취소선 끝")

    @Test
    fun `NF-4 취소선 구간을 낭독 글자에서 알린다`() {
        val result =
            spoken(
                NoteContentSpan("a ", NoteSpanStyle.Normal),
                NoteContentSpan("b", NoteSpanStyle.Normal, strike = true),
                NoteContentSpan(" c", NoteSpanStyle.Normal),
            )

        assertEquals("a 취소선 b 취소선 끝 c", result)
    }

    @Test
    fun `NF-4 이어진 취소선 구간은 모양이 달라도 한 번만 알린다`() {
        val result =
            spoken(
                NoteContentSpan("UI 에서 ", NoteSpanStyle.Normal, strike = true),
                NoteContentSpan("runBlocking", NoteSpanStyle.Code, strike = true),
                NoteContentSpan(" 셋 다", NoteSpanStyle.Bold, italic = true, strike = true),
            )

        assertEquals("취소선 UI 에서 runBlocking 셋 다 취소선 끝", result)
    }

    @Test
    fun `NF-4 취소선이 없으면 낭독 글자를 바꾸지 않는다`() {
        val result =
            spoken(
                NoteContentSpan("굵게", NoteSpanStyle.Bold, italic = true),
                NoteContentSpan("code", NoteSpanStyle.Code),
            )

        assertNull(result)
    }
}
