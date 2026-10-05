package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.ConnectionSeed
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SeedOutcome
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.model.SyncRun
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val CONNECTION = RepoConnection("o", "r", "main", "notes/index.json", isPublic = true)
private val SEED = ConnectionSeed(owner = "o", repo = "r", branch = "", token = null)

private class SeedHarness(
    seed: ConnectionSeed?,
    storedToken: String? = null,
    autoApply: Boolean = true,
    connected: ConnectionState = ConnectionState.None,
    seedBlocked: Boolean = false,
) {
    val applied = FakeApplied(storedToken)
    val tokens = FakeTokens(storedToken, if (autoApply) applied.state else null)
    val connections = FakeConnections(tokens, connected, seedBlocked)
    val consume =
        ConsumeSeedUseCase(seed, connections, tokens, SyncPendingConnectionUseCase(connections, tokens, applied))
}

class ConnectionUseCaseTest {
    @Test
    fun `RC-19 시드가 있으면 첫 실행 연결로 쓴다`() =
        runTest {
            val h = SeedHarness(SEED)

            val outcome = h.consume()

            assertTrue(outcome is SeedOutcome.Connected)
            assertEquals(listOf("begin", "syncPending", "confirm"), h.connections.calls)
            assertEquals(
                ConnectionState.Connected(SourceId("c1"), CONNECTION.copy(branch = "")),
                h.connections.connected,
            )
        }

    @Test
    fun `RC-19 시드의 일부만 있으면 시드 없음이다`() =
        runTest {
            assertEquals(SeedOutcome.NotApplicable, SeedHarness(SEED.copy(repo = "")).consume())
            assertEquals(SeedOutcome.NotApplicable, SeedHarness(SEED.copy(owner = " ")).consume())
            assertEquals(SeedOutcome.NotApplicable, SeedHarness(null).consume())
            val h = SeedHarness(SEED.copy(repo = ""))
            h.consume()
            assertTrue(h.connections.calls.isEmpty())
        }

    @Test
    fun `RC-19 끊은 뒤에는 시드를 다시 쓰지 않는다`() =
        runTest {
            val h = SeedHarness(SEED, seedBlocked = true)

            assertEquals(SeedOutcome.NotApplicable, h.consume())
            assertTrue(h.connections.calls.isEmpty())
        }

    @Test
    fun `RC-12 저장된 연결이 있으면 시드를 소비하지 않는다`() =
        runTest {
            val stored = ConnectionState.Connected(SourceId("c9"), CONNECTION.copy(owner = "stored"))
            val h = SeedHarness(SEED, connected = stored)

            assertEquals(SeedOutcome.NotApplicable, h.consume())
            assertEquals(stored, h.connections.connected)
            assertTrue(h.connections.calls.isEmpty())
        }

    @Test
    fun `RC-19 시드 첫 동기화가 실패하면 폐기하고 실패를 알린다`() =
        runTest {
            val h = SeedHarness(SEED)
            h.connections.syncResult = Result.failure(SyncFailure.Offline())

            val outcome = h.consume()

            assertTrue((outcome as SeedOutcome.Failed).cause is SyncFailure.Offline)
            assertEquals(listOf("begin", "syncPending", "discard"), h.connections.calls)
            assertEquals(ConnectionState.None, h.connections.connected)
        }

    @Test
    fun `RC-22 내장 토큰은 시드 연결에만 쓴다`() =
        runTest {
            val seeded = SeedHarness(SEED.copy(token = "BUILD"))
            seeded.consume()
            assertEquals("BUILD", seeded.connections.beginToken)
            assertEquals("BUILD", seeded.tokens.currentToken())
            assertTrue(seeded.connections.connected is ConnectionState.Connected)

            val connected =
                SeedHarness(
                    SEED.copy(token = "BUILD"),
                    connected = ConnectionState.Connected(SourceId("c9"), CONNECTION),
                )
            connected.consume()
            assertTrue(connected.tokens.writes.isEmpty())
        }

    @Test
    fun `RC-22 지운 토큰이 내장 토큰으로 되살아나지 않는다`() =
        runTest {
            val h = SeedHarness(SEED.copy(token = "BUILD"))
            h.consume()
            SaveTokenUseCase(h.tokens)(null)

            assertEquals(SeedOutcome.NotApplicable, h.consume())
            assertNull(h.tokens.currentToken())
        }

    @Test
    fun `RC-22 시드 연결은 토큰이 적용된 뒤에 동기화한다`() =
        runTest {
            val h = SeedHarness(SEED.copy(token = "BUILD"), autoApply = false)

            val outcome = async { h.consume() }
            runCurrent()
            assertEquals(listOf("begin"), h.connections.calls)

            h.applied.state.value = "BUILD"
            runCurrent()

            assertEquals(listOf("begin", "syncPending", "confirm"), h.connections.calls)
            assertTrue(outcome.await() is SeedOutcome.Connected)
        }

    @Test
    fun `RC-22 이미 저장된 토큰을 시드가 덮어쓰지 않는다`() =
        runTest {
            val h = SeedHarness(SEED.copy(token = "BUILD"), storedToken = "STORED")

            h.consume()

            assertNull(h.connections.beginToken)
            assertEquals("STORED", h.tokens.currentToken())
            assertTrue(h.tokens.writes.isEmpty())
        }

    @Test
    fun `RC-22 토큰이 적용되지 않으면 동기화하지 않고 실패한다`() =
        runTest {
            val h = SeedHarness(SEED.copy(token = "BUILD"), autoApply = false)

            val outcome = h.consume()

            assertTrue(outcome is SeedOutcome.Failed)
            assertEquals(listOf("begin", "discard"), h.connections.calls)
        }

    @Test
    fun `SY-5 실패 종류 6종이 서로 다른 타입이다`() {
        val kinds =
            listOf(
                SyncFailure.Offline(),
                SyncFailure.TokenRejected(),
                SyncFailure.AccessDenied(),
                SyncFailure.RateLimited(),
                SyncFailure.Server(),
                SyncFailure.Format(),
            ).map { it::class }

        assertEquals(6, kinds.toSet().size)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRunsTest {
    private class Rig {
        val applied = FakeApplied()
        val tokens = FakeTokens(applied = applied.state)
        val connections = FakeConnections(tokens)
        val notes = GatedNotes()
        val refreshes = MutableStateFlow(0)
        val runs = mutableListOf<SyncRun>()
        val observe = ObserveSyncRunsUseCase(SyncNotesUseCase(notes), ObserveAppliedTokenUseCase(applied), connections)
        val firstSync = SyncPendingConnectionUseCase(connections, tokens, applied)
    }

    @Test
    fun `SY-1 연결이 바뀌면 돌던 동기화를 취소하고 새로 돈다`() =
        runTest {
            val rig = Rig().apply { notes.gate = CompletableDeferred() }
            backgroundScope.launch { rig.observe(rig.refreshes).toList(rig.runs) }
            runCurrent()
            assertEquals(1, rig.notes.syncs)
            assertEquals(SyncRun.Running(announce = false), rig.runs.last())

            val pending = BeginConnectionUseCase(rig.connections)(CONNECTION)
            runCurrent()

            assertEquals(1, rig.notes.cancelled)
            assertEquals(SyncRun.Idle, rig.runs.last())
            assertTrue(rig.runs.none { it is SyncRun.Finished })

            rig.firstSync(pending)
            assertEquals(1, rig.connections.firstSyncs)
            assertEquals(1, rig.notes.syncs)
        }

    @Test
    fun `SY-1 연결과 토큰이 같이 바뀌어도 한 번만 돈다`() =
        runTest {
            val rig = Rig()
            backgroundScope.launch { rig.observe(rig.refreshes).toList(rig.runs) }
            runCurrent()
            assertEquals(1, rig.notes.syncs)

            val pending = BeginConnectionUseCase(rig.connections)(CONNECTION, token = "T2")
            runCurrent()
            rig.firstSync(pending)
            ConfirmConnectionUseCase(rig.connections)(pending)
            runCurrent()

            assertEquals("T2", rig.applied.state.value)
            assertEquals(1, rig.connections.firstSyncs)
            assertEquals(1, rig.notes.syncs)
        }

    @Test
    fun `SY-1 확정 뒤 동기화는 다시 시작되지 않는다`() =
        runTest {
            val rig = Rig()
            backgroundScope.launch { rig.observe(rig.refreshes).toList(rig.runs) }
            runCurrent()
            val pending = BeginConnectionUseCase(rig.connections)(CONNECTION)
            rig.firstSync(pending)
            val before = rig.runs.size

            ConfirmConnectionUseCase(rig.connections)(pending)
            runCurrent()

            assertEquals(1, rig.notes.syncs)
            assertEquals(before, rig.runs.size)
        }

    @Test
    fun `SY-1 새로고침과 토큰 변경은 동기화를 다시 시작한다`() =
        runTest {
            val rig = Rig()
            backgroundScope.launch { rig.observe(rig.refreshes).toList(rig.runs) }
            runCurrent()

            rig.refreshes.value = 1
            runCurrent()
            assertEquals(2, rig.notes.syncs)
            assertEquals(SyncRun.Finished(true, Result.success(SyncOutcome())), rig.runs.last())

            rig.applied.state.value = "NEW"
            runCurrent()
            assertEquals(3, rig.notes.syncs)
        }

    @Test
    fun `SY-1 시도를 폐기하면 토큰 되돌림만 동기화를 부른다`() =
        runTest {
            val rig = Rig()
            backgroundScope.launch { rig.observe(rig.refreshes).toList(rig.runs) }
            runCurrent()
            val pending = BeginConnectionUseCase(rig.connections)(CONNECTION)
            runCurrent()

            DiscardConnectionUseCase(rig.connections)(pending)
            runCurrent()

            assertEquals(1, rig.notes.syncs)
        }
}
