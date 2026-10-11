package com.hancekim.mynote.feature.settings.element

import com.hancekim.mynote.core.domain.model.RepoConnection
import org.junit.Assert.assertEquals
import org.junit.Test

/** 연결 확인 시험 가운데 설정의 토큰 확인 링크 쪽 — 링크 만들기가 설정에 남아 여기 산다(FM-13 같은 클래스 이름). */
class ConnectConfirmTest {
    @Test
    fun `RC-18 설정의 토큰 확인은 현재 연결의 링크로 한다`() {
        val default = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)

        assertEquals("https://github.com/octo-learner/android-notes/tree/main", connectionLinkOf(default))
        assertEquals(
            "https://github.com/octo-learner/android-notes/blob/dev/docs/index.json",
            connectionLinkOf(default.copy(branch = "dev", indexPath = "docs/index.json")),
        )
        assertEquals(
            "https://github.com/octo-learner/android-notes",
            connectionLinkOf(default.copy(branch = "")),
        )
    }
}
