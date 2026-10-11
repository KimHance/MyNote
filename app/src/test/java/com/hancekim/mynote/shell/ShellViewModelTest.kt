package com.hancekim.mynote.shell

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

    private companion object {
        const val EXIT_WINDOW = 1_000L
    }
}
