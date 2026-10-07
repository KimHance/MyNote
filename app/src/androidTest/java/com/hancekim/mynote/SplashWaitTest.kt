package com.hancekim.mynote

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashWaitTest {
    @Test
    fun `BR-4 움직임 시작이나 길이를 모르면 바로 내려간다`() {
        assertEquals(0L, splashWaitMillis(startMillis = null, durationMillis = DURATION, scale = 1f, nowMillis = START))
        assertEquals(0L, splashWaitMillis(startMillis = START, durationMillis = null, scale = 1f, nowMillis = START))
    }

    @Test
    fun `BR-4 움직임 끝이 이미 지났으면 바로 내려간다`() {
        assertEquals(0L, splashWaitMillis(START, DURATION, scale = 1f, nowMillis = START + DURATION))
        assertEquals(0L, splashWaitMillis(START, DURATION, scale = 1f, nowMillis = START + DURATION + 900))
    }

    @Test
    fun `BR-4 남은 시간은 움직임 길이에 애니메이터 배율을 곱한 끝까지다`() {
        assertEquals(516L, splashWaitMillis(START, DURATION, scale = 1f, nowMillis = START + 100))
        assertEquals(208L, splashWaitMillis(START, DURATION, scale = 0.5f, nowMillis = START + 100))
        assertEquals(6060L, splashWaitMillis(START, DURATION, scale = 10f, nowMillis = START + 100))
    }

    @Test
    fun `BR-4 애니메이터 배율이 0 이면 기다리지 않는다`() {
        assertEquals(0L, splashWaitMillis(START, DURATION, scale = 0f, nowMillis = START))
        assertEquals(0L, splashWaitMillis(START, DURATION, scale = 0f, nowMillis = START + 100))
    }

    private companion object {
        const val START = 1_760_000_000_000L
        const val DURATION = 616L
    }
}
