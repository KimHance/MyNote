package com.hancekim.mynote.feature.connect.element

import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.ui.element.TokenFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectEntryTest {
    @Test
    fun `RC-25 입구는 입력 상태에서만 보인다`() {
        assertTrue(entryVisible(ConnectStep.Editing, tokenPanelOpen = false, notice = null, templateAvailable = true))

        assertFalse(entryVisible(ConnectStep.Probing, false, null, true))
        assertFalse(entryVisible(ConnectStep.Syncing, false, null, true))
        assertFalse(entryVisible(ConnectStep.Done, false, null, true))
        assertFalse(entryVisible(ConnectStep.Editing, tokenPanelOpen = true, notice = null, templateAvailable = true))
        ConnectNotice.entries.forEach { notice ->
            assertFalse("$notice", entryVisible(ConnectStep.Editing, false, notice, true))
        }
    }

    @Test
    fun `RC-25 템플릿 주소가 없으면 입구가 없다`() {
        ConnectStep.entries.forEach { step ->
            listOf(false, true).forEach { panel ->
                (listOf<ConnectNotice?>(null) + ConnectNotice.entries).forEach { notice ->
                    assertFalse(entryVisible(step, panel, notice, templateAvailable = false))
                }
            }
        }
    }

    @Test
    fun `RC-25 입구 위계는 자리와 연결 유무로 갈린다`() {
        assertEquals(EntryEmphasis.Equal, entryEmphasis(ConnectPlace.Root, hasConnection = false))
        assertEquals(EntryEmphasis.LinkFirst, entryEmphasis(ConnectPlace.Stacked, hasConnection = false))
        assertEquals(EntryEmphasis.LinkFirst, entryEmphasis(ConnectPlace.Stacked, hasConnection = true))
        assertEquals(EntryEmphasis.LinkFirst, entryEmphasis(ConnectPlace.Root, hasConnection = true))
    }

    @Test
    fun `RC-25 가로 두 단은 같은 무게일 때만 허용된다`() {
        assertTrue(twoColumnsOf(EntryEmphasis.Equal, shortHeight = true, contentWidthDp = 800f, entryShown = true))
        assertFalse(twoColumnsOf(EntryEmphasis.LinkFirst, shortHeight = true, contentWidthDp = 800f, entryShown = true))
    }

    @Test
    fun `RC-25 가로 두 단은 높이 Compact 이면서 콘텐츠 폭 640 이상일 때만이다`() {
        assertTrue(twoColumnsOf(EntryEmphasis.Equal, true, TWO_COLUMN_MIN_WIDTH_DP, true))
        assertFalse(twoColumnsOf(EntryEmphasis.Equal, true, TWO_COLUMN_MIN_WIDTH_DP - 1f, true))
        assertFalse(twoColumnsOf(EntryEmphasis.Equal, shortHeight = false, contentWidthDp = 852f, entryShown = true))
        assertFalse(twoColumnsOf(EntryEmphasis.Equal, true, 800f, entryShown = false))
    }

    @Test
    fun `RC-9 목록 없음 상자의 길은 템플릿 주소가 있을 때만이다`() {
        assertEquals(NoticeWays.CreateAndGuide, noticeWays(ConnectNotice.NoList, templateAvailable = true))
        assertEquals(NoticeWays.None, noticeWays(ConnectNotice.NoList, templateAvailable = false))
    }

    @Test
    fun `RC-9 목록 없음 밖의 상자에는 길이 없다`() {
        listOf(ConnectNotice.BadList, ConnectNotice.Offline, ConnectNotice.Transient).forEach {
            assertEquals("$it", NoticeWays.None, noticeWays(it, templateAvailable = true))
        }
    }

    @Test
    fun `RC-9 토큰 사용 중 목록 접근 불가에는 길이 없다`() {
        val issue = issueOfProbe(ConnectProbeFailure.ManifestInaccessible, hadToken = true)

        assertEquals(ConnectIssue.TokenField(TokenFailureKind.NoList), issue)
        assertFalse(issue is ConnectIssue.Notice)
        val ways = (issue as? ConnectIssue.Notice)?.let { noticeWays(it.kind, templateAvailable = true) }
        assertEquals(null, ways)
        assertFalse(entryVisible(ConnectStep.Editing, tokenPanelOpen = true, notice = null, templateAvailable = true))
    }
}
