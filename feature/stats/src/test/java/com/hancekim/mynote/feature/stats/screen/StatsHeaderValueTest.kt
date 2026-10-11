package com.hancekim.mynote.feature.stats.screen

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.token.myNoteTextStyles
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsHeaderValueTest {
    @Test
    fun `BR-8 통계 머리 숫자는 제목 글꼴을 쓰지 않고 바꾸기 전 모양 그대로다`() {
        val style = myNoteTextStyles(16f, titleFontFamily = FontFamily.Cursive).statsHeaderValue()

        val before =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 24.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.01).em,
                lineHeight = 30.sp,
            )
        assertEquals(before, style)
    }
}
