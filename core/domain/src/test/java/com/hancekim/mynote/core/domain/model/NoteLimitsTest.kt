package com.hancekim.mynote.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteLimitsTest {
    @Test
    fun `NF-8 한도 값은 32단과 512KB 다`() {
        assertEquals(32, NoteLimits.Default.maxDepth)
        assertEquals(512, NoteLimits.Default.maxKilobytes)
        assertEquals(524_288, NoteLimits.Default.maxBytes)
    }
}
