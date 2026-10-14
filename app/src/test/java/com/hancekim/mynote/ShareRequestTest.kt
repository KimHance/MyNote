package com.hancekim.mynote

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareRequestTest {
    @Test
    fun `LN-8 파일이 붙은 공유는 파일 열기 규칙으로 받는다`() {
        // 파일 이름 · 설명을 글로 같이 보내는 앱이 많다 — 파일이 먼저다(plan D34)
        assertEquals(
            ShareRequest.File("content://files/flow.md", "flow"),
            shareRequestOf(text = "flow.md", subject = "flow", stream = "content://files/flow.md"),
        )
    }

    @Test
    fun `LN-8 글 공유는 글로 받고 글이 없으면 빈 글이다`() {
        assertEquals(
            ShareRequest.Text("# 제목\n본문", "제목"),
            shareRequestOf(text = "# 제목\n본문", subject = "제목", stream = null),
        )
        assertEquals(ShareRequest.Text("", null), shareRequestOf(text = null, subject = null, stream = null))
    }
}
