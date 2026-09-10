package com.hancekim.mynote.core.common.mvi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

private sealed interface TestIntent : MviEvent {
    data class Type(val text: String) : TestIntent
    data object NextPage : TestIntent
}

private data class PlainState(val keyword: String, val note: String) : MviState

/** trigger 하나만 등록 — trigger 등록 경로를 우회로 없이 검증하기 위해. */
private class PlainViewModel : MviViewModel<PlainState, TestIntent>() {
    val keyword = trigger("")
    var note = "초기"
    var buildCount = 0

    override fun build(): PlainState {
        buildCount++
        return PlainState(keyword.value, note)
    }

    override fun onIntent(event: TestIntent) {
        when (event) {
            is TestIntent.Type -> keyword.value = event.text
            TestIntent.NextPage -> Unit
        }
    }
}

private data class CountState(val count: Int) : MviState

/** 파생 없이 바깥 Flow 하나만 등록 */
private class SimpleStreamViewModel(upstream: Flow<Int>) : MviViewModel<CountState, TestIntent>() {
    val count = upstream.triggerIn(0)
    override fun build() = CountState(count.value)
    override fun onIntent(event: TestIntent) = Unit
}

/** upstream 을 살려두도록 옵트인한 VM. */
private class LazyStreamViewModel(upstream: Flow<Int>) : MviViewModel<CountState, TestIntent>() {
    val count = upstream.triggerIn(0, started = SharingStarted.Lazily)
    override fun build() = CountState(count.value)
    override fun onIntent(event: TestIntent) = Unit
}

private data class StreamState(val count: Int, val doubled: Int) : MviState

/** 외부 Flow 와 파생 값을 등록한다. */
private class StreamViewModel(upstream: Flow<Int>) : MviViewModel<StreamState, TestIntent>() {
    val count = upstream.triggerIn(0)
    val doubled = count.map { it * 2 }.triggerIn(0)

    override fun build() = StreamState(count.value, doubled.value)
    override fun onIntent(event: TestIntent) = Unit
}

/** Flow 를 return 하는 유스케이스. 호출 인자를 기록하고, 결과는 [delayMs] 뒤에 방출 */
private class FakeSearch(private val delayMs: Long = 100, private val failOn: String? = null) {
    val calls = mutableListOf<String>()

    operator fun invoke(query: String) = flow {
        calls += query
        delay(delayMs.milliseconds)
        if (query == failOn) error("boom")
        emit(listOf("hit:$query"))
    }
}

private data class SearchState(val keyword: String, val page: Int, val results: List<String>) :
    MviState

/** trigger 두 개 + triggerIn  */
@OptIn(ExperimentalCoroutinesApi::class)
private class SearchViewModel(search: FakeSearch) : MviViewModel<SearchState, TestIntent>() {
    val keyword = trigger("")
    val page = trigger(1)
    val results = keyword
        .flatMapLatest { search(it) }
        .catch { emit(emptyList()) }
        .triggerIn(emptyList())

    override fun build() = SearchState(keyword.value, page.value, results.value)

    override fun onIntent(event: TestIntent) {
        when (event) {
            is TestIntent.Type -> keyword.value = event.text
            TestIntent.NextPage -> page.value += 1
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <S : MviState> TestScope.subscribe(
        vm: MviViewModel<S, TestIntent>,
        sink: MutableList<S>? = null,
    ): Job {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect { sink?.add(it) }
        }
        advanceUntilIdle()
        return job
    }

    // --- 초기값 ---

    @Test
    fun `구독 전 state 는 build 결과를 초기값으로 갖는다`() = runTest(dispatcher) {
        assertEquals(PlainState("", "초기"), PlainViewModel().state.value)
    }

    @Test
    fun `첫 구독 때 상태가 그대로면 중복 방출하지 않는다`() = runTest(dispatcher) {
        val seen = mutableListOf<PlainState>()

        subscribe(PlainViewModel(), seen)

        assertEquals(1, seen.size)
    }

    // --- 재조립 트리거 ---

    @Test
    fun `onIntent 로 trigger 를 바꾸면 재조립된다`() = runTest(dispatcher) {
        val vm = PlainViewModel()
        subscribe(vm)

        vm.onIntent(TestIntent.Type("kotlin"))
        advanceUntilIdle()

        assertEquals("kotlin", vm.state.value.keyword)
    }

    @Test
    fun `triggerIn 의 upstream 이 방출하면 재조립된다`() = runTest(dispatcher) {
        val upstream = MutableStateFlow(0)
        val vm = StreamViewModel(upstream)
        subscribe(vm)

        upstream.value = 21
        advanceUntilIdle()

        assertEquals(21, vm.state.value.count)
    }

    @Test
    fun `앞줄 값을 재료로 만든 파생 triggerIn 도 재조립을 깨운다`() = runTest(dispatcher) {
        val upstream = MutableStateFlow(0)
        val vm = StreamViewModel(upstream)
        subscribe(vm)

        upstream.value = 21
        advanceUntilIdle()

        assertEquals(42, vm.state.value.doubled)
    }

    @Test
    fun `등록하지 않은 값은 바뀌어도 재조립되지 않는다`() = runTest(dispatcher) {
        val vm = PlainViewModel()
        subscribe(vm)
        val builds = vm.buildCount

        vm.note = "바뀜"
        advanceUntilIdle()

        assertEquals(builds, vm.buildCount)
        assertEquals("초기", vm.state.value.note)
    }

    @Test
    fun `trigger 에 같은 값을 다시 넣으면 재조립되지 않는다`() = runTest(dispatcher) {
        val vm = PlainViewModel()
        subscribe(vm)
        vm.onIntent(TestIntent.Type("kotlin"))
        advanceUntilIdle()
        val builds = vm.buildCount

        vm.onIntent(TestIntent.Type("kotlin"))
        advanceUntilIdle()

        assertEquals(builds, vm.buildCount)
    }

    // --- 조립 일관성 ---

    @Test
    fun `중간 방출이 conflate 돼도 마지막 상태는 정확하다`() = runTest(dispatcher) {
        val vm = PlainViewModel()
        val seen = mutableListOf<PlainState>()
        subscribe(vm, seen)

        repeat(5) { vm.onIntent(TestIntent.Type("v$it")) }
        advanceUntilIdle()

        assertEquals("v4", seen.last().keyword)
    }

    // --- state 구독 수명 ---

    @Test
    fun `구독이 타임아웃 안에 재개되면 다시 조립하지 않는다`() = runTest(dispatcher) {
        val vm = PlainViewModel()
        subscribe(vm).cancel()
        val builds = vm.buildCount

        advanceTimeBy(1_000)
        subscribe(vm)

        assertEquals(builds, vm.buildCount)
    }

    @Test
    fun `타임아웃이 지난 뒤 재구독하면 다시 조립한다`() = runTest(dispatcher) {
        val vm = PlainViewModel()
        subscribe(vm).cancel()
        val builds = vm.buildCount

        advanceTimeBy(5_001)
        subscribe(vm)

        assertEquals(builds + 1, vm.buildCount)
    }

    // --- triggerIn upstream 수명 ---

    @Test
    fun `구독자가 없으면 upstream 을 수집하지 않는다`() = runTest(dispatcher) {
        var started = 0

        StreamViewModel(MutableStateFlow(0).onStart { started++ })
        advanceUntilIdle()

        assertEquals(0, started)
    }

    @Test
    fun `구독이 끊기고 타임아웃이 지나면 upstream 수집이 멈춘다`() = runTest(dispatcher) {
        var stopped = 0
        val vm = SimpleStreamViewModel(MutableStateFlow(0).onCompletion { stopped++ })
        subscribe(vm).cancel()

        // 수명 정책은 state 한 곳뿐 — 5초가 지나면 upstream 도 곧바로 따라 멈춘다.
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(0, stopped)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, stopped)
    }

    @Test
    fun `파생 triggerIn 을 거쳐도 정지 타임아웃은 쌓이지 않는다`() = runTest(dispatcher) {
        var stopped = 0
        val vm = StreamViewModel(MutableStateFlow(0).onCompletion { stopped++ })
        subscribe(vm).cancel()

        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(0, stopped)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, stopped)
    }

    @Test
    fun `triggerIn 에 Lazily 를 주면 구독이 끊겨도 계속 수집한다`() = runTest(dispatcher) {
        var started = 0
        var stopped = 0
        val vm = LazyStreamViewModel(
            MutableStateFlow(0).onStart { started++ }.onCompletion { stopped++ },
        )

        assertEquals(0, started)          // 구독 전에는 Lazily 도 시작하지 않는다
        subscribe(vm).cancel()
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(1, started)
        assertEquals(0, stopped)          // ViewModel 이 살아있는 한 멈추지 않는다
    }

    // --- trigger 여러 개 ---

    @Test
    fun `첫 번째가 아닌 trigger 만 바뀌어도 재조립된다`() = runTest(dispatcher) {
        val vm = SearchViewModel(FakeSearch())
        subscribe(vm)

        vm.onIntent(TestIntent.NextPage)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.page)
    }
    @Test
    fun `한 trigger 가 바뀌어도 다른 trigger 값은 유지된다`() = runTest(dispatcher) {
        val vm = SearchViewModel(FakeSearch())
        subscribe(vm)

        vm.onIntent(TestIntent.NextPage)
        vm.onIntent(TestIntent.NextPage)
        vm.onIntent(TestIntent.Type("kotlin"))
        advanceUntilIdle()

        assertEquals(SearchState("kotlin", 3, listOf("hit:kotlin")), vm.state.value)
    }

    // --- 유스케이스 스트림 ---

    @Test
    fun `입력은 즉시 반영되고 유스케이스 결과는 도착 시점에 반영된다`() = runTest(dispatcher) {
        val vm = SearchViewModel(FakeSearch(delayMs = 100))
        subscribe(vm)
        val before = vm.state.value.results

        vm.onIntent(TestIntent.Type("kotlin"))
        runCurrent()
        assertEquals("kotlin", vm.state.value.keyword)
        assertEquals(before, vm.state.value.results)

        advanceTimeBy(101)
        runCurrent()
        assertEquals(listOf("hit:kotlin"), vm.state.value.results)
    }

    @Test
    fun `연속 입력이면 이전 유스케이스 호출 결과는 버려진다`() = runTest(dispatcher) {
        val search = FakeSearch(delayMs = 100)
        val vm = SearchViewModel(search)
        val seen = mutableListOf<SearchState>()
        subscribe(vm, seen)

        vm.onIntent(TestIntent.Type("a"))
        runCurrent()
        vm.onIntent(TestIntent.Type("b"))
        advanceUntilIdle()

        assertEquals(listOf("", "a", "b"), search.calls)
        assertEquals(listOf("hit:b"), vm.state.value.results)
        assertEquals(emptyList<String>(), seen.map { it.results }.filter { it == listOf("hit:a") })
    }

    @Test
    fun `유스케이스가 실패해도 catch 로 폴백하면 재조립이 이어진다`() = runTest(dispatcher) {
        val vm = SearchViewModel(FakeSearch(failOn = "boom"))
        subscribe(vm)

        vm.onIntent(TestIntent.Type("boom"))
        advanceUntilIdle()
        assertEquals(emptyList<String>(), vm.state.value.results)

        vm.onIntent(TestIntent.NextPage)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.page)
    }

}
