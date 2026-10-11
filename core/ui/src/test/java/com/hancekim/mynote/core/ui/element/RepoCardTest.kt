package com.hancekim.mynote.core.ui.element

import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoCardTest {
    private val public = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)
    private val private = public.copy(isPublic = false)

    @Test
    fun `RC-13 카드 상태 매핑`() {
        assertEquals(
            RepoCardState.None,
            repoCardUi(null, hasToken = false, last = LastSync.None).state,
        )
        assertEquals(
            RepoCardState.Public,
            repoCardUi(public, hasToken = false, last = LastSync.Succeeded).state,
        )
        assertEquals(
            RepoCardState.TokenInUse,
            repoCardUi(private, hasToken = true, last = LastSync.Succeeded).state,
        )
        assertEquals(
            RepoCardState.TokenNeedsCheck,
            repoCardUi(private, hasToken = true, last = LastSync.Failed(SyncFailureKind.TokenRejected)).state,
        )
        assertEquals(
            RepoCardState.NoResult,
            repoCardUi(public, hasToken = false, last = LastSync.None).state,
        )
        assertEquals(
            RepoCardState.TokenNeedsCheck,
            repoCardUi(private, hasToken = true, last = LastSync.Failed(SyncFailureKind.AccessDenied)).state,
        )
    }

    @Test
    fun `RC-13 공개 연결의 토큰 없음은 배지가 없다`() {
        val publicNoToken = repoCardUi(public, hasToken = false, last = LastSync.Succeeded)
        val privateNoToken = repoCardUi(private, hasToken = false, last = LastSync.Succeeded)
        val rejected = repoCardUi(public, hasToken = true, last = LastSync.Failed(SyncFailureKind.TokenRejected))

        assertFalse(publicNoToken.showBadge)
        assertEquals(R.string.ui_repo_token_none_public, publicNoToken.tokenNone)
        assertTrue(privateNoToken.showBadge)
        assertEquals(R.string.ui_repo_token_none_private, privateNoToken.tokenNone)
        assertTrue(rejected.showBadge)
    }

    @Test
    fun `RC-13 공개 연결의 접근 불가는 토큰 확인 필요가 아니다`() {
        val card = repoCardUi(public, hasToken = false, last = LastSync.Failed(SyncFailureKind.AccessDenied))

        assertEquals(RepoCardState.Public, card.state)
        assertFalse(card.showBadge)
    }

    @Test
    fun `RC-13 시작 직후는 결과 없음이고 연결 없음에는 저장소 줄이 없다`() {
        assertEquals(RepoCardState.NoResult, repoCardUi(public, true, LastSync.None).state)
        assertEquals(RepoDot.Muted, repoCardUi(public, true, LastSync.None).dot)
        assertEquals(null, repoCardUi(null, false, LastSync.Succeeded).tokenNone)
    }

    @Test
    fun `SY-5 마지막 결과 종류는 실패 6종으로 갈린다`() {
        val kinds =
            listOf(
                SyncFailure.Offline(),
                SyncFailure.TokenRejected(),
                SyncFailure.AccessDenied(),
                SyncFailure.RateLimited(),
                SyncFailure.Server(),
                SyncFailure.Format(),
            ).map { syncFailureKindOf(it) }

        assertEquals(SyncFailureKind.entries.toList(), kinds)
        assertEquals(6, kinds.map { syncFailureKindRes(it) }.toSet().size)
        assertEquals(SyncFailureKind.Server, syncFailureKindOf(IllegalStateException()))
    }
}
