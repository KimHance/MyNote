package com.hancekim.mynote.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class Utf8SizeTest {
    @Test
    fun `LN-3 본문 크기는 글자 수가 아니라 UTF-8 바이트로 센다`() {
        val samples = listOf("a", "é", "가", "😀", "a가😀\n", "가".repeat(1_000))
        samples.forEach { text ->
            assertEquals(text, text.toByteArray(Charsets.UTF_8).size, Utf8Size.bytes(text))
        }
    }

    @Test
    fun `LN-7 짝 없는 대리 문자는 저장 판정처럼 3 바이트로 센다`() {
        // plan D46 — core/format MarkdownLimits 와 같은 셈(인코더의 ? 한 바이트가 아니다)
        assertEquals(3, Utf8Size.bytes("\uD83D"))
        assertEquals(3, Utf8Size.bytes("\uDE00"))
        assertEquals(1 + 3 + 4, Utf8Size.bytes("a\uD83D😀"))
    }

    @Test
    fun `LN-3 이유 줄의 KB 는 올림이다`() {
        assertEquals(0, Utf8Size.kilobytes(0))
        assertEquals(1, Utf8Size.kilobytes(1))
        assertEquals(512, Utf8Size.kilobytes(524_288))
        assertEquals(513, Utf8Size.kilobytes(524_289))
    }

    @Test
    fun `LN-3 이유 줄의 KB 는 앞머리 8 바이트를 더한 저장 원문 기준이다`() {
        // plan D44 — 저장 경계는 원문 524,288 바이트. 본문 524,281 바이트부터 넘고, 넘으면 늘 513KB 이상이다
        assertEquals(512, Utf8Size.savedKilobytes("a".repeat(524_280)))
        assertEquals(513, Utf8Size.savedKilobytes("a".repeat(524_281)))
        assertEquals(2, Utf8Size.savedKilobytes("a".repeat(1_017)))
        assertEquals(1, Utf8Size.savedKilobytes(""))
    }

    @Test
    fun `LN-7 경계를 넘으면 거기서 센 것을 멈춘다`() {
        val counted = Utf8Size.bytes("a".repeat(10_000), stopAfter = 100)
        assertEquals(101, counted)
    }
}
