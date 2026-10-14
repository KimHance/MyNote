package com.hancekim.mynote

import org.junit.Assert.assertEquals
import org.junit.Test

private const val OWN = "com.hancekim.mynote"

/** 시험용 — 앱 id 로 시작하는 authority 만 이 앱 것이다. */
private val ownsAuthority: (String) -> Boolean = { it == OWN || it.startsWith("$OWN.") }

private fun stream(
    scheme: String?,
    authority: String?,
    path: String = "/flow.md",
) = SharedStream("$scheme://$authority$path", scheme, authority)

class ShareRequestTest {
    @Test
    fun `LN-8 파일이 붙은 공유는 파일 열기 규칙으로 받는다`() {
        // 파일 이름 · 설명을 글로 같이 보내는 앱이 많다 — 파일이 먼저다(plan D34)
        assertEquals(
            ShareRequest.File("content://files/flow.md", "flow"),
            shareRequestOf(
                text = "flow.md",
                subject = "flow",
                stream = stream("content", "files"),
                ownsAuthority = ownsAuthority,
            ),
        )
    }

    @Test
    fun `LN-8 글 공유는 글로 받고 글이 없으면 빈 글이다`() {
        assertEquals(
            ShareRequest.Text("# 제목\n본문", "제목"),
            shareRequestOf(text = "# 제목\n본문", subject = "제목", stream = null, ownsAuthority = ownsAuthority),
        )
        assertEquals(
            ShareRequest.Text("", null),
            shareRequestOf(text = null, subject = null, stream = null, ownsAuthority = ownsAuthority),
        )
    }

    @Test
    fun `LN-8 content 주소가 아닌 파일 공유는 받지 않는 종류다`() {
        // plan D39 — file 주소는 남의 앱이 이 앱 권한으로 기기 파일을 읽게 한다. scheme 은 내용 제공자처럼 글자 그대로 비교한다
        listOf(
            stream("file", "", "/sdcard/Download/flow.md"),
            stream("CONTENT", "files"),
            stream("android.resource", OWN, "/raw/notes"),
            stream(null, null, "flow.md"),
        ).forEach { refused ->
            assertEquals(
                refused.uri,
                ShareRequest.RefusedFile("flow"),
                shareRequestOf(text = "flow.md", subject = "flow", stream = refused, ownsAuthority = ownsAuthority),
            )
        }
    }

    @Test
    fun `LN-8 이 앱 자신의 authority 를 가리키는 파일 공유는 받지 않는다`() {
        // 사용자 번호를 앞에 붙인 꼴(0@…)도 내용 제공자는 같은 제공자로 찾는다 — 떼고 본다
        listOf(
            stream("content", "$OWN.androidx-startup"),
            stream("content", "0@$OWN.androidx-startup"),
            stream("content", OWN),
            stream("content", ""),
        ).forEach { refused ->
            assertEquals(
                refused.uri,
                ShareRequest.RefusedFile(null),
                shareRequestOf(text = null, subject = null, stream = refused, ownsAuthority = ownsAuthority),
            )
        }
        // 이름이 앱 id 로 시작하기만 하는 남의 authority 는 받는다
        assertEquals(
            ShareRequest.File("content://$OWN-other/flow.md", null),
            shareRequestOf(
                text = null,
                subject = null,
                stream = stream("content", "$OWN-other"),
                ownsAuthority = ownsAuthority,
            ),
        )
    }
}
