package com.hancekim.mynote.shell

import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FM-9 루트에서 창 안에 두 번 누르면 종료한다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val vm = fixture.shell()
            fixture.subscribe(vm)

            vm.onIntent(ShellIntent.Back)
            assertEquals(1, vm.state.value.exitHintId)
            assertEquals(0, vm.state.value.exitRequestId)

            advanceTimeBy(EXIT_WINDOW - 1)
            vm.onIntent(ShellIntent.Back)

            assertEquals(1, vm.state.value.exitRequestId)
        }

    @Test
    fun `FM-9 창이 지나면 다시 안내한다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val vm = fixture.shell()
            fixture.subscribe(vm)

            vm.onIntent(ShellIntent.Back)
            advanceTimeBy(EXIT_WINDOW)
            vm.onIntent(ShellIntent.Back)

            assertEquals(2, vm.state.value.exitHintId)
            assertEquals(0, vm.state.value.exitRequestId)

            advanceTimeBy(EXIT_WINDOW - 1)
            vm.onIntent(ShellIntent.Back)
            assertEquals(1, vm.state.value.exitRequestId)
        }

    @Test
    fun `RC-1 첫 화면 홈의 뒤로는 종료 안내이고 쌓은 연결 화면의 뒤로는 이전 화면이다`() =
        runTest {
            val fixture = FeatureFixture(this)
            val vm = fixture.shell()
            fixture.subscribe(vm)

            // 쌓은 연결 화면(홈 첫 실행 빈 상태의 `저장소 연결`) — 뒤로 = 홈, 종료 안내 없음
            fixture.navigator.dispatchImmediately(NavCommand.Push(NotesNavKey.Connect))
            fixture.settle()
            vm.onIntent(ShellIntent.Back)
            assertEquals(listOf(NotesNavKey.Home), fixture.navigator.stack.value)
            assertEquals(0, vm.state.value.exitHintId)

            // 첫 화면 홈 — 한 번은 종료 안내, 창 안의 두 번째가 종료
            fixture.settle()
            vm.onIntent(ShellIntent.Back)
            assertEquals(1, vm.state.value.exitHintId)
            vm.onIntent(ShellIntent.Back)
            assertEquals(1, vm.state.value.exitRequestId)
        }

    private companion object {
        const val EXIT_WINDOW = 1_000L
    }
}
