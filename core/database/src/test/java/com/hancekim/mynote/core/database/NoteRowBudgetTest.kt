package com.hancekim.mynote.core.database

import com.hancekim.mynote.core.domain.model.NoteLimits
import org.junit.Assert.assertTrue
import org.junit.Test

private const val CURSOR_WINDOW_BYTES = 2 * 1_024 * 1_024
private const val OTHER_COLUMNS_BYTES = 64 * 1_024
private const val HEADROOM_BYTES = 256 * 1_024

class NoteRowBudgetTest {
    @Test
    fun `NF-8 한도 안 노트의 행은 원문과 캐시를 다 담아도 커서 창 하나에 여유 있게 들어간다`() {
        val limits = NoteLimits.Default
        val worstRow = limits.maxBytes + limits.maxCacheBytes + OTHER_COLUMNS_BYTES

        assertTrue("$worstRow", worstRow + HEADROOM_BYTES <= CURSOR_WINDOW_BYTES)
    }
}
