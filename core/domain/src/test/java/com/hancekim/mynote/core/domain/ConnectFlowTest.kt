package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.connect.ConnectBranch
import com.hancekim.mynote.core.domain.connect.ConnectCandidate
import com.hancekim.mynote.core.domain.connect.ConnectLinkRejection
import com.hancekim.mynote.core.domain.connect.ConnectLinkRepo
import com.hancekim.mynote.core.domain.connect.ConnectProbe
import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.domain.connect.ConnectProbeResult
import com.hancekim.mynote.core.domain.connect.ConnectProgress
import com.hancekim.mynote.core.domain.connect.ConnectReplacement
import com.hancekim.mynote.core.domain.connect.ConnectVerification
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.NetworkStatus
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.NetworkStatusSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val NEW_REPO = RepoConnection("foo", "bar", "main", "notes/index.json", isPublic = true)
private val OLD_REPO = RepoConnection("old", "repo", "main", "notes/index.json", isPublic = false)

private class FakeProbe : ConnectProbe {
    val calls = mutableListOf<Triple<ConnectLinkRepo, List<ConnectCandidate>, String?>>()
    var result: ConnectProbeResult = ConnectProbeResult.Verified(NEW_REPO)

    override suspend fun verify(
        repo: ConnectLinkRepo,
        candidates: List<ConnectCandidate>,
        token: String?,
    ): ConnectProbeResult {
        calls += Triple(repo, candidates, token)
        return result
    }
}

private class FakeNetwork(
    var status: NetworkStatus = NetworkStatus.WIFI,
) : NetworkStatusSource {
    override fun current(): NetworkStatus = status
}

private class FlowHarness(
    stored: String? = null,
    connected: ConnectionState = ConnectionState.None,
    status: NetworkStatus = NetworkStatus.WIFI,
) {
    val applied = FakeApplied(stored)
    val tokens = FakeTokens(stored, applied.state)
    val connections = FakeConnections(tokens, connected)
    val probe = FakeProbe()
    val network = FakeNetwork(status)
    val verify = VerifyConnectLinkUseCase(probe, network, connections, tokens)
    val connect = ConnectRepositoryUseCase(connections, SyncPendingConnectionUseCase(connections, tokens, applied))
}

private val CONNECTED_OLD = ConnectionState.Connected(SourceId("c0"), OLD_REPO)

class ConnectFlowTest {
    @Test
    fun `RC-3 받지 않는 링크는 요청 없이 이유 7종이 다르다`() =
        runTest {
            val h = FlowHarness()
            val inputs =
                listOf(
                    "   " to ConnectLinkRejection.Empty,
                    "http://github.com/o/r" to ConnectLinkRejection.NotHttps,
                    "https://example.com/foo" to ConnectLinkRejection.NotGithub,
                    "https://gist.github.com/o/abc" to ConnectLinkRejection.Gist,
                    "https://o.github.io/r" to ConnectLinkRejection.GithubPages,
                    "https://github.com/o/r/blob/main/README.md" to ConnectLinkRejection.SingleFile,
                    "https://github.com/o" to ConnectLinkRejection.MissingRepo,
                )

            val reasons = inputs.map { (link, _) -> (h.verify(link, null) as ConnectVerification.Rejected).reason }

            assertEquals(inputs.map { it.second }, reasons)
            assertEquals(ConnectLinkRejection.entries.toSet(), reasons.toSet())
            assertTrue(h.probe.calls.isEmpty())
            assertTrue(h.connections.calls.isEmpty())
            assertEquals(0, h.connections.countCalls)
        }

    @Test
    fun `RC-4 공개 저장소는 토큰 없이 연결된다`() =
        runTest {
            val h = FlowHarness()

            val ready = h.verify("foo/bar", null) as ConnectVerification.Ready
            val progress = h.connect(ready).toList()

            assertNull(
                h.probe.calls
                    .single()
                    .third,
            )
            assertEquals(listOf(ConnectProgress.Syncing, ConnectProgress.Connected(SyncOutcome(added = 1))), progress)
            assertEquals(listOf("begin", "syncPending", "confirm"), h.connections.calls)
            assertNull(h.connections.beginToken)
            assertTrue(h.tokens.writes.isEmpty())
        }

    @Test
    fun `RC-5 무인증 404 는 토큰 요청 상태가 된다`() =
        runTest {
            val h = FlowHarness(stored = "stored")
            h.probe.result = ConnectProbeResult.Failed(ConnectProbeFailure.NeedsToken)

            val result = h.verify("foo/bar", null)

            assertEquals(ConnectVerification.Failed(ConnectProbeFailure.NeedsToken), result)
            assertTrue(h.connections.calls.isEmpty())
            assertTrue(h.tokens.writes.isEmpty())
        }

    @Test
    fun `RC-6 후보 토큰으로 확인한다`() =
        runTest {
            val h = FlowHarness()

            val ready = h.verify("foo/bar", "  cand  ") as ConnectVerification.Ready
            h.connect(ready).toList()

            assertEquals(
                "cand",
                h.probe.calls
                    .single()
                    .third,
            )
            assertEquals("cand", ready.candidateToken)
            assertEquals("cand", h.connections.beginToken)
            assertEquals(listOf<String?>("cand"), h.tokens.writes)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RC-6 토큰이 적용된 뒤에 첫 동기화가 돈다`() =
        runTest {
            val applied = FakeApplied(null)
            val tokens = FakeTokens(null, null)
            val connections = FakeConnections(tokens)
            val connect =
                ConnectRepositoryUseCase(connections, SyncPendingConnectionUseCase(connections, tokens, applied))
            val ready = FlowHarness().verify("foo/bar", "cand") as ConnectVerification.Ready

            val job = launch { connect(ready).toList() }
            runCurrent()
            assertEquals(listOf("begin"), connections.calls)

            applied.state.value = "cand"
            runCurrent()

            assertEquals(listOf("begin", "syncPending", "confirm"), connections.calls)
            assertTrue(job.isCompleted)
        }

    @Test
    fun `RC-6 토큰이 적용되지 않으면 SyncFailure 로 알리고 폐기한다`() =
        runTest {
            val h = FlowHarness()
            val noApply = FakeApplied(null)
            val tokens = FakeTokens(null, null)
            val connections = FakeConnections(tokens)
            val connect =
                ConnectRepositoryUseCase(connections, SyncPendingConnectionUseCase(connections, tokens, noApply))
            val ready = h.verify("foo/bar", "cand") as ConnectVerification.Ready

            val progress = connect(ready).toList()

            val failed = progress.last() as ConnectProgress.Failed
            assertTrue(failed.failure is SyncFailure.Server)
            assertTrue(failed.failure.cause is IllegalStateException)
            assertEquals(listOf("begin", "discard"), connections.calls)
        }

    @Test
    fun `RC-6 실패하면 토큰이 시도 전 값이다`() =
        runTest {
            val h = FlowHarness()
            h.probe.result = ConnectProbeResult.Failed(ConnectProbeFailure.TokenRejected)

            assertEquals(ConnectVerification.Failed(ConnectProbeFailure.TokenRejected), h.verify("foo/bar", "bad"))
            assertTrue(h.tokens.writes.isEmpty())
            assertTrue(h.connections.calls.isEmpty())

            h.probe.result = ConnectProbeResult.Verified(NEW_REPO)
            h.connections.syncResult = Result.failure(SyncFailure.RateLimited())
            val ready = h.verify("foo/bar", "cand") as ConnectVerification.Ready
            val progress = h.connect(ready).toList()

            assertTrue((progress.last() as ConnectProgress.Failed).failure is SyncFailure.RateLimited)
            assertEquals(listOf("begin", "syncPending", "discard"), h.connections.calls)
            assertTrue(h.connections.connected is ConnectionState.None)
        }

    @Test
    fun `RC-7 한도로 첫 동기화가 실패하면 토큰 권유 상태다`() =
        runTest {
            val h = FlowHarness()
            h.connections.syncResult = Result.failure(SyncFailure.RateLimited())
            val ready = h.verify("foo/bar", null) as ConnectVerification.Ready

            val progress = h.connect(ready).toList()

            assertTrue((progress.last() as ConnectProgress.Failed).failure is SyncFailure.RateLimited)
            assertEquals(ConnectionState.None, h.connections.connected)
            assertEquals(listOf("begin", "syncPending", "discard"), h.connections.calls)
        }

    @Test
    fun `RC-7 SyncFailure 가 아닌 첫 동기화 예외는 일시 오류로 감싼다`() =
        runTest {
            val h = FlowHarness()
            h.connections.syncResult = Result.failure(IllegalArgumentException("로컬 실패"))
            val ready = h.verify("foo/bar", null) as ConnectVerification.Ready

            val failed = h.connect(ready).toList().last() as ConnectProgress.Failed

            assertTrue(failed.failure is SyncFailure.Server)
            assertTrue(failed.failure.cause is IllegalArgumentException)
        }

    @Test
    fun `RC-11 오프라인과 일시 오류는 연결을 바꾸지 않는다`() =
        runTest {
            val offline = FlowHarness(stored = "stored", connected = CONNECTED_OLD, status = NetworkStatus.OFFLINE)

            assertEquals(ConnectVerification.Failed(ConnectProbeFailure.Offline), offline.verify("foo/bar", null))
            assertTrue(offline.probe.calls.isEmpty())
            assertTrue(offline.connections.calls.isEmpty())
            assertTrue(offline.tokens.writes.isEmpty())
            assertEquals(CONNECTED_OLD, offline.connections.connected)

            val flaky = FlowHarness(stored = "stored", connected = CONNECTED_OLD)
            flaky.probe.result = ConnectProbeResult.Failed(ConnectProbeFailure.Transient)

            assertEquals(ConnectVerification.Failed(ConnectProbeFailure.Transient), flaky.verify("foo/bar", null))
            assertTrue(flaky.connections.calls.isEmpty())
            assertTrue(flaky.tokens.writes.isEmpty())
            assertEquals(0, flaky.connections.countCalls)
            assertEquals(CONNECTED_OLD, flaky.connections.connected)
        }

    @Test
    fun `RC-15 틀린 링크에는 삭제 확인이 안 뜬다`() =
        runTest {
            val h = FlowHarness(stored = "stored", connected = CONNECTED_OLD)
            h.connections.erasable = ErasureCount(22, 3)

            val failures =
                listOf(
                    ConnectProbeFailure.NeedsToken,
                    ConnectProbeFailure.RepoNotVisible,
                    ConnectProbeFailure.NoManifest,
                    ConnectProbeFailure.MalformedManifest,
                    ConnectProbeFailure.RateLimited,
                )
            failures.forEach { failure ->
                h.probe.result = ConnectProbeResult.Failed(failure)
                assertTrue(h.verify("foo/bar", null) is ConnectVerification.Failed)
            }
            assertTrue(h.verify("not a link", null) is ConnectVerification.Rejected)

            assertEquals(0, h.connections.countCalls)
        }

    @Test
    fun `RC-15 확인에 성공한 바꾸기는 그때 센 개수를 돌려준다`() =
        runTest {
            val h = FlowHarness(connected = CONNECTED_OLD)
            h.connections.erasable = ErasureCount(22, 3)

            val ready = h.verify("foo/bar", null) as ConnectVerification.Ready

            assertEquals(
                ConnectReplacement.Erases(ErasureCount(22, 3), dropsToken = false, adopts = false),
                ready.replacement,
            )
            assertEquals(1, h.connections.countCalls)
        }

    @Test
    fun `RC-15 같은 저장소와 첫 연결은 지울 것이 없어 확인이 없다`() =
        runTest {
            val same = FlowHarness(connected = ConnectionState.Connected(SourceId("c0"), NEW_REPO.copy(owner = "FOO")))
            same.connections.erasable = ErasureCount(22, 3)
            assertEquals(
                ConnectReplacement.None,
                (same.verify("foo/bar", null) as ConnectVerification.Ready).replacement,
            )
            assertEquals(0, same.connections.countCalls)

            val first = FlowHarness()
            assertEquals(
                ConnectReplacement.None,
                (first.verify("foo/bar", null) as ConnectVerification.Ready).replacement,
            )

            val adopt = FlowHarness()
            adopt.connections.erasable = ErasureCount(22, 3)
            assertEquals(
                ConnectReplacement.Erases(ErasureCount(22, 3), dropsToken = false, adopts = true),
                (adopt.verify("foo/bar", null) as ConnectVerification.Ready).replacement,
            )
        }

    private suspend fun dropsToken(
        stored: String?,
        candidate: String?,
        public: Boolean,
    ): Boolean {
        val h = FlowHarness(stored = stored, connected = CONNECTED_OLD)
        h.probe.result = ConnectProbeResult.Verified(NEW_REPO.copy(isPublic = public))
        h.connections.erasable = ErasureCount(1, 0)
        val ready = h.verify("foo/bar", candidate) as ConnectVerification.Ready
        return (ready.replacement as ConnectReplacement.Erases).dropsToken
    }

    @Test
    fun `D36 공개로 바꿀 때만 토큰이 지워진다고 알린다`() =
        runTest {
            assertTrue(dropsToken(stored = "old", candidate = null, public = true))
            assertEquals(false, dropsToken(stored = null, candidate = null, public = true))
            assertEquals(false, dropsToken(stored = "old", candidate = "limit", public = true))
            assertEquals(false, dropsToken(stored = "old", candidate = null, public = false))
        }

    @Test
    fun `RC-15 새 저장소 동기화가 실패하면 옛 연결이 남는다`() =
        runTest {
            val h = FlowHarness(connected = CONNECTED_OLD)
            h.connections.syncResult = Result.failure(SyncFailure.AccessDenied())
            val ready = h.verify("foo/bar", null) as ConnectVerification.Ready

            val progress = h.connect(ready).toList()

            assertTrue((progress.last() as ConnectProgress.Failed).failure is SyncFailure.AccessDenied)
            assertEquals(CONNECTED_OLD, h.connections.connected)
            assertEquals(listOf("begin", "syncPending", "discard"), h.connections.calls)
        }

    @Test
    fun `RC-15 확정이 예외로 끝나면 폐기를 부르지 않는다`() =
        runTest {
            val h = FlowHarness()
            h.connections.confirmFailure = IllegalStateException("저장 실패")
            val ready = h.verify("foo/bar", null) as ConnectVerification.Ready

            val terminal = h.connect(ready).toList().last()

            assertTrue(terminal is ConnectProgress.ConfirmFailed)
            assertEquals(listOf("begin", "syncPending", "confirm"), h.connections.calls)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RC-24 첫 동기화가 도는 중 끊기면 폐기하고 확정하지 않는다`() =
        runTest {
            val h = FlowHarness()
            h.connections.firstSyncGate = CompletableDeferred()
            val ready = h.verify("foo/bar", null) as ConnectVerification.Ready
            val seen = mutableListOf<ConnectProgress>()

            val job = launch { h.connect(ready).collect { seen += it } }
            runCurrent()
            assertEquals(listOf<ConnectProgress>(ConnectProgress.Syncing), seen)
            job.cancel()
            runCurrent()

            assertEquals(listOf("begin", "syncPending", "discard"), h.connections.calls)
        }

    @Test
    fun `RC-24 확인 중 취소는 아무것도 바꾸지 않는다`() =
        runTest {
            val gate = CompletableDeferred<ConnectProbeResult>()
            val h = FlowHarness(stored = "stored", connected = CONNECTED_OLD)
            val slow =
                object : ConnectProbe {
                    override suspend fun verify(
                        repo: ConnectLinkRepo,
                        candidates: List<ConnectCandidate>,
                        token: String?,
                    ): ConnectProbeResult = gate.await()
                }
            val verify = VerifyConnectLinkUseCase(slow, h.network, h.connections, h.tokens)

            val job = launch { verify("foo/bar", null) }
            runCurrent()
            job.cancel()
            runCurrent()

            assertTrue(job.isCancelled)
            assertTrue(h.connections.calls.isEmpty())
            assertTrue(h.tokens.writes.isEmpty())
            assertEquals(0, h.connections.countCalls)
        }
}
