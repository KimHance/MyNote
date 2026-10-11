package com.hancekim.mynote.shell

import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.ui.element.TokenFailureKind
import com.hancekim.mynote.feature.connect.element.ConnectIssue
import com.hancekim.mynote.feature.connect.element.ConnectNotice
import com.hancekim.mynote.feature.connect.element.issueOfProbe
import com.hancekim.mynote.feature.notes.element.tokenFailureOfProbe
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 설정 편집기의 토큰 실패 판정과 연결 화면의 실패 판정이 같은 갈래인지 — 두 판정이 서로 다른 기능 모듈에 있어
 * 둘을 다 보는 `:app` 에 산다(FM-13 같은 클래스 이름).
 */
class ConnectIssueTest {
    @Test
    fun `RC-18 설정 편집기와 연결 화면의 토큰 실패 갈래가 같다`() {
        // 편집기에는 토큰이 늘 있다 — 연결 화면에서 토큰이 있을 때의 갈래를 토큰 실패 종류로 모은 값과 같아야 한다
        fun viaConnect(failure: ConnectProbeFailure): TokenFailureKind =
            when (val issue = issueOfProbe(failure, hadToken = true)) {
                is ConnectIssue.TokenField -> {
                    issue.kind
                }

                ConnectIssue.Limit -> {
                    TokenFailureKind.Limit
                }

                ConnectIssue.NeedsToken -> {
                    TokenFailureKind.Rejected
                }

                is ConnectIssue.Notice -> {
                    when (issue.kind) {
                        ConnectNotice.NoList -> TokenFailureKind.NoList
                        ConnectNotice.BadList -> TokenFailureKind.BadList
                        ConnectNotice.Offline -> TokenFailureKind.Offline
                        ConnectNotice.Transient -> TokenFailureKind.Transient
                    }
                }
            }

        assertEquals(
            ConnectProbeFailure.entries.associateWith { viaConnect(it) },
            ConnectProbeFailure.entries.associateWith { tokenFailureOfProbe(it) },
        )
    }
}
