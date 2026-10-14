package com.hancekim.mynote.brand

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.font.decideTitleFont
import com.hancekim.mynote.core.designsystem.font.googleTitleFontSource
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.designsystem.theme.ProvideTitleFontFamily
import com.hancekim.mynote.feature.stats.GeneratedAgo
import com.hancekim.mynote.feature.stats.SummaryCardUi
import com.hancekim.mynote.feature.stats.SummaryLabel
import com.hancekim.mynote.feature.stats.screen.SummaryCard
import com.hancekim.mynote.feature.stats.screen.SummaryForm
import com.hancekim.mynote.feature.stats.screen.rememberStatsFormats
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

class TitleFontScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val gothicA1 = runBlocking { decideTitleFont(googleTitleFontSource(context)) { delay(LOAD_MILLIS) } }

    private var fontFamily by mutableStateOf<FontFamily>(FontFamily.Default)

    @Test
    fun `BR-8 통계 요약 문장은 두 글꼴에서 줄 수가 같다`() {
        val summary =
            SummaryCardUi(
                label = SummaryLabel.ThisWeek,
                generated = GeneratedAgo.Yesterday,
                headline = "android와 kotlin을 고르게 읽었고, cs 노트는 이번 주에 한 번도 열지 않았습니다.",
                detail = "남은 사흘 중 하루는 cs 노트로 시작해 보세요.",
                focus = "cs",
                stale = false,
                running = false,
                failed = false,
                focusInLegend = true,
            )
        show(width = SUMMARY_WIDTH, height = null) {
            SummaryCard(
                summary = summary,
                form = SummaryForm.Regular,
                formats = rememberStatsFormats(),
                onOpenHistory = {},
            )
        }
        val heights = eachFont("stats-summary") { listOf(rule.onNodeWithTag(SCREEN).height()) }
        assumeGothic()
        assertSameLines("요약 카드", heights[0][0], heights[1][0], line = (19 * 1.4f).dp)
    }

    private fun show(
        width: Dp,
        height: Dp?,
        windowClass: MyNoteWindowClass = MyNoteWindowClass.Compact,
        heightClass: MyNoteHeightClass = MyNoteHeightClass.Medium,
        content: @Composable () -> Unit,
    ) {
        val density = Density(context.resources.displayMetrics.widthPixels / (width.value + MARGIN_DP), fontScale = 1f)
        rule.setContent {
            ProvideTitleFontFamily(fontFamily) {
                MyNoteTheme(windowClass = windowClass, heightClass = heightClass) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                        CompositionLocalProvider(LocalDensity provides density) {
                            val size =
                                if (height !=
                                    null
                                ) {
                                    Modifier.requiredSize(width, height)
                                } else {
                                    Modifier.requiredWidth(width)
                                }
                            Box(
                                size
                                    .testTag(SCREEN)
                                    .background(MyNoteTheme.colors.bg)
                                    .padding(if (height == null) 16.dp else 0.dp),
                            ) { content() }
                        }
                    }
                }
            }
        }
    }

    private fun eachFont(
        shot: String,
        measure: (String) -> List<Dp>,
    ): List<List<Dp>> =
        listOf("device" to FontFamily.Default, "gothic-a1" to gothicA1).map { (name, family) ->
            fontFamily = family
            rule.waitForIdle()
            measure(
                name,
            ).also { save(rule.onNodeWithTag(SCREEN).captureToImage().asAndroidBitmap(), "$shot-$name.png") }
        }

    private fun assertSameLines(
        label: String,
        device: Dp,
        gothic: Dp,
        line: Dp,
    ) {
        File(dir(), "heights.txt").appendText("$label device=$device gothic=$gothic\n")
        assertTrue("$label 줄 수(기기 $device · Gothic A1 $gothic)", abs(device.value - gothic.value) < line.value)
    }

    private fun assumeGothic() = assumeTrue("Gothic A1 을 받지 못했다(글꼴 제공자 · 네트워크)", gothicA1 != FontFamily.Default)

    private fun SemanticsNodeInteraction.height(): Dp = getBoundsInRoot().let { it.bottom - it.top }

    private fun save(
        bitmap: Bitmap,
        name: String,
    ) {
        File(dir(), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

private fun dir(): File =
    File(InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir, "brand-font").apply { mkdirs() }

private val SUMMARY_WIDTH = 380.dp
private const val MARGIN_DP = 20f
private const val LOAD_MILLIS = 10_000L
private const val SCREEN = "title-font-screen"
