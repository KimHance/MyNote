package com.hancekim.mynote.core.designsystem.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val NOISE = 6f
private const val ZONE = 48f

class FloatingNavScrollStateTest {
    private fun state() = FloatingNavScrollState(noiseThresholdPx = NOISE, alwaysVisibleZonePx = ZONE)

    @Test
    fun `미세한 떨림은 무시한다`() {
        val state = state()
        repeat(3) { state.onScroll(5f) }
        assertFalse(state.hidden)
    }

    @Test
    fun `상단 안전지대 안에서는 아래로 스크롤해도 숨지 않는다`() {
        val state = state()
        state.onScroll(ZONE - 1f)
        assertFalse(state.hidden)
    }

    @Test
    fun `안전지대를 넘겨 아래로 스크롤하면 숨는다`() {
        val state = state()
        state.onScroll(200f)
        assertTrue(state.hidden)
    }

    @Test
    fun `위로 스크롤하면 다시 나타난다`() {
        val state = state()
        state.onScroll(200f)
        assertTrue(state.hidden)

        state.onScroll(-120f)
        assertFalse(state.hidden)
    }

    @Test
    fun `누적 위치는 0 아래로 내려가지 않는다`() {
        val state = state()
        state.onScroll(-500f)
        state.onScroll(60f)
        // 0 에서 60 으로 올라온 것이므로 안전지대를 갓 넘겨 숨어야 한다
        assertTrue(state.hidden)
    }

    @Test
    fun `reset 은 숨김과 기준점을 함께 되돌린다`() {
        val state = state()
        state.onScroll(200f)
        assertTrue(state.hidden)

        state.reset()
        assertFalse(state.hidden)

        // 기준점까지 0 으로 돌아갔다면 안전지대 안에서는 다시 숨지 않는다
        state.onScroll(ZONE - 1f)
        assertFalse(state.hidden)
    }
}
