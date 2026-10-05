package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.domain.model.BookmarkEventKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookmarkEventKindColumnTest {
    @Test
    fun `저장값은 enum 이름이 아니라 고정 문자열이다`() {
        assertEquals("added", BookmarkEventKind.Added.toColumn())
        assertEquals("removed", BookmarkEventKind.Removed.toColumn())
    }

    @Test
    fun `저장값을 되읽으면 같은 종류로 돌아온다`() {
        BookmarkEventKind.entries.forEach { assertEquals(it, it.toColumn().toBookmarkEventKind()) }
    }

    @Test
    fun `모르는 값은 던지지 않고 버린다`() {
        assertNull("Added".toBookmarkEventKind())
        assertNull("".toBookmarkEventKind())
    }
}
