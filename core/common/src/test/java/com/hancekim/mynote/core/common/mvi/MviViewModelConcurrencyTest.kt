package com.hancekim.mynote.core.common.mvi

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

private data class RaceState(val a: Int, val b: Int) : MviState

private sealed interface RaceIntent : MviEvent

private class RaceViewModel : MviViewModel<RaceState, RaceIntent>() {
    val a = trigger(0)
    val b = trigger(0)

    val builds = AtomicInteger()
    private val inFlight = AtomicInteger()

    /** [build] 가 동시에 겹쳐 실행된 최대 개수. 단일 collector 라면 항상 1이어야 한다. */
    val maxInFlight = AtomicInteger()

    override fun build(): RaceState {
        val now = inFlight.incrementAndGet()
        maxInFlight.updateAndGet { maxOf(it, now) }
        builds.incrementAndGet()
        val snapshot = RaceState(a.value, b.value)
        inFlight.decrementAndGet()
        return snapshot
    }

    override fun onIntent(event: RaceIntent) = Unit
}

/** 안드로이드 메인루퍼 흉내 — 자기 스레드에서 부르면 디스패치 없이 인라인 실행(`Main.immediate`). */
private class AndroidLikeMain : CoroutineDispatcher() {
    private val queue = LinkedBlockingQueue<Runnable>()

    @Volatile
    private var alive = true

    val thread = Thread({ while (alive) queue.take().run() }, "fake-main")
        .apply { isDaemon = true; start() }

    override fun isDispatchNeeded(context: CoroutineContext) = Thread.currentThread() !== thread

    override fun dispatch(context: CoroutineContext, block: Runnable) = queue.put(block)

    fun runOnMain(block: () -> Unit) {
        val done = CountDownLatch(1)
        queue.put(Runnable { try { block() } finally { done.countDown() } })
        done.await()
    }

    fun shutdown() {
        alive = false
        queue.put(Runnable { })
    }
}

/** 실제 스레드를 [CyclicBarrier] 로 모았다가 동시에 푼다. 실제 방출 횟수를 돌려준다. */
private fun hammer(totalOps: Int, threads: Int, op: (Int) -> Unit): Int {
    val per = totalOps / threads
    val barrier = CyclicBarrier(threads)
    val workers = List(threads) { t -> Thread { barrier.await(); repeat(per) { op(t) } } }
    workers.forEach { it.start() }
    workers.forEach { it.join() }
    return per * threads
}

private suspend fun awaitUntil(timeoutMs: Long = 10_000, cond: () -> Boolean): Boolean =
    withTimeoutOrNull(timeoutMs) { while (!cond()) delay(2); true } ?: false

private val CORES = Runtime.getRuntime().availableProcessors()
private const val OPS = 50_000

/** 가상시간이 아니라 실제 스레드로 때린다. Main = [Dispatchers.Default] 라 항상 디스패치된다. */
class MviViewModelConcurrencyTest {

    private val threads = CORES * 4

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Default)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `여러 스레드가 동시에 state 를 처음 읽어도 파이프라인은 한 번만 만들어진다`() {
        val vm = RaceViewModel()
        val seen = java.util.Collections.synchronizedList(mutableListOf<Any>())

        hammer(totalOps = threads, threads = threads) { seen += vm.state }

        seen.forEach { assertSame(seen.first(), it) }
        assertEquals(1, vm.builds.get())
    }

    @Test
    fun `여러 스레드가 동시에 갱신해도 최종 상태가 정확하고 build 는 겹치지 않는다`() = runBlocking {
        val vm = RaceViewModel()
        val job = launch(Dispatchers.Default) { vm.state.collect {} }
        awaitUntil { vm.builds.get() >= 1 }

        val n = hammer(OPS, threads) { t -> if (t % 2 == 0) vm.a.update { it + 1 } else vm.b.update { it + 1 } }

        assertTrue("최종 상태 불일치: ${vm.state.value}", awaitUntil { vm.state.value == RaceState(n / 2, n / 2) })
        assertEquals(1, vm.maxInFlight.get())
        job.cancel()
    }
}

/** Main = 안드로이드 유사(단일 스레드 + immediate 인라인) — 실제 ViewModel 이 도는 조건. */
@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelMainImmediateConcurrencyTest {

    private lateinit var main: AndroidLikeMain

    @Before
    fun setUp() {
        main = AndroidLikeMain()
        Dispatchers.setMain(main)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        main.shutdown()
    }

    @Test
    fun `메인에서 폭주 입력해도 build 는 재진입되지 않는다`() = runBlocking {
        val vm = RaceViewModel()
        val job = launch(main) { vm.state.collect {} }
        awaitUntil { vm.builds.get() >= 1 }

        // immediate 라 수집기가 같은 콜스택에서 인라인으로 깨어나는 경로.
        main.runOnMain { repeat(OPS) { vm.a.update { v -> v + 1 } } }

        assertTrue(awaitUntil { vm.state.value.a == OPS })
        assertEquals(1, vm.maxInFlight.get())
        job.cancel()
    }

    @Test
    fun `메인 입력과 백그라운드 스트림이 동시에 들어와도 최종 상태가 정확하다`() = runBlocking {
        val vm = RaceViewModel()
        val job = launch(main) { vm.state.collect {} }
        awaitUntil { vm.builds.get() >= 1 }

        val bg = Thread { repeat(OPS) { vm.b.update { v -> v + 1 } } }.apply { start() }
        main.runOnMain { repeat(OPS) { vm.a.update { v -> v + 1 } } }
        bg.join()

        assertTrue("최종 상태 불일치: ${vm.state.value}", awaitUntil { vm.state.value == RaceState(OPS, OPS) })
        assertEquals(1, vm.maxInFlight.get())
        job.cancel()
    }
}
