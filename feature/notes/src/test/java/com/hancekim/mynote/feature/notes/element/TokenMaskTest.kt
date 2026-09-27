package com.hancekim.mynote.feature.notes.element

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenMaskTest {
    @Test
    fun `앞 네 자와 뒤 네 자만 남기고 가린다`() {
        assertEquals("ghp_••••••••a3F9", maskToken("ghp_AbCdEfGhIjKlMnOpa3F9"))
    }

    @Test
    fun `가린 길이는 원본 길이를 드러내지 않는다`() {
        val short = maskToken("ghp_" + "x".repeat(10) + "a3F9")
        val long = maskToken("ghp_" + "x".repeat(200) + "a3F9")

        assertEquals(short, long)
    }

    @Test
    fun `토큰이 없으면 가릴 것도 없다`() {
        assertNull(maskToken(null))
        assertNull(maskToken(""))
        assertNull(maskToken("   "))
    }

    @Test
    fun `너무 짧으면 한 글자도 드러내지 않는다`() {
        assertEquals("••••••••", maskToken("ghp_1234"))
    }

    @Test
    fun `앞뒤 공백은 토큰의 일부가 아니다`() {
        assertEquals("ghp_••••••••a3F9", maskToken("  ghp_AbCdEfGhIjKlMnOpa3F9  "))
    }

    @Test
    fun `읽어 줄 끝자리만 따로 뽑는다`() {
        assertEquals("a3F9", maskedTokenTail("ghp_AbCdEfGhIjKlMnOpa3F9"))
        assertNull(maskedTokenTail("ghp_1234"))
    }
}
