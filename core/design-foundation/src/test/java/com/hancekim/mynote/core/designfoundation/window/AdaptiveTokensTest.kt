package com.hancekim.mynote.core.designfoundation.window

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTokensTest {
    @Test
    fun `높이 클래스는 480과 900을 경계로 나뉜다`() {
        assertEquals(MyNoteHeightClass.Compact, MyNoteHeightClass.fromHeight(479.dp))
        assertEquals(MyNoteHeightClass.Medium, MyNoteHeightClass.fromHeight(480.dp))
        assertEquals(MyNoteHeightClass.Medium, MyNoteHeightClass.fromHeight(899.dp))
        assertEquals(MyNoteHeightClass.Expanded, MyNoteHeightClass.fromHeight(900.dp))
    }

    @Test
    fun `좁고 높은 창은 하단 바를 쓴다`() {
        val tokens = adaptiveTokensFor(MyNoteWindowClass.Compact, MyNoteHeightClass.Medium)
        assertFalse(tokens.useFloatingRail)
    }

    @Test
    fun `넓은 창은 하단 바 대신 레일을 쓴다`() {
        listOf(MyNoteWindowClass.Expanded, MyNoteWindowClass.Large, MyNoteWindowClass.ExtraLarge)
            .forEach { widthClass ->
                val tokens = adaptiveTokensFor(widthClass, MyNoteHeightClass.Medium)
                assertTrue("$widthClass 은 레일이어야 한다", tokens.useFloatingRail)
            }
    }

    @Test
    fun `짧은 창은 너비가 좁아도 레일을 쓴다`() {
        val tokens = adaptiveTokensFor(MyNoteWindowClass.Medium, MyNoteHeightClass.Compact)
        assertTrue(tokens.useFloatingRail)
    }

    @Test
    fun `높이 Compact 에서는 히어로 로고를 렌더하지 않는다`() {
        val short = adaptiveTokensFor(MyNoteWindowClass.Compact, MyNoteHeightClass.Compact)
        assertNull(short.heroLogoSize)

        val tall = adaptiveTokensFor(MyNoteWindowClass.Compact, MyNoteHeightClass.Medium)
        assertEquals(46.dp, tall.heroLogoSize)
    }

    @Test
    fun `노트 제목은 히어로 제목보다 4 크다`() {
        MyNoteHeightClass.entries.forEach { heightClass ->
            val tokens = adaptiveTokensFor(MyNoteWindowClass.Compact, heightClass)
            assertEquals(
                (tokens.heroTitleSize.value + 4).sp,
                tokens.noteTitleSize,
            )
        }
    }

    @Test
    fun `레일 오른쪽 여백은 최소 14을 지킨다`() {
        // page 16 → 16-12 = 4 이지만 하한 14 로 올라간다
        val compact = adaptiveTokensFor(MyNoteWindowClass.Compact, MyNoteHeightClass.Medium)
        assertEquals(14.dp, compact.railRightMargin)

        // page 56 → 56-12 = 44
        val extraLarge = adaptiveTokensFor(MyNoteWindowClass.ExtraLarge, MyNoteHeightClass.Medium)
        assertEquals(44.dp, extraLarge.railRightMargin)
    }

    @Test
    fun `모달 드로어는 너비 Compact 에서만 쓴다`() {
        assertTrue(adaptiveTokensFor(MyNoteWindowClass.Compact).useModalDrawer)
        MyNoteWindowClass.entries
            .filter { it != MyNoteWindowClass.Compact }
            .forEach { assertFalse("$it 은 고정 패널이어야 한다", adaptiveTokensFor(it).useModalDrawer) }
    }

    @Test
    fun `본문 최대 폭은 Expanded 부터 생긴다`() {
        assertNull(adaptiveTokensFor(MyNoteWindowClass.Compact).readingMaxWidth)
        assertNull(adaptiveTokensFor(MyNoteWindowClass.Medium).readingMaxWidth)
        assertEquals(720.dp, adaptiveTokensFor(MyNoteWindowClass.Expanded).readingMaxWidth)
        assertEquals(760.dp, adaptiveTokensFor(MyNoteWindowClass.Large).readingMaxWidth)
        assertEquals(820.dp, adaptiveTokensFor(MyNoteWindowClass.ExtraLarge).readingMaxWidth)
    }
}
