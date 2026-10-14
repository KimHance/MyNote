package com.hancekim.mynote.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class Utf8SizeTest {
    @Test
    fun `LN-3 본문 크기는 글자 수가 아니라 UTF-8 바이트로 센다`() {
        val samples = listOf("a", "é", "가", "😀", "a가😀\n", "\uD83D", "가".repeat(1_000))
        samples.forEach { text ->
            assertEquals(text, text.toByteArray(Charsets.UTF_8).size, Utf8Size.bytes(text))
        }
    }

    @Test
    fun `LN-3 이유 줄의 KB 는 올림이다`() {
        assertEquals(0, Utf8Size.kilobytes(0))
        assertEquals(1, Utf8Size.kilobytes(1))
        assertEquals(512, Utf8Size.kilobytes(524_288))
        assertEquals(513, Utf8Size.kilobytes(524_289))
    }

    @Test
    fun `LN-7 경계를 넘으면 거기서 센 것을 멈춘다`() {
        val counted = Utf8Size.bytes("a".repeat(10_000), stopAfter = 100)
        assertEquals(101, counted)
    }
}
