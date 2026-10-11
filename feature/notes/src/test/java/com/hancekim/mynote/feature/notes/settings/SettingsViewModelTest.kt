package com.hancekim.mynote.feature.notes.settings

import com.hancekim.mynote.feature.notes.FeatureFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FM-9 지운 토큰을 되돌린다`() =
        runTest {
            val fixture = FeatureFixture(this, token = "ghp_stored0000a3F9")
            val vm = fixture.settings()
            fixture.subscribe(vm)
            assertTrue(vm.state.value.hasToken)

            vm.onIntent(SettingsIntent.ClearToken)
            runCurrent()
            assertEquals(false, vm.state.value.hasToken)
            val notice = vm.state.value.notice
            assertEquals("tokenCleared", notice?.message)
            assertEquals(SettingsIntent.UndoClearToken, notice?.undo)

            vm.onIntent(SettingsIntent.UndoClearToken)
            runCurrent()

            assertEquals(listOf(null, "ghp_stored0000a3F9"), fixture.tokens.saved)
            assertTrue(vm.state.value.hasToken)
        }

    @Test
    fun `FM-8 처리 실패는 주입한 기록기로 남긴다`() =
        runTest {
            val fixture = FeatureFixture(this, disconnect = Result.failure(IllegalStateException("boom")))
            val vm = fixture.settings()
            fixture.subscribe(vm)

            vm.onIntent(SettingsIntent.ConfirmDisconnect)
            runCurrent()

            assertEquals("boom", fixture.failures.single().message)
            assertEquals(
                "disconnectFailed",
                vm.state.value.notice
                    ?.message,
            )
        }
}
