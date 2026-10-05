package com.hancekim.mynote.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun note(vararg blocks: NoteBlock) = Note(id = "n", title = "t", date = "2026.10.01", blocks = blocks.toList())

private fun text(value: String) = NoteSpan(value)

private fun paragraph(vararg spans: NoteSpan) = NoteBlock.Paragraph(spans.toList())

class NoteExcerptTest {
    @Test
    fun `제목을 건너뛰고 첫 문단의 첫 문장을 고른다`() {
        val result =
            note(
                NoteBlock.Heading(1, listOf(text("TCP 3-way handshake"))),
                paragraph(text("연결을 맺기 전에 세 번 주고받습니다. 그래서 순서 번호가 맞습니다.")),
            ).firstSentence()

        assertEquals("연결을 맺기 전에 세 번 주고받습니다.", result)
    }

    @Test
    fun `코드 블록이 먼저 와도 건너뛴다`() {
        val result =
            note(
                NoteBlock.Code("fun main() = println(1)"),
                NoteBlock.Quote(listOf(text("인용은 건너뜁니다."))),
                NoteBlock.Divider,
                NoteBlock.Spacer,
                paragraph(text("본문입니다. 둘째.")),
            ).firstSentence()

        assertEquals("본문입니다.", result)
    }

    @Test
    fun `문단이 없으면 첫 목록 항목을 쓴다`() {
        val result =
            note(
                NoteBlock.BulletList(
                    listOf(
                        NoteListItem(listOf(text("  "))),
                        NoteListItem(listOf(text("첫 항목입니다! 둘째 문장."))),
                    ),
                ),
            ).firstSentence()

        assertEquals("첫 항목입니다!", result)
    }

    @Test
    fun `아무것도 없으면 null 이다`() {
        assertNull(note().firstSentence())
        assertNull(note(NoteBlock.Heading(2, listOf(text("제목만"))), NoteBlock.Code("x")).firstSentence())
    }

    @Test
    fun `숫자 사이의 점은 문장 끝이 아니다`() {
        val result = note(paragraph(text("버전 v2.0 은 1.5 배 빠릅니다. 다음."))).firstSentence()

        assertEquals("버전 v2.0 은 1.5 배 빠릅니다.", result)
    }

    @Test
    fun `코드 스팬 안의 점은 문장 끝이 아니다`() {
        val result =
            note(
                paragraph(
                    text("수집은 "),
                    NoteSpan("Flow.collect()", SpanStyle.Code),
                    text(" 로 합니다. 끝."),
                ),
            ).firstSentence()

        assertEquals("수집은 Flow.collect() 로 합니다.", result)
    }

    @Test
    fun `영문 약어 뒤의 점은 문장 끝이 아니다`() {
        val result = note(paragraph(text("Use a cache, e.g. Room. Then sync."))).firstSentence()

        assertEquals("Use a cache, e.g. Room.", result)
    }

    @Test
    fun `140자를 넘으면 마지막 공백에서 자르고 말줄임표를 붙인다`() {
        val word = "가나다라마바사 "
        val long = word.repeat(30).trim()
        val result = note(paragraph(text(long))).firstSentence()

        assertTrue(result!!.endsWith("…"))
        assertTrue(result.length <= 141)
        assertEquals(' ', long[result.length - 1])
    }

    @Test
    fun `굵게 스팬은 기호 없이 텍스트만 이어 붙인다`() {
        val result =
            note(
                paragraph(
                    text("리컴포지션은 "),
                    NoteSpan("상태가 바뀐 범위", SpanStyle.Bold),
                    text("만 다시 그립니다.\n줄바꿈   뒤."),
                ),
            ).firstSentence()

        assertEquals("리컴포지션은 상태가 바뀐 범위만 다시 그립니다.", result)
    }

    @Test
    fun `공백뿐인 문단은 건너뛴다`() {
        val result =
            note(
                paragraph(text("   \n\t")),
                paragraph(text("진짜 첫 문단입니다.")),
            ).firstSentence()

        assertEquals("진짜 첫 문단입니다.", result)
    }

    @Test
    fun `일본어 마침표 뒤에 공백이 오면 문장이 끝난다`() {
        val result = note(paragraph(text("これは文です。 次の文。"))).firstSentence()

        assertEquals("これは文です。", result)
    }

    @Test
    fun `마침표 뒤에 공백이 없으면 문장 끝으로 보지 않는다`() {
        val result = note(paragraph(text("これは文です。次の文。"))).firstSentence()

        assertEquals("これは文です。次の文。", result)
    }

    @Test
    fun `끝을 못 찾으면 문단 전체를 쓴다`() {
        val result = note(paragraph(text("마침표 없는 문단"))).firstSentence()

        assertEquals("마침표 없는 문단", result)
    }
}
