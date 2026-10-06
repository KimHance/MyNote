package com.hancekim.mynote.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteLimitsTest {
    @Test
    fun `NF-8 한도 값은 32단과 512KB 다`() {
        assertEquals(32, NoteLimits.Default.maxDepth)
        assertEquals(512, NoteLimits.Default.maxKilobytes)
        assertEquals(524_288, NoteLimits.Default.maxBytes)
    }

    @Test
    fun `NF-8 결과 한도 값은 표 칸 4,096 과 캐시 1MB 다`() {
        assertEquals(4_096, NoteLimits.Default.maxTableCells)
        assertEquals(1_024, NoteLimits.Default.maxCacheKilobytes)
        assertEquals(1_048_576, NoteLimits.Default.maxCacheBytes)
    }

    @Test
    fun `NF-8 노트 전체 상자 사유만 노트 전체를 대신한다`() {
        assertTrue(UnsupportedReason.BrokenNote.wholeNote)
        assertTrue(UnsupportedReason.LimitExceeded.wholeNote)
        assertFalse(UnsupportedReason.UnknownType.wholeNote)
        assertFalse(UnsupportedReason.BrokenBlock.wholeNote)
    }
}
