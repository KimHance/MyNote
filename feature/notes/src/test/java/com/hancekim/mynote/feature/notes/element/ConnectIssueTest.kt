package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.domain.connect.ConnectLinkRejection
import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.domain.model.SyncFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectIssueTest {
    @Test
    fun `RC-5 무인증 404 는 토큰 요청 상태가 된다`() {
        assertEquals(ConnectIssue.NeedsToken, issueOfProbe(ConnectProbeFailure.NeedsToken, hadToken = false))
        assertEquals(ConnectIssue.NeedsToken, issueOfProbe(ConnectProbeFailure.RepoNotVisible, hadToken = false))
    }

    @Test
    fun `RC-7 토큰 실패가 서로 다른 화면 상태로 갈린다`() {
        val states =
            listOf(
                issueOfProbe(ConnectProbeFailure.TokenRejected, hadToken = true),
                issueOfProbe(ConnectProbeFailure.RepoNotVisible, hadToken = true),
                issueOfProbe(ConnectProbeFailure.ManifestInaccessible, hadToken = true),
                issueOfProbe(ConnectProbeFailure.RateLimited, hadToken = true),
                issueOfProbe(ConnectProbeFailure.Transient, hadToken = true),
            )

        assertEquals(5, states.toSet().size)
        assertEquals(ConnectIssue.TokenField(TokenFailureKind.Rejected), states[0])
        assertEquals(ConnectIssue.TokenField(TokenFailureKind.NotVisible), states[1])
        assertEquals(ConnectIssue.TokenField(TokenFailureKind.NoList), states[2])
        assertEquals(ConnectIssue.Limit, states[3])
    }

    @Test
    fun `RC-7 한도로 첫 동기화가 실패하면 토큰 권유 상태다`() {
        assertEquals(ConnectIssue.Limit, issueOfSync(SyncFailure.RateLimited(), hadToken = false))
        assertEquals(ConnectIssue.Limit, issueOfSync(SyncFailure.RateLimited(), hadToken = true))
    }

    @Test
    fun `RC-6 첫 동기화 실패 6종이 화면 상태로 갈린다`() {
        assertEquals(ConnectIssue.Notice(ConnectNotice.Offline), issueOfSync(SyncFailure.Offline(), true))
        assertEquals(
            ConnectIssue.TokenField(TokenFailureKind.Rejected),
            issueOfSync(SyncFailure.TokenRejected(), true),
        )
        assertEquals(ConnectIssue.NeedsToken, issueOfSync(SyncFailure.AccessDenied(), hadToken = false))
        assertEquals(
            ConnectIssue.TokenField(TokenFailureKind.NotVisible),
            issueOfSync(SyncFailure.AccessDenied(), hadToken = true),
        )
        assertEquals(ConnectIssue.Notice(ConnectNotice.Transient), issueOfSync(SyncFailure.Server(), true))
        assertEquals(ConnectIssue.Notice(ConnectNotice.BadList), issueOfSync(SyncFailure.Format(), true))
    }

    @Test
    fun `RC-9 RC-10 RC-11 목록 없음 목록 깨짐 오프라인 일시 오류는 서로 다른 안내다`() {
        val notices =
            listOf(
                issueOfProbe(ConnectProbeFailure.NoManifest, false),
                issueOfProbe(ConnectProbeFailure.MalformedManifest, false),
                issueOfProbe(ConnectProbeFailure.Offline, false),
                issueOfProbe(ConnectProbeFailure.Transient, false),
            )

        assertEquals(4, notices.toSet().size)
        assertTrue(notices.all { it is ConnectIssue.Notice })
        assertEquals(
            4,
            ConnectNotice.entries
                .map { connectNoticeRes(it) }
                .toSet()
                .size,
        )
    }

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

    @Test
    fun `RC-3 받지 않는 링크는 이유 7종이 다른 문구다`() {
        assertEquals(
            7,
            ConnectLinkRejection.entries
                .map { linkReasonRes(it) }
                .toSet()
                .size,
        )
        assertEquals(LinkCheck.Rejected(ConnectLinkRejection.Empty), linkCheckOf("   "))
        assertEquals(LinkCheck.Rejected(ConnectLinkRejection.NotHttps), linkCheckOf("http://github.com/a/b"))
        assertEquals(LinkCheck.Rejected(ConnectLinkRejection.NotHttps), linkCheckOf("ftp://github.com/a/b"))
    }

    @Test
    fun `RC-2 알아본 결과는 저장소와 브랜치와 노트 위치 폴더를 보인다`() {
        assertEquals(
            LinkCheck.Found("octo-learner/android-notes", null, "notes"),
            linkCheckOf("octo-learner/android-notes"),
        )
        assertEquals(
            LinkCheck.Found("octo-learner/android-notes", "main", "docs"),
            linkCheckOf("https://github.com/octo-learner/android-notes/tree/main/docs"),
        )
        assertTrue(linkCheckOf("octo-learner/android-notes").canConnect)
        assertEquals(false, linkCheckOf("example.com/foo").canConnect)
    }
}
