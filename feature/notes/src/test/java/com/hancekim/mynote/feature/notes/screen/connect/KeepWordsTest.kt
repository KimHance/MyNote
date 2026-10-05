package com.hancekim.mynote.feature.notes.screen.connect

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepWordsTest {
    private val joiner = '⁠'

    @Test
    fun `D50 한글 음절 사이에만 결합자를 넣어 어절 중간에서 꺾이지 않게 한다`() {
        assertEquals("불⁠러⁠옵⁠니⁠다", "불러옵니다".keepKoreanWords())
        assertEquals("저⁠장⁠소 octo-learner/android-notes", "저장소 octo-learner/android-notes".keepKoreanWords())
    }

    @Test
    fun `D50 한글이 아닌 글자와 공백은 그대로다`() {
        assertEquals("GitHub 2026.06.12 https", "GitHub 2026.06.12 https".keepKoreanWords())
        assertEquals("", "".keepKoreanWords())
    }

    @Test
    fun `D51 결합자를 넣은 글의 접근성 글자에는 결합자가 없고 화면 글자에만 있다`() {
        val raw = AnnotatedString("저장소 주소를 붙여 넣으세요.")

        val kept = keptTextOf(raw)

        assertEquals(raw, kept.plain)
        assertFalse(kept.plain.text.contains(joiner))
        assertTrue(kept.display.text.contains(joiner))
        assertEquals(raw.text, kept.display.text.replace(joiner.toString(), ""))
    }

    @Test
    fun `RC-2 한글 폴더 인자에도 mono 강조가 남고 인자 구간이 정확하다`() {
        val color = SpanStyle(color = Color.Red)
        val raw =
            buildAnnotatedString {
                append("저장소 ")
                withStyle(color) { append("octo/notes") }
                append(" · 노트 위치 ")
                withStyle(color) { append("학습노트") }
            }

        val display = keepKoreanWords(raw)

        assertEquals(2, display.spanStyles.size)
        val folder = display.spanStyles[1]
        assertEquals("학⁠습⁠노⁠트", display.text.substring(folder.start, folder.end))
        val repo = display.spanStyles[0]
        assertEquals("octo/notes", display.text.substring(repo.start, repo.end))
    }
}
