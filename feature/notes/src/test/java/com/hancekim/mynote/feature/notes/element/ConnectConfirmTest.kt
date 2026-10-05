package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.domain.connect.ConnectReplacement
import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.RepoConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectConfirmTest {
    @Test
    fun `RC-15 지울 것이 없으면 확인 대화상자가 없다`() {
        assertNull(confirmUiOf(ConnectReplacement.None))
    }

    @Test
    fun `RC-15 바꾸기 대화상자는 그때 센 노트와 북마크 개수를 든다`() {
        val ui = confirmUiOf(ConnectReplacement.Erases(ErasureCount(24, 3), dropsToken = false, adopts = false))

        assertEquals(ConfirmUi(ConfirmKind.Switch, notes = 24, bookmarks = 3, dropsToken = false), ui)
    }

    @Test
    fun `D36 공개로 바꿀 때만 토큰이 지워진다는 줄이 붙는다`() {
        val withToken = confirmUiOf(ConnectReplacement.Erases(ErasureCount(24, 3), dropsToken = true, adopts = false))
        val adopt = confirmUiOf(ConnectReplacement.Erases(ErasureCount(5, 0), dropsToken = true, adopts = true))

        assertEquals(true, withToken?.dropsToken)
        assertEquals(false, adopt?.dropsToken)
    }

    @Test
    fun `RC-19 이어받기 대화상자는 이어받기 종류다`() {
        val ui = confirmUiOf(ConnectReplacement.Erases(ErasureCount(5, 0), dropsToken = false, adopts = true))

        assertEquals(ConfirmKind.Adopt, ui?.kind)
        assertEquals(5, ui?.notes)
    }

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
