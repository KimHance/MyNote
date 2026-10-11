package com.hancekim.mynote.shell

import androidx.lifecycle.SavedStateHandle
import com.hancekim.mynote.core.domain.ConnectRepositoryUseCase
import com.hancekim.mynote.core.domain.ObserveConnectionUseCase
import com.hancekim.mynote.core.domain.SyncPendingConnectionUseCase
import com.hancekim.mynote.core.domain.VerifyConnectLinkUseCase
import com.hancekim.mynote.core.domain.connect.ConnectCandidate
import com.hancekim.mynote.core.domain.connect.ConnectLinkRepo
import com.hancekim.mynote.core.domain.connect.ConnectProbe
import com.hancekim.mynote.core.domain.connect.ConnectProbeResult
import com.hancekim.mynote.core.domain.connect.TemplateRepo
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.NetworkStatus
import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import com.hancekim.mynote.core.domain.repository.NetworkStatusSource
import com.hancekim.mynote.core.domain.repository.TokenRepository
import com.hancekim.mynote.feature.connect.element.ConfirmKind
import com.hancekim.mynote.feature.connect.element.ConnectStep
import com.hancekim.mynote.feature.connect.screen.ConnectIntent
import com.hancekim.mynote.feature.connect.screen.ConnectViewModel
import com.hancekim.mynote.feature.notes.element.NotesUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** 연결 ViewModel 시험 가운데 홈 히어로 저장소 줄(노트 상태) 쪽 — 연결 VM 과 노트 상태를 같이 봐야 해서 `:app` 에 산다(FM-13 같은 클래스 이름). */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectViewModelTest {
    private val repo = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)
    private val link = "octo-learner/android-notes"

    private class FakeProbe(
        var onVerify: suspend () -> ConnectProbeResult,
    ) : ConnectProbe {
        var calls = 0
        var cancelled = false

        override suspend fun verify(
            repo: ConnectLinkRepo,
            candidates: List<ConnectCandidate>,
            token: String?,
        ): ConnectProbeResult {
            calls++
            try {
                return onVerify()
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                cancelled = true
                throw cancel
            }
        }
    }

    private class FakeConnections(
        val connection: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.None),
    ) : ConnectionRepository {
        val syncGate = CompletableDeferred<Result<SyncOutcome>>()
        var begun = 0
        var discarded = 0

        override fun observeConnection(): Flow<ConnectionState> = connection

        override fun observeAttempting(): Flow<Boolean> = flowOf(false)

        override suspend fun countErasable() = ErasureCount(0, 0)

        override suspend fun countAdoptionErasable(keptDocIds: Set<String>) = ErasureCount(0, 0)

        override suspend fun isSeedEligible() = false

        override suspend fun begin(
            connection: RepoConnection,
            token: String?,
        ): PendingConnection {
            begun++
            return PendingConnection(SourceId("s1"), connection, PendingConnection.Kind.New)
        }

        override suspend fun syncPending(pending: PendingConnection): Result<SyncOutcome> = syncGate.await()

        override suspend fun confirm(pending: PendingConnection) {
            connection.value = ConnectionState.Connected(pending.sourceId, pending.connection)
        }

        override suspend fun discard(pending: PendingConnection) {
            discarded++
        }

        override suspend fun disconnect(): Result<Unit> = Result.success(Unit)

        override suspend fun recover() = Unit
    }

    private object FakeTokens : TokenRepository, AppliedTokenSource, NetworkStatusSource {
        override suspend fun currentToken(): String? = null

        override suspend fun saveToken(token: String?) = Unit

        override fun observeToken(): Flow<String?> = flowOf(null)

        override fun applied(): Flow<String?> = flowOf(null)

        override fun current() = NetworkStatus.WIFI
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        probe: FakeProbe,
        connections: FakeConnections,
        template: TemplateRepo? = null,
    ): ConnectViewModel {
        val verify = VerifyConnectLinkUseCase(probe, FakeTokens, connections, FakeTokens)
        val connect =
            ConnectRepositoryUseCase(connections, SyncPendingConnectionUseCase(connections, FakeTokens, FakeTokens))
        return ConnectViewModel(
            SavedStateHandle(),
            verify,
            connect,
            ObserveConnectionUseCase(connections),
            template,
            failureLog = { _, _, _ -> },
        )
    }

    @Test
    fun `BR-6 히어로 저장소 줄은 확정된 연결에서만 나온다`() =
        runTest {
            val old = RepoConnection("Old-Owner", "Old-Notes", "main", "notes/index.json", isPublic = true)
            val connections = FakeConnections(MutableStateFlow(ConnectionState.Connected(SourceId("s0"), old)))
            val vm = viewModel(FakeProbe { ConnectProbeResult.Verified(repo, emptySet()) }, connections)
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

            fun heroRepo() = NotesUiState(connection = connections.connection.value).heroRepoName

            vm.onIntent(ConnectIntent.LinkChanged(link))
            vm.onIntent(ConnectIntent.Submit(null))
            val dialog = vm.state.value.confirm
            assertEquals(ConfirmKind.Switch, dialog?.kind)
            assertEquals("Old-Owner/Old-Notes", heroRepo())

            vm.onIntent(ConnectIntent.AcceptConfirm)
            assertEquals(ConnectStep.Syncing, vm.state.value.step)
            assertEquals("Old-Owner/Old-Notes", heroRepo())

            connections.syncGate.complete(Result.success(SyncOutcome(added = 3)))

            assertEquals(ConnectStep.Done, vm.state.value.step)
            assertEquals("octo-learner/android-notes", heroRepo())
            collector.cancel()
        }
}
