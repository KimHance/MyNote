package com.hancekim.mynote.shell

import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designfoundation.window.adaptiveTokensFor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesLayoutTest {
    @Test
    fun `1280 데스크톱은 패널이 펴져 있어도 2단이다`() {
        val adaptive = adaptiveTokensFor(MyNoteWindowClass.Large, MyNoteHeightClass.Medium)
        assertTrue(contentTwoPane(1280.dp, adaptive, panelOpen = true))
    }

    @Test
    fun `852 펼친 폴드는 패널이 펴져 있으면 한 단이고 접으면 2단이다`() {
        val adaptive = adaptiveTokensFor(MyNoteWindowClass.Expanded, MyNoteHeightClass.Medium)
        assertFalse(contentTwoPane(852.dp, adaptive, panelOpen = true))
        assertTrue(contentTwoPane(852.dp, adaptive, panelOpen = false))
    }

    @Test
    fun `891x411 가로 폰은 패널이 펴져 있으면 한 단이고 접으면 2단이다`() {
        val adaptive = adaptiveTokensFor(MyNoteWindowClass.Expanded, MyNoteHeightClass.Compact)
        assertFalse(contentTwoPane(891.dp, adaptive, panelOpen = true))
        assertTrue(contentTwoPane(891.dp, adaptive, panelOpen = false))
    }

    @Test
    fun `Compact 세로 폰은 드로어라 패널 폭을 빼지 않지만 폭이 모자라 한 단이다`() {
        val adaptive = adaptiveTokensFor(MyNoteWindowClass.Compact, MyNoteHeightClass.Expanded)
        assertFalse(contentTwoPane(443.dp, adaptive, panelOpen = true))
        assertFalse(contentTwoPane(443.dp, adaptive, panelOpen = false))
    }
}
