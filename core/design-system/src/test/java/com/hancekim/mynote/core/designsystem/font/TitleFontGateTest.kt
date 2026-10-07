package com.hancekim.mynote.core.designsystem.font

import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TitleFontGateTest {
    private val gothicA1 = FontFamily.Cursive

    @Test
    fun `BR-8 글꼴 문은 상한 안에 준비되면 Gothic A1 이다`() =
        runTest {
            val decided = timed { decideTitleFont(FakeSource(loadMillis = 300)) { delay(LAMP_MILLIS) } }
            assertEquals(gothicA1 to 300L, decided)
        }

    @Test
    fun `BR-8 글꼴을 못 받거나 늦거나 제공자가 없으면 기기 글꼴이다`() =
        runTest {
            val failing = FakeSource(failure = IllegalStateException())
            assertEquals(FontFamily.Default to 0L, timed { decideTitleFont(failing) { awaitCancellation() } })

            val late = FakeSource(loadMillis = LAMP_MILLIS + 1)
            assertEquals(FontFamily.Default to LAMP_MILLIS, timed { decideTitleFont(late) { delay(LAMP_MILLIS) } })

            val noProvider = FakeSource(available = false, loadMillis = LAMP_MILLIS * 10)
            assertEquals(FontFamily.Default to 0L, timed { decideTitleFont(noProvider) { awaitCancellation() } })
        }

    @Test
    fun `BR-8 첫 프레임이 늦으면 그동안 받은 글꼴을 쓰고 불 켜짐 뒤로는 기다리지 않는다`() =
        runTest {
            val slowFrame = CompletableDeferred<Unit>()
            launch {
                delay(2_000)
                slowFrame.complete(Unit)
            }
            val afterSlowFrame: suspend () -> Unit = {
                slowFrame.await()
                delay(LAMP_MILLIS - 2_000)
            }
            assertEquals(gothicA1 to 300L, timed { decideTitleFont(FakeSource(loadMillis = 300), afterSlowFrame) })

            val fastFrame = CompletableDeferred(Unit)
            val afterLamp: suspend () -> Unit = {
                fastFrame.await()
                delay(LAMP_MILLIS)
            }
            assertEquals(
                FontFamily.Default to LAMP_MILLIS,
                timed { decideTitleFont(FakeSource(loadMillis = 700), afterLamp) },
            )
        }

    @Test
    fun `BR-8 한 번 정한 글꼴은 프로세스 동안 바뀌지 않는다`() =
        runTest {
            val deviceGate = TitleFontGate()
            val late = FakeSource(loadMillis = LAMP_MILLIS * 2)
            assertEquals(FontFamily.Default, deviceGate.decide(late) { delay(LAMP_MILLIS) })
            val ready = FakeSource()
            assertEquals(FontFamily.Default, deviceGate.decide(ready) { awaitCancellation() })
            assertFalse(ready.loadCalled)
            assertEquals(FontFamily.Default, deviceGate.fontFamily)

            val gothicGate = TitleFontGate()
            assertEquals(gothicA1, gothicGate.decide(FakeSource()) { awaitCancellation() })
            val failing = FakeSource(failure = IllegalStateException())
            assertEquals(gothicA1, gothicGate.decide(failing) { awaitCancellation() })
            assertEquals(gothicA1, gothicGate.fontFamily)
        }

    @Test
    fun `BR-8 불 켜짐이 끝날 때까지 남은 시간은 실행 요청 시각에서 잰다`() {
        val cold =
            titleFontWaitMillis(
                processStartMillis = 1_000,
                createdMillis = 1_300,
                nowMillis = 1_400,
                lampMillis = LAMP_MILLIS,
            )
        assertEquals("콜드 — 프로세스 시작 요청이 기준", 150L, cold)

        val warm =
            titleFontWaitMillis(
                processStartMillis = 1_000,
                createdMillis = 60_000,
                nowMillis = 60_100,
                lampMillis = LAMP_MILLIS,
            )
        assertEquals("웜 — 오래전에 뜬 프로세스는 화면을 만든 때가 기준", 450L, warm)

        val reduced =
            titleFontWaitMillis(processStartMillis = 1_000, createdMillis = 1_050, nowMillis = 1_100, lampMillis = 0)
        assertEquals("감속 모션 — 불 켜짐 길이가 0 이면 기다리지 않는다", -100L, reduced)
    }

    private suspend fun TestScope.timed(block: suspend () -> FontFamily): Pair<FontFamily, Long> {
        val start = testScheduler.currentTime
        val fontFamily = block()
        return fontFamily to testScheduler.currentTime - start
    }

    private inner class FakeSource(
        private val available: Boolean = true,
        private val loadMillis: Long = 0,
        private val failure: Exception? = null,
    ) : TitleFontSource {
        var loadCalled = false

        override suspend fun isAvailable(): Boolean = available

        override suspend fun load(): FontFamily {
            loadCalled = true
            delay(loadMillis)
            failure?.let { throw it }
            return gothicA1
        }
    }
}

private const val LAMP_MILLIS = 550L
