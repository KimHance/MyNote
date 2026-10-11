package com.hancekim.mynote.core.ui.element

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesDraftsTest {
    @Test
    fun `RC-24 끊은 뒤 연결 화면을 다시 구성해도 새로 쓴 토큰 draft 가 남는다`() {
        val drafts = NotesDrafts()
        drafts.connectToken.text = "github_pat_old"

        drafts.consumeReset(1)
        assertEquals("", drafts.connectToken.text)

        drafts.connectToken.text = "github_pat_new"
        drafts.consumeReset(1)
        drafts.consumeReset(1)

        assertEquals("github_pat_new", drafts.connectToken.text)
    }

    @Test
    fun `RC-24 끊을 때마다 한 번씩만 draft 를 비운다`() {
        val drafts = NotesDrafts()

        drafts.consumeReset(1)
        drafts.connectToken.text = "github_pat_a"
        drafts.consumeReset(2)

        assertEquals("", drafts.connectToken.text)

        drafts.connectToken.text = "github_pat_b"
        drafts.consumeReset(2)
        assertEquals("github_pat_b", drafts.connectToken.text)
    }

    @Test
    fun `RC-24 처음부터 끊은 적이 없으면 draft 를 건드리지 않는다`() {
        val drafts = NotesDrafts()
        drafts.connectToken.text = "github_pat_x"

        drafts.consumeReset(0)

        assertEquals("github_pat_x", drafts.connectToken.text)
    }
}
