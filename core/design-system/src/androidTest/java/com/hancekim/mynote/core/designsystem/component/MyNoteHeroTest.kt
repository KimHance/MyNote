package com.hancekim.mynote.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.platform.app.InstrumentationRegistry
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class MyNoteHeroTest {
    @get:Rule
    val rule = createComposeRule()

    private val shortName = "octo-learner/android-notes"
    private val longName =
        "study-group-of-the-android-platform-org" + "/" +
            "kotlin-coroutines-flow-and-compose-state-notes-reading-log-for-the-autumn-semester-2026-finaledition"

    private fun show(
        repoName: String?,
        heightClass: MyNoteHeightClass = MyNoteHeightClass.Medium,
        windowClass: MyNoteWindowClass = MyNoteWindowClass.Compact,
        width: Dp = 360.dp,
    ) {
        rule.setContent {
            MyNoteTheme(windowClass = windowClass, heightClass = heightClass) {
                Box(Modifier.testTag(FRAME).width(width).background(MyNoteTheme.colors.bg)) {
                    MyNoteHero(title = TITLE, repoName = repoName, hint = HINT)
                }
            }
        }
    }

    private fun capture(name: String) {
        val bitmap = rule.onNodeWithTag(FRAME).captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.getExternalFilesDir(null), "hero").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun repoLayout(name: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(name).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }

    @Test
    fun `BR-6 긴 이름도 한 줄이고 높이가 같다`() {
        assertEquals(140, longName.length)
        var name by mutableStateOf(shortName)
        rule.setContent {
            MyNoteTheme {
                Box(Modifier.testTag(FRAME).width(360.dp).background(MyNoteTheme.colors.bg)) {
                    MyNoteHero(title = TITLE, repoName = name, hint = HINT)
                }
            }
        }
        val shortHeight = rule.onNodeWithTag(FRAME).getUnclippedBoundsInRoot().height

        name = longName
        rule.waitForIdle()

        val layout = repoLayout(longName)
        assertEquals(1, layout.lineCount)
        assertTrue(layout.isLineEllipsized(0))
        val frame = rule.onNodeWithTag(FRAME).getUnclippedBoundsInRoot()
        val line = rule.onNodeWithText(longName).getUnclippedBoundsInRoot()
        assertTrue(line.right <= frame.right)
        assertEquals(shortHeight, frame.height)
        capture("hero-long-medium")
    }

    @Test
    fun `BR-6 히어로 제목은 MyNote 로 읽히는 제목이다`() {
        show(shortName)

        rule
            .onNodeWithContentDescription(TITLE)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule
            .onNodeWithTag(HERO_MARK_TAG)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        rule.onAllNodesWithContentDescription(TITLE).assertCountEquals(1)
        rule.onNodeWithText(shortName).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        capture("hero-repo-medium")
    }

    @Test
    fun `BR-6 저장소 줄은 전체 이름을 읽는다`() {
        show(longName)

        rule
            .onNodeWithText(longName)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString(longName))))
    }

    @Test
    fun `BR-6 높이 Compact 에서는 마크가 없다`() {
        show(longName, heightClass = MyNoteHeightClass.Compact, windowClass = MyNoteWindowClass.Medium, width = 520.dp)

        rule.onNodeWithTag(HERO_MARK_TAG).assertDoesNotExist()
        rule.onNodeWithContentDescription(TITLE).assertExists()
        rule.onNodeWithText(longName).assertExists()
        rule.onNodeWithText(HINT).assertDoesNotExist()
        capture("hero-long-compact")
    }

    @Test
    fun `BR-6 연결이 없으면 저장소 줄이 없다`() {
        var name by mutableStateOf<String?>(shortName)
        rule.setContent {
            MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Expanded) {
                Box(Modifier.testTag(FRAME).width(560.dp).background(MyNoteTheme.colors.bg)) {
                    MyNoteHero(title = TITLE, repoName = name, hint = HINT)
                }
            }
        }
        val repoTop = rule.onNodeWithText(shortName).getUnclippedBoundsInRoot().top

        name = null
        rule.waitForIdle()

        rule.onNodeWithTag(HERO_MARK_TAG).assertExists()
        rule.onNode(hasContentDescription(TITLE)).assertExists()
        rule.onAllNodes(anyText).assertCountEquals(1)
        rule.onNode(anyText).assertTextEquals(HINT)
        assertEquals(repoTop, rule.onNodeWithText(HINT).getUnclippedBoundsInRoot().top)
        capture("hero-none-expanded")
    }

    @Test
    fun `BR-6 워드마크는 글자 배율로 커지지 않는다`() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Expanded) {
                    Box(Modifier.testTag(FRAME).width(560.dp).background(MyNoteTheme.colors.bg)) {
                        MyNoteHero(title = TITLE, repoName = shortName, hint = HINT)
                    }
                }
            }
        }

        rule.onNodeWithContentDescription(TITLE).assertHeightIsEqualTo((28f * WORDMARK_HEIGHT / WORDMARK_X_HEIGHT).dp)
        rule.onNodeWithTag(HERO_MARK_TAG).assertHeightIsEqualTo(54.dp)
        capture("hero-repo-expanded-x2")
    }

    private companion object {
        const val FRAME = "hero_frame"
        const val TITLE = "MyNote"
        const val HINT = "읽고 싶은 노트를 고르세요."
        val anyText = SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)
    }
}
