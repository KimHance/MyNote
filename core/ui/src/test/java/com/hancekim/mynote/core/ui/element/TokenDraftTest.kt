package com.hancekim.mynote.core.ui.element

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenDraftTest {
    @Test
    fun `RC-8 토큰 입력 객체 문자열에 원문이 없다`() {
        val raw = "github_pat_AbCdEfGh1234"
        val draft = TokenDraft()
        draft.text = raw

        assertFalse(draft.toString().contains(raw))
        assertFalse(draft.toString().contains("AbCd"))
        assertFalse(draft.secret().toString().contains(raw))
        assertFalse(NotesDrafts().toString().contains(raw))
    }

    @Test
    fun `RC-8 classic 접두사면 경고 상태다`() {
        assertTrue(isClassicToken("ghp_AbCdEfGh1234"))
        assertTrue(isClassicToken("  ghp_AbCdEfGh1234"))
        assertFalse(isClassicToken("github_pat_AbCdEfGh1234"))
        assertFalse(isClassicToken("gho_AbCdEfGh1234"))
        assertFalse(isClassicToken(""))
        val draft = TokenDraft()
        draft.text = "ghp_xxxxxxxx"
        assertTrue(draft.isClassic)
    }

    @Test
    fun `RC-8 지우면 입력과 보기 여부가 비워진다`() {
        val draft = TokenDraft()
        draft.text = "github_pat_x"
        draft.revealed = true

        draft.clear()

        assertEquals("", draft.text)
        assertFalse(draft.revealed)
        assertTrue(draft.isBlank)
    }
}
