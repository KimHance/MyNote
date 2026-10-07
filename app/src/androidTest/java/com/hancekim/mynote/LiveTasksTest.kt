package com.hancekim.mynote

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveTasksTest {
    @Test
    fun `BR-4 같은 태스크의 이전 화면이 아직 살아 있으면 앱이 스플래시를 내리지 않는다`() {
        val tasks = LiveTasks()
        assertTrue(tasks.enter(TASK))
        assertFalse(tasks.enter(TASK))
        tasks.leave(TASK)
        assertFalse("둘 중 하나만 끝나면 아직 이전 화면이 남아 있다", tasks.enter(TASK))
        tasks.leave(TASK)
        tasks.leave(TASK)
        assertTrue(tasks.enter(TASK))
    }

    @Test
    fun `BR-4 이전 화면이 먼저 끝났거나 다른 태스크면 앱이 스플래시를 내린다`() {
        val tasks = LiveTasks()
        assertTrue(tasks.enter(TASK))
        tasks.leave(TASK)
        assertTrue(tasks.enter(TASK))
        assertTrue(tasks.enter(TASK + 1))
    }

    private companion object {
        const val TASK = 485
    }
}
