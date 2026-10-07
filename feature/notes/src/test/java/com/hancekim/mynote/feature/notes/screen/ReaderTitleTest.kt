package com.hancekim.mynote.feature.notes.screen

import com.hancekim.mynote.core.designfoundation.token.myNoteTextStyles
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designfoundation.window.adaptiveTokensFor
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTitleTest {
    @Test
    fun `BR-8 리더 머리 노트 제목은 창 크기로 커져도 줄 높이가 같은 배율로 따라간다`() {
        val styles = myNoteTextStyles(16f)
        for (height in MyNoteHeightClass.entries) {
            val adaptive = adaptiveTokensFor(MyNoteWindowClass.Compact, height)
            val style = styles.readerTitle(adaptive)

            assertEquals("$height", adaptive.noteTitleSize, style.fontSize)
            assertEquals("$height", adaptive.noteTitleSize.value * 1.25f, style.lineHeight.value, 0.001f)
        }
        val expanded = styles.readerTitle(adaptiveTokensFor(MyNoteWindowClass.Compact, MyNoteHeightClass.Expanded))
        assertEquals("높이 Expanded 32sp 의 줄 높이", 40f, expanded.lineHeight.value, 0.001f)
    }
}
