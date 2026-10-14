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
    fun `RC-25 연결 화면의 세 길은 모두 링크 첫째이고 두 단이 없다`() {
        // 세 길(홈 첫 실행 빈 상태 · 홈 안내 · 설정)은 모두 같은 쌓은 목적지를 열고, 구역 순서는 입구 보임 하나로만 갈린다
        assertEquals(listOf(ConnectSection.Link, ConnectSection.Entry), connectSectionsOf(entryShown = true))
        assertEquals(listOf(ConnectSection.Link), connectSectionsOf(entryShown = false))
        listOf(true, false).forEach { shown ->
            val sections = connectSectionsOf(shown)
            assertEquals(ConnectSection.Link, sections.first())
            assertEquals(sections.size, sections.toSet().size)
        }
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
