package com.hancekim.mynote.core.ai

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class EngineSlotTest {
    private class FakeEngine : AutoCloseable {
        var closed = false

        override fun close() {
            closed = true
        }
    }

    private class Factory(
        private val gate: CountDownLatch? = null,
        private val started: CountDownLatch? = null,
    ) {
        val made = mutableListOf<FakeEngine>()

        fun create(): FakeEngine {
            started?.countDown()
            gate?.await(5, TimeUnit.SECONDS)
            return FakeEngine().also { made += it }
        }
    }

    @Test
    fun `워커가 새로 올린 엔진은 앱이 백그라운드면 닫고 포그라운드면 남긴다`() =
        runBlocking {
            val factory = Factory()
            val slot = EngineSlot(factory::create, log = {})

            val closedRun = slot.tryUse(shouldRelease = { true }) { "ok" } as EngineUse.Done
            assertTrue(closedRun.released)
            assertTrue(factory.made.single().closed)
            assertFalse(slot.loaded)

            val keptRun = slot.tryUse(shouldRelease = { false }) { "ok" } as EngineUse.Done
            assertFalse(keptRun.released)
            assertFalse(factory.made[1].closed)
            assertTrue(slot.loaded)
        }

    @Test
    fun `퀴즈가 올려 둔 엔진은 재사용만 하고 닫지 않는다`() =
        runBlocking {
            val factory = Factory()
            val slot = EngineSlot(factory::create, log = {})
            slot.use { }

            val run = slot.tryUse(shouldRelease = { true }) { "ok" } as EngineUse.Done

            assertNull(run.initMillis)
            assertFalse(run.released)
            assertEquals(1, factory.made.size)
            assertFalse(factory.made.single().closed)
        }

    @Test
    fun `엔진을 쓰는 중이면 기다리지 않고 Busy`() =
        runBlocking {
            val slot = EngineSlot(Factory()::create, log = {})
            val holding = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            val quiz =
                async(Dispatchers.Default) {
                    slot.use {
                        entered.complete(Unit)
                        holding.await()
                    }
                }
            entered.await()

            assertEquals(EngineUse.Busy, slot.tryUse(shouldRelease = { true }) { "ok" })

            holding.complete(Unit)
            quiz.await()
        }

    @Test
    fun `init 도중 취소돼도 만든 엔진을 버리지 않고 닫는다`() =
        runBlocking {
            val gate = CountDownLatch(1)
            val started = CountDownLatch(1)
            val factory = Factory(gate, started)
            val slot = EngineSlot(factory::create, log = {})
            val worker = async(Dispatchers.Default) { slot.tryUse(shouldRelease = { true }) { "never" } }
            started.await(5, TimeUnit.SECONDS)

            worker.cancel()
            gate.countDown()
            runCatching { worker.await() }

            assertEquals(1, factory.made.size)
            assertTrue(factory.made.single().closed)
            assertFalse(slot.loaded)
        }

    @Test
    fun `퀴즈 init 도중 그만둬도 엔진은 슬롯에 남아 다음 퀴즈가 재사용한다`() =
        runBlocking {
            val gate = CountDownLatch(1)
            val started = CountDownLatch(1)
            val factory = Factory(gate, started)
            val slot = EngineSlot(factory::create, log = {})
            val quiz = async(Dispatchers.Default) { slot.use { "never" } }
            started.await(5, TimeUnit.SECONDS)

            quiz.cancel()
            gate.countDown()
            runCatching { quiz.await() }
            slot.use { }

            assertEquals(1, factory.made.size)
            assertFalse(factory.made.single().closed)
            assertTrue(slot.loaded)
        }
}
