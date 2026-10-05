package com.hancekim.mynote.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteKeyTest {
    @Test
    fun `NoteKey 는 소스와 docId 를 슬래시로 잇는다`() {
        assertEquals("s0/graph", NoteKey(SourceId.Legacy, "graph").value)
        assertEquals("s0/graph", NoteKey(SourceId.Legacy, "graph").toString())
    }

    @Test
    fun `parse 는 첫 슬래시에서 자르고 docId 의 슬래시는 남긴다`() {
        assertEquals(NoteKey(SourceId("s3"), "android/compose-state"), NoteKey.parse("s3/android/compose-state"))
        assertEquals(NoteKey(SourceId.Legacy, "graph"), NoteKey.parse("s0/graph"))
    }

    @Test
    fun `parse 는 앞이나 뒤가 비면 null 이다`() {
        assertNull(NoteKey.parse("graph"))
        assertNull(NoteKey.parse("/graph"))
        assertNull(NoteKey.parse("s0/"))
        assertNull(NoteKey.parse(""))
    }
}
