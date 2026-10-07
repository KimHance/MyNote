package com.hancekim.mynote.feature.notes.screen.connect

import androidx.lifecycle.SavedStateHandle
import com.hancekim.mynote.core.domain.ConnectRepositoryUseCase
import com.hancekim.mynote.core.domain.ObserveConnectionUseCase
import com.hancekim.mynote.core.domain.SyncPendingConnectionUseCase
import com.hancekim.mynote.core.domain.VerifyConnectLinkUseCase
import com.hancekim.mynote.core.domain.connect.ConnectCandidate
import com.hancekim.mynote.core.domain.connect.ConnectLinkRepo
import com.hancekim.mynote.core.domain.connect.ConnectProbe
import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
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
import com.hancekim.mynote.feature.notes.element.ConfirmKind
import com.hancekim.mynote.feature.notes.element.ConfirmUi
import com.hancekim.mynote.feature.notes.element.ConnectNotice
import com.hancekim.mynote.feature.notes.element.ConnectStep
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.element.NoticeWays
import com.hancekim.mynote.feature.notes.element.SecretText
import com.hancekim.mynote.feature.notes.element.noticeWays
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
        return ConnectViewModel(SavedStateHandle(), verify, connect, ObserveConnectionUseCase(connections), template)
    }

    @Test
    fun `RC-25 템플릿 주소가 없으면 입구가 없다`() =
        runTest {
            val probe = FakeProbe { ConnectProbeResult.Failed(ConnectProbeFailure.NoManifest) }
            val vm = viewModel(probe, FakeConnections(), template = null)
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

            assertNull(vm.state.value.template)
            assertFalse(vm.state.value.entryShown)

            vm.onIntent(ConnectIntent.LinkChanged(link))
            vm.onIntent(ConnectIntent.Submit(null))

            val notice = vm.state.value.notice
            assertEquals(ConnectNotice.NoList, notice)
            assertEquals(NoticeWays.None, noticeWays(notice!!, vm.state.value.template != null))
            collector.cancel()
        }

    @Test
    fun `RC-25 입구가 여는 주소는 생성 주소다`() =
        runTest {
            val template = TemplateRepo("octo-learner", "notes-template")
            val vm = viewModel(FakeProbe { ConnectProbeResult.Verified(repo, emptySet()) }, FakeConnections(), template)
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

            val shown = vm.state.value
            assertTrue(shown.entryShown)
            assertEquals("https://github.com/octo-learner/notes-template/generate", shown.template?.generateUrl)
            assertEquals("https://github.com/octo-learner/notes-template", shown.template?.introUrl)
            collector.cancel()
        }

    @Test
    fun `RC-25 확인 대화상자는 입구 판정을 바꾸지 않는다`() {
        val template = TemplateRepo("octo-learner", "notes-template")
        val plain = ConnectUiState(link = link, template = template)
        val withDialog =
            plain.copy(confirm = ConfirmUi(ConfirmKind.Adopt, notes = 5, bookmarks = 0, dropsToken = false))

        assertTrue(plain.entryShown)
        assertEquals(plain.entryShown, withDialog.entryShown)
    }

    @Test
    fun `RC-9 목록 없음 상자의 길은 한 화면 상태에서도 템플릿 주소가 있을 때만이다`() {
        val template = TemplateRepo("octo-learner", "notes-template")
        val noList = ConnectUiState(link = link, notice = ConnectNotice.NoList, template = template)

        assertFalse(noList.entryShown)
        assertEquals(NoticeWays.CreateAndGuide, noticeWays(ConnectNotice.NoList, noList.template != null))
        assertEquals(NoticeWays.None, noticeWays(ConnectNotice.NoList, noList.copy(template = null).template != null))
    }

    @Test
    fun `RC-24 확인 중 뒤로는 요청을 취소하고 입력을 유지한다`() =
        runTest {
            val gate = CompletableDeferred<ConnectProbeResult>()
            val probe = FakeProbe { gate.await() }
            val vm = viewModel(probe, FakeConnections())
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

            vm.onIntent(ConnectIntent.LinkChanged(link))
            vm.onIntent(ConnectIntent.Submit(null))
            assertEquals(ConnectStep.Probing, vm.state.value.step)

            vm.onIntent(ConnectIntent.CancelProbe)

            assertEquals(ConnectStep.Editing, vm.state.value.step)
            assertEquals(link, vm.state.value.link)
            assertNull(vm.state.value.notice)
            assertTrue(probe.cancelled)
            collector.cancel()
        }

    @Test
    fun `RC-24 첫 동기화 중 입력은 무시된다`() =
        runTest {
            val connections = FakeConnections()
            val probe = FakeProbe { ConnectProbeResult.Verified(repo, emptySet()) }
            val vm = viewModel(probe, connections)
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

            vm.onIntent(ConnectIntent.LinkChanged(link))
            vm.onIntent(ConnectIntent.Submit(null))
            assertEquals(ConnectStep.Syncing, vm.state.value.step)

            vm.onIntent(ConnectIntent.LinkChanged("other/repo"))
            vm.onIntent(ConnectIntent.CancelProbe)
            vm.onIntent(ConnectIntent.Submit(SecretText("github_pat_x")))

            assertEquals(ConnectStep.Syncing, vm.state.value.step)
            assertEquals(link, vm.state.value.link)
            assertEquals(1, probe.calls)
            assertEquals(1, connections.begun)
            assertEquals(0, connections.discarded)
            collector.cancel()
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

    @Test
    fun `RC-5 무인증 404 는 토큰 요청 상태가 되고 링크는 그대로다`() =
        runTest {
            val probe = FakeProbe { ConnectProbeResult.Failed(ConnectProbeFailure.NeedsToken) }
            val vm = viewModel(probe, FakeConnections())
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

            vm.onIntent(ConnectIntent.LinkChanged(link))
            vm.onIntent(ConnectIntent.Submit(null))

            assertEquals(ConnectStep.Editing, vm.state.value.step)
            assertEquals(link, vm.state.value.link)
            assertTrue(vm.state.value.tokenPanel != null)
            collector.cancel()
        }

    @Test
    fun `RC-17 끊은 뒤 연결 화면은 빈 상태로 시작한다`() =
        runTest {
            val connections = FakeConnections(MutableStateFlow(ConnectionState.Connected(SourceId("s1"), repo)))
            val probe = FakeProbe { ConnectProbeResult.Failed(ConnectProbeFailure.NoManifest) }
            val vm = viewModel(probe, connections)
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

            vm.onIntent(ConnectIntent.LinkChanged(link))
            vm.onIntent(ConnectIntent.Submit(null))
            assertEquals(ConnectNotice.NoList, vm.state.value.notice)

            connections.connection.value = ConnectionState.None

            val state = vm.state.value
            assertEquals("", state.link)
            assertNull(state.notice)
            assertNull(state.tokenPanel)
            assertEquals(ConnectStep.Editing, state.step)
            assertFalse(state.linkCheck is com.hancekim.mynote.feature.notes.element.LinkCheck.Found)
            assertTrue(state.resetCount > 0)
            collector.cancel()
        }

    @Test
    fun `RC-17 연결이 처음부터 없으면 입력을 비우지 않는다`() =
        runTest {
            val connections = FakeConnections()
            val vm = viewModel(FakeProbe { ConnectProbeResult.Failed(ConnectProbeFailure.Offline) }, connections)
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

            vm.onIntent(ConnectIntent.LinkChanged(link))
            connections.connection.value = ConnectionState.None

            assertEquals(link, vm.state.value.link)
            collector.cancel()
        }
}
