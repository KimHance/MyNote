package com.hancekim.mynote.core.designfoundation.token

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResizedTextStyleTest {
    private val styles = myNoteTextStyles(16f)

    @Test
    fun `BR-8 크기를 덮으면 sp 줄 높이도 같은 배율로 따라간다`() {
        val noteTitle = styles.noteTitle.resized(32.sp)
        assertTrue(noteTitle.lineHeight.isSp)
        assertEquals(40f, noteTitle.lineHeight.value, 0.001f)
        assertEquals(styles.noteTitle.lineHeightStyle, noteTitle.lineHeightStyle)

        assertEquals(
            17 * 1.3f,
            styles.title
                .resized(17.sp)
                .lineHeight.value,
            0.001f,
        )
    }

    @Test
    fun `BR-8 줄 높이가 sp 가 아니면 크기만 바꾼다`() {
        assertEquals(TextUnit.Unspecified, styles.caption.resized(20.sp).lineHeight)
        assertEquals(1.4.em, TextStyle(fontSize = 16.sp, lineHeight = 1.4.em).resized(20.sp).lineHeight)
    }
}
