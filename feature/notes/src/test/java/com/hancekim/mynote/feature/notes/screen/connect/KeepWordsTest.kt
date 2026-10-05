package com.hancekim.mynote.feature.notes.screen.connect

import org.junit.Assert.assertEquals
import org.junit.Test

class KeepWordsTest {
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
}
