package com.hancekim.mynote.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderScrollEffectsTest {
    @Test
    fun `측정 전에는 비율을 내지 않는다`() {
        assertNull(seenPercent(value = 0, maxValue = Int.MAX_VALUE, viewportSize = 0))
    }

    @Test
    fun `한 화면에 다 들어오면 100`() {
        assertEquals(100, seenPercent(value = 0, maxValue = 0, viewportSize = 1_000))
    }

    @Test
    fun `맨 위에서는 한 화면만큼 본 비율이다`() {
        assertEquals(33, seenPercent(value = 0, maxValue = 2_000, viewportSize = 1_000))
    }

    @Test
    fun `끝까지 내리면 100 이고 중간은 내림이다`() {
        assertEquals(100, seenPercent(value = 2_000, maxValue = 2_000, viewportSize = 1_000))
        assertEquals(66, seenPercent(value = 999, maxValue = 2_000, viewportSize = 1_000))
    }
}
