package com.hancekim.mynote.core.navigation

import com.hancekim.mynote.core.navigation.key.NotesNavKey
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val WINDOW = 400L

class NavigatorTest {
    private val home = NotesNavKey.Home
    private val a = NotesNavKey.Reader("a")
    private val b = NotesNavKey.Reader("b")

    @Test
    fun `처음에는 루트만 있고 전환이 끝난 상태다`() =
        runTest {
            val navigator = Navigator(this, home, WINDOW) { currentTime }

            assertEquals(listOf(home), navigator.stack.value)
            assertTrue(navigator.isSettled.value)
        }

    @Test
    fun `push 하면 스택이 쌓이고 창이 열린다`() =
        runTest {
            val navigator = Navigator(this, home, WINDOW) { currentTime }

            assertEquals(NavResult.Applied, navigator.push(a))

            assertEquals(listOf(home, a), navigator.stack.value)
            assertFalse(navigator.isSettled.value)
            advanceUntilIdle()
        }

    @Test
    fun `전환 중에 들어온 이동은 버려지고 스택이 그대로다`() =
        runTest {
            val navigator = Navigator(this, home, WINDOW) { currentTime }
            navigator.push(a)

            advanceTimeBy(WINDOW - 1)
            assertEquals(NavResult.Throttled, navigator.push(b))

            assertEquals(listOf(home, a), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `전환 중에는 pop 도 막힌다`() =
        runTest {
            val navigator = Navigator(this, home, WINDOW) { currentTime }
            navigator.push(a)

            advanceTimeBy(WINDOW - 1)
            assertEquals(NavResult.Throttled, navigator.pop())

            assertEquals(listOf(home, a), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `창이 지나면 다시 통과한다`() =
        runTest {
            val navigator = Navigator(this, home, WINDOW) { currentTime }
            navigator.push(a)
            advanceUntilIdle()

            assertTrue(navigator.isSettled.value)
            assertEquals(NavResult.Applied, navigator.pop())

            assertEquals(listOf(home), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `맨 위와 같은 키를 또 push 하면 창이 열리지 않는다`() =
        runTest {
            val navigator = Navigator(this, home, WINDOW) { currentTime }
            navigator.push(a)
            advanceUntilIdle()

            assertEquals(NavResult.Ignored, navigator.push(a))

            // 이동이 없었으므로 막을 것도 없다 — 다음 진짜 이동이 바로 통과해야 한다.
            assertTrue(navigator.isSettled.value)
            assertEquals(NavResult.Applied, navigator.push(b))
            advanceUntilIdle()
        }

    @Test
    fun `루트에서 pop 하면 더 갈 데가 없다고 답한다`() =
        runTest {
            val navigator = Navigator(this, home, WINDOW) { currentTime }

            assertEquals(NavResult.Ignored, navigator.pop())

            assertEquals(listOf(home), navigator.stack.value)
            assertTrue(navigator.isSettled.value)
        }

    @Test
    fun `전환 중 루트에서 pop 하면 Ignored 가 아니라 Throttled 다`() =
        runTest {
            // 이 둘이 섞이면 전환 중 뒤로가기가 앱 종료로 샌다.
            val navigator = Navigator(this, home, WINDOW) { currentTime }
            navigator.push(a)
            advanceTimeBy(WINDOW - 1)
            navigator.pop() // 막힘 — 스택은 [home, a] 그대로

            assertEquals(NavResult.Throttled, navigator.pop())
            advanceUntilIdle()
        }

    @Test
    fun `dispatchImmediately 는 전환 중에도 적용된다`() =
        runTest {
            val navigator = Navigator(this, home, WINDOW) { currentTime }
            navigator.push(a)
            advanceTimeBy(WINDOW - 1)

            navigator.dispatchImmediately(NavCommand.ReplaceAll(listOf(home)))

            assertEquals(listOf(home), navigator.stack.value)
            advanceUntilIdle()
        }

    @Test
    fun `창은 마지막 이동을 기준으로 다시 시작한다`() =
        runTest {
            val navigator = Navigator(this, home, WINDOW) { currentTime }
            navigator.push(a)
            advanceUntilIdle()
            navigator.push(b)

            advanceTimeBy(WINDOW - 1)
            assertFalse(navigator.isSettled.value)

            advanceUntilIdle()
            assertTrue(navigator.isSettled.value)
        }
}
