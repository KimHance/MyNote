package com.hancekim.mynote.core.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CancellableTest {
    @Test
    fun `취소되면 네이티브 호출을 멈추고 그 뒤 실패를 엔진 오류가 아니라 취소로 던진다`() =
        runBlocking {
            val started = CountDownLatch(1)
            val stop = CountDownLatch(1)
            var cancels = 0
            val job =
                async(Dispatchers.Default) {
                    cancellable(
                        cancel = {
                            cancels++
                            stop.countDown()
                        },
                    ) {
                        started.countDown()
                        stop.await(5, TimeUnit.SECONDS)
                        throw IllegalStateException("native cancelled")
                    }
                }
            started.await(5, TimeUnit.SECONDS)
            job.cancel()

            val thrown = runCatching { job.await() }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            assertEquals(1, cancels)
        }

    @Test
    fun `타임아웃도 cancelProcess 를 부른다`() =
        runBlocking {
            val stop = CountDownLatch(1)
            var cancels = 0

            val thrown =
                runCatching {
                    withTimeout(100) {
                        withContext(Dispatchers.Default) {
                            cancellable(cancel = {
                                cancels++
                                stop.countDown()
                            }) { stop.await(5, TimeUnit.SECONDS) }
                        }
                    }
                }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            assertEquals(1, cancels)
        }

    @Test
    fun `취소가 아닐 때의 실패는 그대로 던진다`() =
        runBlocking {
            val thrown =
                runCatching {
                    cancellable(
                        cancel = {},
                    ) { throw IllegalStateException("boom") }
                }.exceptionOrNull()

            assertTrue(thrown is IllegalStateException)
        }
}
