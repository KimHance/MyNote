package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.ui.element.TokenFailureKind
import com.hancekim.mynote.core.ui.element.tokenFailureRes
import org.junit.Assert.assertEquals
import org.junit.Test

/** 연결 실패 판정 시험 가운데 설정 토큰 편집기 쪽 — 판정 함수가 설정에 남아 여기 산다(FM-13 같은 클래스 이름). */
class ConnectIssueTest {
    @Test
    fun `RC-18 편집기 실패는 확인 실패 종류를 그대로 보인다`() {
        assertEquals(TokenFailureKind.Rejected, tokenFailureOfProbe(ConnectProbeFailure.NeedsToken))
        assertEquals(TokenFailureKind.NotVisible, tokenFailureOfProbe(ConnectProbeFailure.RepoNotVisible))
        assertEquals(TokenFailureKind.Limit, tokenFailureOfProbe(ConnectProbeFailure.RateLimited))
        assertEquals(TokenFailureKind.Offline, tokenFailureOfProbe(ConnectProbeFailure.Offline))
        assertEquals(
            7,
            TokenFailureKind.entries
                .map { tokenFailureRes(it) }
                .toSet()
                .size,
        )
    }
}
