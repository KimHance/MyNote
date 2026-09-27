package com.hancekim.mynote.core.ai.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageSpaceTest {
    @Test
    fun `여유가 넉넉하면 받을 수 있다`() {
        assertTrue(enoughSpace(allocatableBytes = 8_000_000_000L, totalBytes = TOTAL, partBytes = 0L))
    }

    @Test
    fun `딱 맞아도 받을 수 있다`() {
        assertTrue(enoughSpace(allocatableBytes = TOTAL, totalBytes = TOTAL, partBytes = 0L))
    }

    @Test
    fun `한 바이트 부족하면 받을 수 없다`() {
        assertFalse(enoughSpace(allocatableBytes = TOTAL - 1, totalBytes = TOTAL, partBytes = 0L))
    }

    @Test
    fun `이미 받아 둔 만큼은 더 필요하지 않다`() {
        assertTrue(enoughSpace(allocatableBytes = 1L, totalBytes = TOTAL, partBytes = TOTAL - 1))
    }

    @Test
    fun `이미 받아 둔 것을 빼도 부족하면 받을 수 없다`() {
        assertFalse(enoughSpace(allocatableBytes = 1L, totalBytes = TOTAL, partBytes = TOTAL - 3))
    }

    @Test
    fun `여유가 Long 최대값이어도 넘치지 않는다`() {
        assertTrue(enoughSpace(allocatableBytes = Long.MAX_VALUE, totalBytes = TOTAL, partBytes = 1_000L))
    }

    @Test
    fun `이미 받은 것이 전체보다 크면 더 받을 필요가 없다`() {
        assertTrue(enoughSpace(allocatableBytes = 0L, totalBytes = TOTAL, partBytes = TOTAL + 1))
    }

    private companion object {
        const val TOTAL = 2_588_147_712L
    }
}
