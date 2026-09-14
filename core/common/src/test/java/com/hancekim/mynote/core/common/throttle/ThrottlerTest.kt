package com.hancekim.mynote.core.common.throttle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrottlerTest {
    private var now = 0L
    private val throttler = Throttler(minIntervalMillis = 500L, clock = { now })

    @Test
    fun `첫 호출은 통과한다`() {
        assertTrue(throttler.tryAccept())
    }

    @Test
    fun `최소 간격 안에 들어온 호출은 버린다`() {
        assertTrue(throttler.tryAccept())
        now += 499
        assertFalse(throttler.tryAccept())
    }

    @Test
    fun `최소 간격이 지나면 다시 통과한다`() {
        assertTrue(throttler.tryAccept())
        now += 500
        assertTrue(throttler.tryAccept())
    }

    @Test
    fun `버려진 호출은 기준 시점을 밀지 않는다`() {
        assertTrue(throttler.tryAccept())
        now += 300
        assertFalse(throttler.tryAccept())
        now += 200
        assertTrue(throttler.tryAccept())
    }

    @Test
    fun `연타해도 최소 간격마다 한 번만 실행된다`() {
        var ran = 0
        repeat(20) {
            throttler.process { ran++ }
            now += 100
        }
        assertEquals(4, ran)
    }

    @Test
    fun `process 는 실행 여부를 돌려준다`() {
        assertTrue(throttler.process {})
        assertFalse(throttler.process {})
    }

    @Test
    fun `reset 하면 다음 호출이 통과한다`() {
        assertTrue(throttler.tryAccept())
        assertFalse(throttler.tryAccept())
        throttler.reset()
        assertTrue(throttler.tryAccept())
    }
}
