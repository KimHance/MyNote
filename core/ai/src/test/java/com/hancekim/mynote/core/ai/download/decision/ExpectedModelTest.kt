package com.hancekim.mynote.core.ai.download.decision

import com.hancekim.mynote.core.ai.download.ModelSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpectedModelTest {
    @Test
    fun `MD-2 앱 상수는 소문자 hex 64자 sha256 과 2588147712 바이트다`() {
        val expected = ModelSource.expected

        assertTrue(expected.sha256, LOWER_HEX_64.matches(expected.sha256))
        assertEquals(VERIFIED_SHA256, expected.sha256)
        assertEquals(2_588_147_712L, expected.totalBytes)
    }

    private companion object {
        val LOWER_HEX_64 = Regex("^[0-9a-f]{64}$")
        const val VERIFIED_SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
    }
}
