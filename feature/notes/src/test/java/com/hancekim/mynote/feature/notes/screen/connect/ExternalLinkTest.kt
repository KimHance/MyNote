package com.hancekim.mynote.feature.notes.screen.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalLinkTest {
    @Test
    fun `RC-8 기본 브라우저가 있으면 그 브라우저로 연다`() {
        assertEquals(
            "org.mozilla.firefox",
            pickBrowser("org.mozilla.firefox", listOf("com.android.chrome", "org.mozilla.firefox")),
        )
    }

    @Test
    fun `RC-8 기본 브라우저가 없으면 Chrome 을 고른다`() {
        assertEquals("com.android.chrome", pickBrowser("android", listOf("org.mozilla.firefox", "com.android.chrome")))
        assertEquals("com.android.chrome", pickBrowser(null, listOf("org.mozilla.firefox", "com.android.chrome")))
    }

    @Test
    fun `RC-8 Chrome 도 없으면 처음 찾은 브라우저를 고른다`() {
        assertEquals("org.mozilla.firefox", pickBrowser("android", listOf("org.mozilla.firefox", "com.opera.browser")))
    }

    @Test
    fun `RC-8 브라우저가 없으면 고르지 않는다`() {
        assertNull(pickBrowser(null, emptyList()))
        assertNull(pickBrowser("android", emptyList()))
    }
}
