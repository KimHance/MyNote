package com.hancekim.mynote.core.ai.download.decision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HubHeadersTest {
    @Test
    fun `세 헤더로 신분증을 만든다`() {
        val plan = planFrom(size = "2588147712", etag = "\"$SHA\"", commit = COMMIT)

        assertEquals(DownloadPlan(COMMIT, SHA, 2_588_147_712L), plan)
    }

    @Test
    fun `etag 의 따옴표를 벗긴다`() {
        assertEquals(SHA, planFrom("1", "\"$SHA\"", COMMIT)?.sha256)
    }

    @Test
    fun `weak etag 의 접두사도 벗긴다`() {
        assertEquals(SHA, planFrom("1", "W/\"$SHA\"", COMMIT)?.sha256)
    }

    @Test
    fun `따옴표가 없어도 그대로 읽는다`() {
        assertEquals(SHA, planFrom("1", SHA, COMMIT)?.sha256)
    }

    @Test
    fun `헤더가 하나라도 없으면 신분증을 못 만든다`() {
        assertNull(planFrom(null, "\"$SHA\"", COMMIT))
        assertNull(planFrom("1", null, COMMIT))
        assertNull(planFrom("1", "\"$SHA\"", null))
    }

    @Test
    fun `크기가 숫자가 아니거나 0 이하면 못 만든다`() {
        assertNull(planFrom("무한", "\"$SHA\"", COMMIT))
        assertNull(planFrom("0", "\"$SHA\"", COMMIT))
        assertNull(planFrom("-1", "\"$SHA\"", COMMIT))
    }

    @Test
    fun `처음부터 받을 때는 Range 를 붙이지 않는다`() {
        assertNull(rangeHeader(0L))
    }

    @Test
    fun `이어받을 때는 그 자리부터 끝까지 요청한다`() {
        assertEquals("bytes=1100000000-", rangeHeader(1_100_000_000L))
    }

    private companion object {
        const val COMMIT = "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1"
        const val SHA = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
    }
}
