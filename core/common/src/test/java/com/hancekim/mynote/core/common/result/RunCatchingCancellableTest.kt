package com.hancekim.mynote.core.common.result

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RunCatchingCancellableTest {
    @Test
    fun `성공하면 값을 담아 돌려준다`() {
        assertEquals(42, runCatchingCancellable { 42 }.getOrNull())
    }

    @Test
    fun `일반 예외는 Result 실패로 감싼다`() {
        val result = runCatchingCancellable { error("boom") }

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun `CancellationException 은 삼키지 않고 다시 던진다`() {
        var rethrown = false

        try {
            runCatchingCancellable { throw CancellationException("취소") }
            fail("취소 예외가 삼켜졌다")
        } catch (e: CancellationException) {
            rethrown = true
        }

        assertTrue(rethrown)
    }

    @Test
    fun `코루틴이 취소되면 블록 이후로 진행하지 않는다`() =
        runTest {
            var reachedAfterBlock = false

            val job =
                launch {
                    runCatchingCancellable { awaitCancellation() }
                    reachedAfterBlock = true
                }
            runCurrent()
            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertFalse(reachedAfterBlock)
        }
}
