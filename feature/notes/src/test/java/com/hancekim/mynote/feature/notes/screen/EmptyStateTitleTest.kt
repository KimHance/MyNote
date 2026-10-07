package com.hancekim.mynote.feature.notes.screen

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.token.myNoteTextStyles
import org.junit.Assert.assertEquals
import org.junit.Test

class EmptyStateTitleTest {
    @Test
    fun `BR-8 빈 상태 제목은 제목 글꼴 17sp 이고 title 의 줄 높이를 물려받지 않는다`() {
        val styles = myNoteTextStyles(16f, titleFontFamily = FontFamily.Cursive)
        val style = styles.emptyStateTitle()

        assertEquals(17.sp, style.fontSize)
        assertEquals(17 * 1.3f, style.lineHeight.value, 0.001f)
        assertEquals(styles.title.lineHeightStyle, style.lineHeightStyle)
        assertEquals(FontFamily.Cursive, style.fontFamily)
        assertEquals(FontWeight.W700, style.fontWeight)
    }
}
