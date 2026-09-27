package com.hancekim.mynote.core.ai.download.decision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResponseVerdictTest {
    @Test
    fun `206 이 요청한 자리에서 시작하면 이어 쓴다`() {
        val facts = ResponseFacts(206, "bytes 1000-1099/$TOTAL")

        assertEquals(Verdict.Append(1000L), judge(askedFrom = 1000L, facts = facts))
    }

    @Test
    fun `206 이 딴 자리에서 시작하면 처음부터 받는다`() {
        val facts = ResponseFacts(206, "bytes 0-1099/$TOTAL")

        assertEquals(Verdict.StartOver, judge(askedFrom = 1000L, facts = facts))
    }

    @Test
    fun `206 인데 Content-Range 가 없으면 처음부터 받는다`() {
        assertEquals(Verdict.StartOver, judge(askedFrom = 1000L, facts = ResponseFacts(206)))
    }

    @Test
    fun `200 을 처음부터 요청했으면 이어 쓴다`() {
        assertEquals(Verdict.Append(0L), judge(askedFrom = 0L, facts = ResponseFacts(200)))
    }

    @Test
    fun `이어받기를 요청했는데 200 이 오면 처음부터 받는다`() {
        assertEquals(Verdict.StartOver, judge(askedFrom = 1_100_000_000L, facts = ResponseFacts(200)))
    }

    @Test
    fun `416 이 길이를 알려주고 그게 요청한 자리면 이미 다 받은 것이다`() {
        val facts = ResponseFacts(416, "bytes */$TOTAL")

        assertEquals(Verdict.Complete, judge(askedFrom = TOTAL, facts = facts))
    }

    @Test
    fun `416 이 길이를 알려주지 않으면 끝난 것으로 보지 않는다`() {
        assertEquals(Verdict.StartOver, judge(askedFrom = TOTAL, facts = ResponseFacts(416)))
    }

    @Test
    fun `416 이 다른 길이를 알려주면 처음부터 받는다`() {
        val facts = ResponseFacts(416, "bytes */${TOTAL - 1}")

        assertEquals(Verdict.StartOver, judge(askedFrom = TOTAL, facts = facts))
    }

    @Test
    fun `429 는 서버가 말한 대기 시간을 그대로 전한다`() {
        val facts = ResponseFacts(429, retryAfterSeconds = 295L)

        assertEquals(Verdict.RetryLater(295L), judge(askedFrom = 0L, facts = facts))
    }

    @Test
    fun `503 은 대기 시간 없이도 재시도한다`() {
        assertEquals(Verdict.RetryLater(null), judge(askedFrom = 0L, facts = ResponseFacts(503)))
    }

    @Test
    fun `서버 오류는 재시도한다`() {
        listOf(500, 502, 504).forEach { status ->
            assertEquals(Verdict.RetryLater(null), judge(askedFrom = 0L, facts = ResponseFacts(status)))
        }
    }

    @Test
    fun `없는 파일은 재시도하지 않고 포기한다`() {
        val verdict = judge(askedFrom = 0L, facts = ResponseFacts(404))

        assertEquals(Verdict.GiveUp::class, verdict::class)
    }

    @Test
    fun `권한 오류도 포기한다`() {
        listOf(401, 403).forEach { status ->
            assertEquals(Verdict.GiveUp::class, judge(askedFrom = 0L, facts = ResponseFacts(status))::class)
        }
    }

    @Test
    fun `Content-Range 를 시작과 전체로 읽는다`() {
        assertEquals(ContentRange(start = 1000L, total = TOTAL), parseContentRange("bytes 1000-1099/$TOTAL"))
    }

    @Test
    fun `416 형식은 시작이 없고 전체만 있다`() {
        assertEquals(ContentRange(start = null, total = TOTAL), parseContentRange("bytes */$TOTAL"))
    }

    @Test
    fun `전체 길이를 모르는 형식도 읽는다`() {
        assertEquals(ContentRange(start = 0L, total = null), parseContentRange("bytes 0-99/*"))
    }

    @Test
    fun `Content-Range 가 없거나 쓰레기면 null 이다`() {
        assertNull(parseContentRange(null))
        assertNull(parseContentRange(""))
        assertNull(parseContentRange("이건 헤더가 아니다"))
        assertNull(parseContentRange("items 1000-1099/$TOTAL"))
    }

    private companion object {
        const val TOTAL = 2_588_147_712L
    }
}
