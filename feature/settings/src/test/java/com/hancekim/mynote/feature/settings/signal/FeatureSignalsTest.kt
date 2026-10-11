package com.hancekim.mynote.feature.settings.signal

import com.hancekim.mynote.feature.settings.FeatureFixture
import com.hancekim.mynote.feature.settings.SettingsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 기능 사이 신호(FM-6) 가운데 설정 ViewModel 하나만 보는 것 — 노트 · 셸과 같이 보는 것은 `:app` 의 같은 이름 클래스(FM-13). */
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureSignalsTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FM-6 새로 만든 설정 ViewModel 은 지난 설정 열기로 편집을 정리하지 않는다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val fixture = FeatureFixture(this)
            fixture.signals.enterSettings()

            val settings = fixture.settings()
            settings.onIntent(SettingsIntent.StartTokenEdit)
            runCurrent()

            assertTrue(settings.state.value.tokenEditing)
        }
}
