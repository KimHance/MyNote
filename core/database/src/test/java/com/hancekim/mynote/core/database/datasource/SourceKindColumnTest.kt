package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.domain.model.SourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceKindColumnTest {
    @Test
    fun `저장값은 enum 이름이 아니라 고정 문자열이다`() {
        assertEquals("github", SourceKind.GitHub.toColumn())
        assertEquals("link", SourceKind.Link.toColumn())
        assertEquals("local", SourceKind.Local.toColumn())
    }

    @Test
    fun `저장값을 되읽으면 같은 종류로 돌아온다`() {
        SourceKind.entries.forEach { assertEquals(it, it.toColumn().toSourceKind()) }
    }

    @Test
    fun `모르는 값은 던지지 않고 버린다`() {
        assertNull("GitHub".toSourceKind())
        assertNull("".toSourceKind())
    }

    @Test
    fun `퀴즈 형식 기본값은 llm 이다`() {
        assertEquals("llm", QuizFormatColumn.LLM)
        assertEquals("card", QuizFormatColumn.CARD)
    }
}
