package com.hancekim.mynote.core.ai.download.decision

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadProgressTest {
    @Test
    fun `전체 크기를 모르면 0퍼센트다`() {
        assertEquals(0, percentOf(received = 1_000L, total = 0L))
    }

    @Test
    fun `퍼센트는 내림한다`() {
        assertEquals(42, percentOf(received = 1_090_000_000L, total = TOTAL))
    }

    @Test
    fun `다 받으면 100퍼센트다`() {
        assertEquals(100, percentOf(received = TOTAL, total = TOTAL))
    }

    @Test
    fun `전체보다 많이 받아도 100을 넘기지 않는다`() {
        assertEquals(100, percentOf(received = TOTAL + 1_000L, total = TOTAL))
    }

    @Test
    fun `기가바이트는 10억으로 나눠 소수 둘째 자리까지 쓴다`() {
        assertEquals("2.59", gigabytesOf(TOTAL))
    }

    @Test
    fun `받은 게 없으면 0점00 이다`() {
        assertEquals("0.00", gigabytesOf(0L))
    }

    @Test
    fun `음수는 0으로 본다`() {
        assertEquals("0.00", gigabytesOf(-1L))
    }

    private companion object {
        const val TOTAL = 2_588_147_712L
    }
}
