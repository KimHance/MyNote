package com.hancekim.mynote.core.ai.download.decision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HubHeadersTest {
    @Test
    fun `처음부터 받을 때는 Range 를 붙이지 않는다`() {
        assertNull(rangeHeader(0L))
    }

    @Test
    fun `이어받을 때는 그 자리부터 끝까지 요청한다`() {
        assertEquals("bytes=1100000000-", rangeHeader(1_100_000_000L))
    }
}
