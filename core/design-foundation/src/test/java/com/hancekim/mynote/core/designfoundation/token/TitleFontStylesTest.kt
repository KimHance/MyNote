package com.hancekim.mynote.core.designfoundation.token

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import org.junit.Assert.assertEquals
import org.junit.Test

class TitleFontStylesTest {
    private val gothicA1 = FontFamily.Cursive

    @Test
    fun `BR-8 제목 글꼴 스타일만 Gothic A1 을 쓴다`() {
        for (scale in MyNoteFontScale.entries) {
            for (height in MyNoteHeightClass.entries) {
                val device = styles(myNoteTextStyles(scale.bodySizeSp, height))
                val titled = styles(myNoteTextStyles(scale.bodySizeSp, height, titleFontFamily = gothicA1))
                val case = "$scale · $height"

                assertEquals(case, MY_NOTE_TEXT_STYLE_COUNT, titled.size)
                assertEquals(case, TitleFontStyleNames, titled.filterValues { it.fontFamily == gothicA1 }.keys)
                for ((name, style) in titled - TitleFontStyleNames) {
                    assertEquals("$case · $name", device.getValue(name), style)
                    val before = if (name.startsWith("mono")) FontFamily.Monospace else FontFamily.Default
                    assertEquals("$case · $name", before, style.fontFamily)
                }
                for (name in TitleFontStyleNames) {
                    val expected = device.getValue(name).copy(fontFamily = gothicA1)
                    assertEquals("$case · $name", expected, titled.getValue(name))
                }
            }
        }
    }

    @Test
    fun `BR-8 제목 글꼴 스타일은 줄 높이와 줄 상자를 고정한다`() {
        val center = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
        for (height in MyNoteHeightClass.entries) {
            val titled = styles(myNoteTextStyles(16f, height, titleFontFamily = gothicA1))
            val screenTitle = if (height == MyNoteHeightClass.Compact) 17f else 19f
            val expected =
                mapOf(
                    "noteTitle" to (28 * 1.25f).sp,
                    "title" to (24 * 1.3f).sp,
                    "sectionTitle" to (21 * 1.3f).sp,
                    "screenTitle" to (screenTitle * 1.3f).sp,
                )
            assertEquals(TitleFontStyleNames, expected.keys)
            for ((name, lineHeight) in expected) {
                val style = titled.getValue(name)
                assertEquals("$height · $name", lineHeight, style.lineHeight)
                assertEquals("$height · $name", center, style.lineHeightStyle)
            }
        }
    }

    private fun styles(textStyles: MyNoteTextStyles): Map<String, TextStyle> =
        MyNoteTextStyles::class.java.declaredFields
            .filter { it.type == TextStyle::class.java }
            .associate { field ->
                field.isAccessible = true
                field.name to field.get(textStyles) as TextStyle
            }
}

private const val MY_NOTE_TEXT_STYLE_COUNT = 19
