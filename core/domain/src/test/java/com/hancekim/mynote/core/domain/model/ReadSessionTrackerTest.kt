package com.hancekim.mynote.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private const val SECOND = 1_000L
private const val MINUTE = 60 * SECOND
private const val WALL_BASE = 1_790_000_000_000L

private fun at(elapsed: Long) = Moment(WALL_BASE + elapsed, elapsed)

private fun visibleTracker(): ReadSessionTracker =
    ReadSessionTracker().also { it.onVisible(true, at(0)) }

class ReadSessionTrackerTest {
    @Test
    fun `40초 읽고 닫으면 세션 하나`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))

        val closed = tracker.onTop(null, null, at(40 * SECOND))

        assertEquals(ReadSession("a", "android", WALL_BASE, WALL_BASE + 40 * SECOND, 40 * SECOND, 0), closed)
    }

    @Test
    fun `29_9초는 버린다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))

        assertNull(tracker.onTop(null, null, at(29_900)))
    }

    @Test
    fun `정확히 30초는 남긴다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))

        assertEquals(30 * SECOND, tracker.onTop(null, null, at(30 * SECOND))?.activeMillis)
    }

    @Test
    fun `61분은 60분으로 자르고 닫힌 시각은 그대로다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))

        val closed = tracker.onTop(null, null, at(61 * MINUTE))

        assertEquals(60 * MINUTE, closed?.activeMillis)
        assertEquals(WALL_BASE + 61 * MINUTE, closed?.endedAt)
    }

    @Test
    fun `퀴즈가 10분 덮고 1분 읽으면 활성은 1분이다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))
        tracker.onCovered(true, at(30 * SECOND))
        tracker.onCovered(false, at(10 * MINUTE + 30 * SECOND))

        val closed = tracker.onTop(null, null, at(11 * MINUTE))

        assertEquals(1 * MINUTE, closed?.activeMillis)
    }

    @Test
    fun `안 보였다가 다시 보이면 세션이 둘이 된다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))

        val first = tracker.onVisible(false, at(40 * SECOND))
        tracker.onVisible(true, at(100 * SECOND))
        val second = tracker.onTop(null, null, at(140 * SECOND))

        assertEquals(40 * SECOND, first?.activeMillis)
        assertEquals(40 * SECOND, second?.activeMillis)
        assertEquals(WALL_BASE + 100 * SECOND, second?.startedAt)
    }

    @Test
    fun `노트 a 에서 b 로 가면 각자 카테고리로 둘`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))
        val first = tracker.onTop("b", "kotlin", at(40 * SECOND))
        val second = tracker.onTop(null, null, at(80 * SECOND))

        assertEquals("a" to "android", first?.let { it.noteId to it.category })
        assertEquals("b" to "kotlin", second?.let { it.noteId to it.category })
    }

    @Test
    fun `다른 노트의 진행률은 무시한다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))
        tracker.onProgress("b", 80)

        assertEquals(0, tracker.onTop(null, null, at(40 * SECOND))?.maxPercent)
    }

    @Test
    fun `진행률은 최댓값만 남는다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))
        tracker.onProgress("a", 70)
        tracker.onProgress("a", 40)
        tracker.onProgress("a", 70)

        assertEquals(70, tracker.onTop(null, null, at(40 * SECOND))?.maxPercent)
    }

    @Test
    fun `안 보이는 동안 열린 리더는 보이는 순간부터 센다`() {
        val tracker = ReadSessionTracker()
        tracker.onTop("a", "android", at(0))
        tracker.onVisible(true, at(100 * SECOND))

        val closed = tracker.onTop(null, null, at(140 * SECOND))

        assertEquals(40 * SECOND, closed?.activeMillis)
        assertEquals(WALL_BASE + 100 * SECOND, closed?.startedAt)
    }

    @Test
    fun `같은 노트로 다시 top 이 와도 세션은 이어진다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))

        assertNull(tracker.onTop("a", "android", at(20 * SECOND)))
        val closed = tracker.onTop(null, null, at(40 * SECOND))

        assertNotNull(closed)
        assertEquals(40 * SECOND, closed?.activeMillis)
    }

    @Test
    fun `경과 시계가 되돌아가면 그 구간은 0 으로 센다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(50 * SECOND))

        val closed = tracker.onTop(null, null, Moment(WALL_BASE + 90 * SECOND, 10 * SECOND))

        assertNull(closed)
    }

    @Test
    fun `보임 재개 세션은 그 노트의 마지막 비율에서 시작한다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))
        tracker.onProgress("a", 100)

        val before = tracker.onVisible(false, at(40 * SECOND))
        tracker.onVisible(true, at(60 * SECOND))
        val after = tracker.onTop(null, null, at(100 * SECOND))

        assertEquals(100, before?.maxPercent)
        assertEquals(100, after?.maxPercent)
        assertEquals(40 * SECOND, after?.activeMillis)
    }

    @Test
    fun `노트가 바뀌면 이전 노트의 비율을 가져가지 않는다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))
        tracker.onProgress("a", 80)
        tracker.onTop("b", "kotlin", at(40 * SECOND))

        assertEquals(0, tracker.onTop(null, null, at(80 * SECOND))?.maxPercent)
    }

    @Test
    fun `다른 화면을 다녀온 세션은 셸이 다시 보낸 지금 비율로 시작한다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))
        tracker.onProgress("a", 90)
        tracker.onTop(null, null, at(40 * SECOND))
        tracker.onTop("a", "android", at(50 * SECOND))
        tracker.onProgress("a", 60)

        assertEquals(60, tracker.onTop(null, null, at(90 * SECOND))?.maxPercent)
    }

    @Test
    fun `세션 안에서 위로 올라가도 최댓값은 내려가지 않는다`() {
        val tracker = visibleTracker()
        tracker.onTop("a", "android", at(0))
        tracker.onProgress("a", 100)
        tracker.onVisible(false, at(40 * SECOND))
        tracker.onVisible(true, at(50 * SECOND))
        tracker.onProgress("a", 20)

        assertEquals(100, tracker.onTop(null, null, at(90 * SECOND))?.maxPercent)
    }
}
