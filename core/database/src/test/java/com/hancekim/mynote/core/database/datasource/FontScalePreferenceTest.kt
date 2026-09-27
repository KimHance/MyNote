package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.domain.model.FontScale
import org.junit.Assert.assertEquals
import org.junit.Test

class FontScalePreferenceTest {
    @Test
    fun `저장이 없으면 기본값이다`() {
        assertEquals(FontScale.Normal, null.toFontScale())
    }

    @Test
    fun `빈 문자열이 저장돼 있어도 기본값이다`() {
        assertEquals(FontScale.Normal, "".toFontScale())
    }

    @Test
    fun `모르는 값이 저장돼 있어도 던지지 않고 기본값이다`() {
        assertEquals(FontScale.Normal, "huge".toFontScale())
    }

    @Test
    fun `저장한 값을 그대로 다시 읽는다`() {
        FontScale.entries.forEach { scale ->
            assertEquals(scale, scale.toPreference().toFontScale())
        }
    }

    @Test
    fun `저장 형식은 enum 이름과 따로 고정돼 있다`() {
        assertEquals("normal", FontScale.Normal.toPreference())
        assertEquals("large", FontScale.Large.toPreference())
    }
}
