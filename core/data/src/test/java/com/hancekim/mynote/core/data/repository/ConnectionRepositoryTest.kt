package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.AttemptRecord
import com.hancekim.mynote.core.data.datasource.ConnectionIdGenerator
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.domain.ObserveAppliedTokenUseCase
import com.hancekim.mynote.core.domain.ObserveSyncRunsUseCase
import com.hancekim.mynote.core.domain.SyncNotesUseCase
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.format.NoteFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val LINK_SOURCE = SourceId("s1")

private fun conn(
    owner: String = "o",
    repo: String = "r",
    branch: String = "main",
    isPublic: Boolean = true,
) = RepoConnection(owner, repo, branch, "notes/index.json", isPublic)

private class Harness(
    val local: FakeLocal = FakeLocal(),
    val tokens: FakeTokenStore = FakeTokenStore(),
    val bookmarks: FakeBookmarkStore = FakeBookmarkStore(),
    val state: FakeStateStore = FakeStateStore(),
    val adapter: FakeAdapter = FakeAdapter(emptyList()),
    ids: List<String> = listOf("c1", "c2", "c3", "c4"),
    autoApply: Boolean = true,
) {
    val applied = FakeAppliedToken(tokens.initial)

    init {
        if (autoApply) tokens.mirror = applied.state
    }

    private val queue = ArrayDeque(ids)
    private val generator = ConnectionIdGenerator { queue.removeFirst() }

    private fun recovery() = ConnectionRecovery(local, tokens, bookmarks, state)

    fun connections() =
        ConnectionRepositoryImpl(
            setOf(adapter),
            local,
            tokens,
            bookmarks,
            state,
            generator,
            NoteFormat(),
            recovery(),
            applied,
        ) { 1_000L }

    fun notes() = recovery().let { NotesRepositoryImpl(setOf(adapter), local, NoteFormat(), it::ensure) { 1_000L } }

    suspend fun connection(): ConnectionState = connections().observeConnection().first()
}

private suspend fun ConnectionRepositoryImpl.connect(
    connection: RepoConnection,
    token: String? = null,
): Pair<PendingConnection, SyncOutcome> {
    val pending = begin(connection, token)
    val outcome = syncPending(pending).getOrThrow()
    confirm(pending)
    return pending to outcome
}

private fun Harness.keys(): Set<String> =
    local.store.value.keys

class ConnectionRepositoryTest {
    @Test
    fun `RC-12 저장된 연결이 빌드 시드보다 우선한다`() =
        runTest {
            val stored = conn(owner = "stored", repo = "notes", branch = "dev")
            val h =
                Harness(
                    local =
                        FakeLocal(
                            connected = false,
                            sources =
                                listOf(
                                    SourceRecord(
                                        SourceId("c9"),
                                        SourceKind.GitHub,
                                        ConnectionConfigCodec.encode(stored),
                                    ),
                                ),
                        ),
                    adapter = FakeAdapter(listOf(entry("a", "h1"))),
                )

            h.notes().sync().getOrThrow()

            assertEquals(
                stored,
                ConnectionConfigCodec
                    .decode(
                        h.adapter.listed
                            .single()
                            .configJson,
                    )?.connection,
            )
            assertFalse(h.connections().isSeedEligible())
        }

    @Test
    fun `RC-12 정식 이름으로 저장한다`() =
        runTest {
            val h = Harness(local = FakeLocal(connected = false), adapter = FakeAdapter(listOf(entry("a", "h1"))))

            h.connections().connect(conn(owner = "Foo", repo = "Bar"))

            val connected = h.connection() as ConnectionState.Connected
            assertEquals("Foo/Bar", connected.connection.fullName)
            assertEquals(
                "Foo",
                ConnectionConfigCodec
                    .decode(
                        h.local.sourceRows
                            .getValue(connected.sourceId)
                            .configJson,
                    )?.connection
                    ?.owner,
            )
        }

    @Test
    fun `RC-12 연결은 재시작 뒤에도 같은 저장소로 요청한다`() =
        runTest {
            val h = Harness(local = FakeLocal(connected = false), adapter = FakeAdapter(listOf(entry("a", "h1"))))
            h.connections().connect(conn(owner = "x", repo = "y", branch = "dev"))
            h.adapter.listed.clear()

            h.notes().sync().getOrThrow()

            val used =
                ConnectionConfigCodec
                    .decode(
                        h.adapter.listed
                            .single()
                            .configJson,
                    )?.connection
            assertEquals("x/y", used?.fullName)
            assertEquals("dev", used?.branch)
        }

    @Test
    fun `RC-14 같은 저장소의 브랜치 변경은 노트를 지우지 않는다`() =
        runTest {
            val h =
                Harness(
                    local =
                        FakeLocal(
                            initial = listOf(cached("a", "h1"), cached("b", "h1")),
                            connected = false,
                            sources =
                                listOf(
                                    SourceRecord(
                                        S0,
                                        SourceKind.GitHub,
                                        ConnectionConfigCodec.encode(conn(owner = "Foo", repo = "Bar")),
                                    ),
                                ),
                        ),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L, "s0/b" to 2L)),
                    adapter = FakeAdapter(listOf(entry("a", "h1"))),
                )
            val repo = h.connections()

            val pending = repo.begin(conn(owner = "foo", repo = "bar", branch = "dev"), null)

            assertEquals(PendingConnection.Kind.SameRepository, pending.kind)
            assertEquals(S0, pending.sourceId)
            assertEquals(setOf("s0/a", "s0/b"), h.keys())
            assertEquals(setOf("s0/a", "s0/b"), h.bookmarks.entries.value.keys)

            repo.syncPending(pending).getOrThrow()
            repo.confirm(pending)

            assertEquals(setOf("s0/a"), h.keys())
            assertEquals(setOf("s0/a"), h.bookmarks.entries.value.keys)
            assertEquals(0, h.local.promoteCalls)
            val connected = h.connection() as ConnectionState.Connected
            assertEquals("dev", connected.connection.branch)
            assertEquals(S0, connected.sourceId)
        }

    @Test
    fun `RC-15 새 저장소 동기화가 실패하면 옛 연결이 남는다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    tokens = FakeTokenStore("T0"),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L)),
                    adapter = FakeAdapter(listOf(entry("x", "h1")), listError = IllegalStateException("net")),
                )
            val repo = h.connections()

            val pending = repo.begin(conn(owner = "other", repo = "repo"), "T1")
            assertEquals("T1", h.tokens.currentToken())
            assertTrue(repo.syncPending(pending).isFailure)
            repo.discard(pending)

            assertEquals(S0, (h.connection() as ConnectionState.Connected).sourceId)
            assertEquals(setOf("s0/a"), h.keys())
            assertEquals(setOf("s0/a"), h.bookmarks.entries.value.keys)
            assertEquals("T0", h.tokens.currentToken())
            assertEquals(setOf(S0), h.local.sourceRows.keys)
            assertNull(h.state.attemptRecord)
        }

    @Test
    fun `RC-15 성공하면 옛 소스의 노트와 북마크만 지운다`() =
        runTest {
            val h =
                Harness(
                    local =
                        FakeLocal(
                            initial = listOf(cached("a", "h1"), cached("clip", "r1", source = LINK_SOURCE)),
                            sources = listOf(SourceRecord(LINK_SOURCE, SourceKind.Link, null)),
                        ),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L, "s0/orphan" to 2L, "s1/clip" to 3L)),
                    adapter = FakeAdapter(listOf(entry("x", "h1"))),
                )

            val (pending, _) = h.connections().connect(conn(owner = "other", repo = "repo"))

            assertEquals(PendingConnection.Kind.New, pending.kind)
            assertEquals(setOf("c1/x", "s1/clip"), h.keys())
            assertEquals(setOf("s1/clip"), h.bookmarks.entries.value.keys)
            assertEquals(setOf(SourceId("c1"), LINK_SOURCE), h.local.sourceRows.keys)
            assertEquals(0, h.state.sweep.size)
            assertNull(h.state.attemptRecord)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RC-15 확정 중 구독자는 새 노트와 옛 노트를 같이 보지 않는다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"), cached("b", "h1"))),
                    adapter = FakeAdapter(listOf(entry("x", "h1"), entry("y", "h1"))),
                )
            val seen = mutableListOf<Set<String>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                h.local.observeNotes().collect { rows -> seen += rows.map { it.sourceId }.toSet() }
            }

            h.connections().connect(conn(owner = "other", repo = "repo"))

            assertTrue(seen.none { "s0" in it && "c1" in it })
            assertEquals(setOf("c1"), seen.last())
            assertEquals(1, h.local.promoteCalls)
        }

    @Test
    fun `RC-15 미확정 소스는 일반 동기화가 건드리지 않는다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    adapter = FakeAdapter(listOf(entry("a", "h1"))),
                )
            val repo = h.connections()
            repo.begin(conn(owner = "other", repo = "repo"), null)
            h.adapter.listed.clear()

            h.notes().sync().getOrThrow()

            assertEquals(listOf(S0), h.adapter.listed.map { it.id })
        }

    @Test
    fun `RC-15 식별자 생성기가 지운 식별자를 주면 피한다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(connected = false),
                    adapter = FakeAdapter(listOf(entry("a", "h1"))),
                    ids = listOf("c1", "c1", "c1", "c2"),
                )
            val (first, _) = h.connections().connect(conn())
            h.connections().disconnect().getOrThrow()

            val (second, _) = h.connections().connect(conn(owner = "again"))

            assertEquals(SourceId("c1"), first.sourceId)
            assertEquals(SourceId("c2"), second.sourceId)
        }

    @Test
    fun `RC-15 확정 전 중단은 옛 연결로 돌아간다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    tokens = FakeTokenStore("T0"),
                    adapter = FakeAdapter(listOf(entry("x", "h1"))),
                )
            val running = h.connections()
            val pending = running.begin(conn(owner = "other", repo = "repo"), "T1")
            running.syncPending(pending).getOrThrow()
            assertEquals(setOf("s0/a", "c1/x"), h.local.store.value.keys)

            h.connections().recover()

            assertEquals(S0, (h.connection() as ConnectionState.Connected).sourceId)
            assertEquals(setOf("s0/a"), h.keys())
            assertEquals(setOf(S0), h.local.sourceRows.keys)
            assertEquals("T0", h.tokens.currentToken())
            assertNull(h.state.attemptRecord)
        }

    @Test
    fun `RC-6 확정 전 중단은 시작 때 시도 전 토큰으로 복구한다`() =
        runTest {
            val h = Harness(local = FakeLocal(connected = false), tokens = FakeTokenStore("T0"))
            h.connections().begin(conn(), "T1")
            assertEquals("T1", h.tokens.currentToken())

            h.connections().recover()

            assertEquals("T0", h.tokens.currentToken())
            assertTrue(h.local.sourceRows.isEmpty())
        }

    @Test
    fun `RC-6 첫 연결이 중단되면 토큰은 없음으로 돌아간다`() =
        runTest {
            val h = Harness(local = FakeLocal(connected = false))
            h.connections().begin(conn(isPublic = false), "T1")

            h.connections().recover()

            assertNull(h.tokens.currentToken())
        }

    @Test
    fun `RC-6 확정 직후 죽어도 확정된 연결의 토큰은 복구하지 않는다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(connected = false),
                    tokens = FakeTokenStore(null),
                    adapter = FakeAdapter(listOf(entry("a", "h1"))),
                )
            val repo = h.connections()
            val pending = repo.begin(conn(isPublic = false), "T1")
            repo.syncPending(pending).getOrThrow()
            val attempt = h.state.attemptRecord
            repo.confirm(pending)
            h.state.attemptRecord = attempt

            h.connections().recover()

            assertEquals("T1", h.tokens.currentToken())
        }

    @Test
    fun `RC-16 연결을 바꿔도 학습 기록은 남는다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    adapter = FakeAdapter(listOf(entry("x", "h1"))),
                )
            val studyLogRows = listOf("read", "quiz", "bookmark")

            h.connections().connect(conn(owner = "other", repo = "repo"))
            h.connections().disconnect().getOrThrow()

            assertEquals(listOf("read", "quiz", "bookmark"), studyLogRows)
            assertEquals(ConnectionState.None, h.connection())
        }

    @Test
    fun `RC-17 끊으면 그 소스의 노트 북마크 토큰만 지운다`() =
        runTest {
            val h =
                Harness(
                    local =
                        FakeLocal(
                            initial = listOf(cached("a", "h1"), cached("clip", "r1", source = LINK_SOURCE)),
                            sources = listOf(SourceRecord(LINK_SOURCE, SourceKind.Link, null)),
                        ),
                    tokens = FakeTokenStore("T"),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L, "s0/orphan" to 2L, "s1/clip" to 3L)),
                )

            h.connections().disconnect().getOrThrow()

            assertEquals(ConnectionState.None, h.connection())
            assertEquals(setOf("s1/clip"), h.keys())
            assertEquals(setOf("s1/clip"), h.bookmarks.entries.value.keys)
            assertNull(h.tokens.currentToken())
            assertEquals(setOf(LINK_SOURCE), h.local.sourceRows.keys)
            assertTrue(h.state.seedBlocked)
            assertNull(h.state.disconnect)
        }

    @Test
    fun `RC-17 끊는 동안 돌던 동기화는 지운 소스의 노트를 되살리지 않는다`() =
        runTest {
            lateinit var h: Harness
            h =
                Harness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    adapter =
                        FakeAdapter(
                            listOf(entry("a", "h2"), entry("b", "h1")),
                            onFetch = { h.connections().disconnect().getOrThrow() },
                        ),
                )

            val outcome = h.notes().sync().getOrThrow()

            assertEquals(SyncOutcome(), outcome)
            assertTrue(h.keys().isEmpty())
            assertEquals(ConnectionState.None, h.connection())
        }

    @Test
    fun `RC-17 시작 복구는 소스 행이 없는 노트를 지운다`() =
        runTest {
            val h =
                Harness(
                    local =
                        FakeLocal(
                            initial = listOf(cached("a", "h1"), cached("x", "r1", source = SourceId("gone"))),
                        ),
                )

            h.connections().recover()

            assertEquals(setOf("s0/a"), h.keys())
        }

    @Test
    fun `RC-17 북마크 삭제는 화면 없이도 끝난다`() =
        runTest {
            val h = Harness(bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L, "s0/b" to 2L)))

            h.connections().disconnect().getOrThrow()

            assertTrue(
                h.bookmarks.entries.value
                    .isEmpty(),
            )
        }

    @Test
    fun `RC-17 노트 삭제가 실패하면 북마크와 토큰은 그대로다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(initial = listOf(cached("a", "h1")), deleteError = IllegalStateException("disk")),
                    tokens = FakeTokenStore("T"),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L)),
                )

            val result = h.connections().disconnect()

            assertTrue(result.isFailure)
            assertEquals(setOf("s0/a"), h.bookmarks.entries.value.keys)
            assertEquals("T", h.tokens.currentToken())
            assertEquals(setOf("s0/a"), h.keys())
            assertFalse(h.state.seedBlocked)
            assertNull(h.state.disconnect)
        }

    @Test
    fun `RC-17 끊기가 중간에 죽어도 재시작이 남은 단계를 마친다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(connected = false),
                    tokens = FakeTokenStore("T"),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L, "s1/clip" to 3L)),
                )
            h.state.markDisconnect("s0")

            h.connections().recover()

            assertEquals(setOf("s1/clip"), h.bookmarks.entries.value.keys)
            assertNull(h.tokens.currentToken())
            assertNull(h.state.disconnect)
            assertTrue(h.state.seedBlocked)
            assertEquals(ConnectionState.None, h.connection())
        }

    @Test
    fun `RC-17 끊기 표식 뒤 노트 삭제 전에 죽으면 옛 상태 그대로다`() =
        runTest {
            val h = Harness(tokens = FakeTokenStore("T"), bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L)))
            h.state.markDisconnect("s0")

            h.connections().recover()

            assertEquals(setOf("s0/a"), h.bookmarks.entries.value.keys)
            assertEquals("T", h.tokens.currentToken())
            assertNull(h.state.disconnect)
            assertFalse(h.state.seedBlocked)
            assertTrue(h.connection() is ConnectionState.Connected)
        }

    @Test
    fun `RC-17 새 연결이 확정되면 끊은 표식이 풀린다`() =
        runTest {
            val h = Harness(adapter = FakeAdapter(listOf(entry("a", "h1"))))
            h.connections().disconnect().getOrThrow()
            assertTrue(h.state.seedBlocked)
            assertFalse(h.connections().isSeedEligible())

            h.connections().connect(conn(owner = "again"))

            assertFalse(h.state.seedBlocked)
        }

    @Test
    fun `RC-19 시드 없는 옛 노트는 처음 연결하는 저장소가 이어받는다`() =
        runTest {
            val old = (0 until 22).map { cached("n$it", "h1") }
            val h =
                Harness(
                    local =
                        FakeLocal(
                            initial = old,
                            connected = false,
                            sources = listOf(SourceRecord(S0, SourceKind.GitHub, null)),
                        ),
                    bookmarks = FakeBookmarkStore(mapOf("s0/n0" to 1L, "s0/n21" to 2L)),
                    adapter = FakeAdapter((0 until 20).map { entry("n$it", "h1") }),
                )
            val repo = h.connections()

            assertEquals(ConnectionState.None, h.connection())
            assertEquals(ErasureCount(notes = 22, bookmarks = 2), repo.countErasable())
            val (pending, _) = repo.connect(conn(owner = "any", repo = "repo"))

            assertEquals(PendingConnection.Kind.Adopt, pending.kind)
            assertEquals(S0, pending.sourceId)
            assertEquals(20, h.keys().size)
            assertEquals(setOf("s0/n0"), h.bookmarks.entries.value.keys)
            assertEquals(S0, (h.connection() as ConnectionState.Connected).sourceId)
        }

    @Test
    fun `RC-15 이어받기는 새 목록에 없는 노트만 센다`() =
        runTest {
            val old = (0 until 22).map { cached("n$it", "h1") }
            val h =
                Harness(
                    local =
                        FakeLocal(
                            initial = old,
                            connected = false,
                            sources = listOf(SourceRecord(S0, SourceKind.GitHub, null)),
                        ),
                    bookmarks = FakeBookmarkStore(mapOf("s0/n0" to 1L, "s0/n21" to 2L, "other/x" to 3L)),
                    adapter = FakeAdapter(emptyList()),
                )

            val counted = h.connections().countAdoptionErasable((0 until 20).map { "n$it" }.toSet())

            assertEquals(ErasureCount(notes = 2, bookmarks = 1), counted)
            assertEquals(0, h.adapter.lists)
        }

    @Test
    fun `RC-19 이어받기 첫 동기화가 실패하면 연결 없음과 옛 노트로 돌아간다`() =
        runTest {
            val old = (0 until 22).map { cached("n$it", "h1") }
            val h =
                Harness(
                    local =
                        FakeLocal(
                            initial = old,
                            connected = false,
                            sources = listOf(SourceRecord(S0, SourceKind.GitHub, null)),
                        ),
                    bookmarks = FakeBookmarkStore(mapOf("s0/n0" to 1L)),
                    adapter = FakeAdapter(emptyList(), listError = IllegalStateException("net")),
                )
            val repo = h.connections()

            val pending = repo.begin(conn(), "T1")
            assertTrue(repo.syncPending(pending).isFailure)
            repo.discard(pending)

            assertEquals(ConnectionState.None, h.connection())
            assertEquals(22, h.keys().size)
            assertEquals(setOf("s0/n0"), h.bookmarks.entries.value.keys)
            assertNull(h.tokens.currentToken())
        }

    @Test
    fun `RC-19 이어받기 중 죽어도 옛 노트와 북마크는 남는다`() =
        runTest {
            val old = (0 until 22).map { cached("n$it", "h1") }
            val h =
                Harness(
                    local =
                        FakeLocal(
                            initial = old,
                            connected = false,
                            sources = listOf(SourceRecord(S0, SourceKind.GitHub, null)),
                        ),
                    bookmarks = FakeBookmarkStore(mapOf("s0/n0" to 1L)),
                    adapter = FakeAdapter(emptyList(), listError = IllegalStateException("net")),
                )
            h.connections().begin(conn(), "T1")

            h.connections().recover()

            assertEquals(ConnectionState.None, h.connection())
            assertEquals(22, h.keys().size)
            assertEquals(setOf("s0/n0"), h.bookmarks.entries.value.keys)
            assertNull(h.tokens.currentToken())
        }

    @Test
    fun `RC-19 끊은 뒤 재시작해도 시드 대상이 아니다`() =
        runTest {
            val h = Harness()
            h.connections().disconnect().getOrThrow()

            assertFalse(h.connections().isSeedEligible())
        }

    @Test
    fun `SY-3 연결이 없으면 요청도 오류도 없다`() =
        runTest {
            val h =
                Harness(
                    local =
                        FakeLocal(
                            initial = listOf(cached("a", "h1"), cached("b", "h1"), cached("c", "h1")),
                            sources = listOf(SourceRecord(S0, SourceKind.GitHub, null)),
                            connected = false,
                        ),
                )

            val outcome = h.notes().sync().getOrThrow()

            assertEquals(0, h.adapter.lists)
            assertFalse(outcome.hasChanges)
            assertEquals(3, h.keys().size)
        }

    @Test
    fun `SY-3 동기화는 소스를 만들지 않는다`() =
        runTest {
            val h = Harness(local = FakeLocal(connected = false))

            h.notes().sync().getOrThrow()
            h.notes().sync().getOrThrow()
            assertTrue(h.local.sourceRows.isEmpty())

            val connected = Harness(adapter = FakeAdapter(listOf(entry("a", "h1"))))
            connected.connections().disconnect().getOrThrow()
            connected.notes().sync().getOrThrow()
            assertTrue(connected.local.sourceRows.isEmpty())
        }

    @Test
    fun `RC-15 확정이 승격 뒤에 취소돼도 확정 소스가 남는다`() =
        runTest {
            val h =
                Harness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L)),
                    adapter = FakeAdapter(listOf(entry("x", "h1"))),
                )
            val repo = h.connections()
            val pending = repo.begin(conn(owner = "other", repo = "repo"), null)
            repo.syncPending(pending).getOrThrow()
            val job = launch { repo.confirm(pending) }
            h.bookmarks.onRemoveByPrefix = {
                job.cancel()
                yield()
            }

            job.join()
            repo.discard(pending)

            assertEquals(setOf("c1/x"), h.keys())
            assertEquals(SourceId("c1"), (h.connection() as ConnectionState.Connected).sourceId)
            assertNull(h.state.attemptRecord)
            assertFalse(repo.observeAttempting().first())
        }

    @Test
    fun `RC-15 이미 확정된 소스는 폐기가 지우지 않는다`() =
        runTest {
            val h = Harness(local = FakeLocal(connected = false), adapter = FakeAdapter(listOf(entry("a", "h1"))))
            val repo = h.connections()
            val (pending, _) = repo.connect(conn())

            repo.discard(pending)

            assertEquals(setOf("c1/a"), h.keys())
            assertTrue(h.connection() is ConnectionState.Connected)
        }

    @Test
    fun `RC-15 · SY-1 confirm 이 예외로 끝나도 시도 중 표시가 풀린다`() =
        runTest {
            val h = Harness(adapter = FakeAdapter(listOf(entry("x", "h1"))))
            val repo = h.connections()
            val pending = repo.begin(conn(owner = "other", repo = "repo"), null)
            repo.syncPending(pending).getOrThrow()
            assertTrue(repo.observeAttempting().first())
            h.local.promoteError = IllegalStateException("disk")

            assertTrue(runCatching { repo.confirm(pending) }.isFailure)

            assertFalse(repo.observeAttempting().first())
        }

    @Test
    fun `RC-6 같은 설정에 새 토큰으로 연결하다 죽으면 시도 전 토큰으로 돌아온다`() =
        runTest {
            val h = Harness(tokens = FakeTokenStore("T0"), adapter = FakeAdapter(listOf(entry("a", "h1"))))
            val running = h.connections()
            val pending = running.begin(conn(), "T1")
            running.syncPending(pending).getOrThrow()
            assertEquals("T1", h.tokens.currentToken())

            h.connections().recover()

            assertEquals("T0", h.tokens.currentToken())
            assertNull(h.state.attemptRecord)
        }

    @Test
    fun `RC-15 공개 저장소로 바꾸면 확정 때 옛 토큰을 지운다`() =
        runTest {
            val h = Harness(tokens = FakeTokenStore("T0"), adapter = FakeAdapter(listOf(entry("x", "h1"))))

            h.connections().connect(conn(owner = "other", repo = "repo", isPublic = true))

            assertNull(h.tokens.currentToken())
        }

    @Test
    fun `RC-7 한도로 넣은 토큰은 공개 연결에서도 남는다`() =
        runTest {
            val h = Harness(tokens = FakeTokenStore("T0"), adapter = FakeAdapter(listOf(entry("x", "h1"))))

            h.connections().connect(conn(owner = "other", repo = "repo", isPublic = true), token = "T1")

            assertEquals("T1", h.tokens.currentToken())
        }

    @Test
    fun `RC-15 같은 저장소 변경은 확정 때 토큰을 지우지 않는다`() =
        runTest {
            val h = Harness(tokens = FakeTokenStore("T0"), adapter = FakeAdapter(listOf(entry("a", "h1"))))

            h.connections().connect(conn(branch = "dev", isPublic = true))

            assertEquals("T0", h.tokens.currentToken())
        }

    @Test
    fun `RC-15 비공개로 바꾸면 토큰을 지우지 않는다`() =
        runTest {
            val h = Harness(tokens = FakeTokenStore("T0"), adapter = FakeAdapter(listOf(entry("x", "h1"))))

            h.connections().connect(conn(owner = "other", repo = "repo", isPublic = false))

            assertEquals("T0", h.tokens.currentToken())
        }

    @Test
    fun `SY-1 확정 때 옛 토큰을 지워도 동기화는 다시 시작되지 않는다`() =
        runTest {
            val h =
                Harness(
                    tokens = FakeTokenStore("T0"),
                    adapter = FakeAdapter(listOf(entry("x", "h1"))),
                    autoApply = false,
                )
            val repo = h.connections()
            val notes = CountingNotes()
            val runs = ObserveSyncRunsUseCase(SyncNotesUseCase(notes), ObserveAppliedTokenUseCase(h.applied), repo)
            backgroundScope.launch { runs(MutableStateFlow(0)).collect {} }
            runCurrent()
            assertEquals(1, notes.syncs)
            val pending = repo.begin(conn(owner = "other", repo = "repo", isPublic = true), null)
            runCurrent()
            repo.syncPending(pending).getOrThrow()

            val confirm = launch { repo.confirm(pending) }
            runCurrent()
            assertTrue(repo.observeAttempting().first())
            h.applied.state.value = null
            confirm.join()
            runCurrent()

            assertFalse(repo.observeAttempting().first())
            assertNull(h.tokens.currentToken())
            assertEquals(1, notes.syncs)
        }

    @Test
    fun `SY-1 실패 폐기로 토큰을 되돌려도 동기화는 다시 시작되지 않는다`() =
        runTest {
            val h =
                Harness(
                    tokens = FakeTokenStore("T0"),
                    adapter = FakeAdapter(listOf(entry("x", "h1"))),
                    autoApply = false,
                )
            val repo = h.connections()
            val notes = CountingNotes()
            val runs = ObserveSyncRunsUseCase(SyncNotesUseCase(notes), ObserveAppliedTokenUseCase(h.applied), repo)
            backgroundScope.launch { runs(MutableStateFlow(0)).collect {} }
            runCurrent()
            assertEquals(1, notes.syncs)
            val pending = repo.begin(conn(owner = "other", repo = "repo", isPublic = false), "T1")
            h.applied.state.value = "T1"
            runCurrent()
            repo.syncPending(pending).getOrThrow()

            val discard = launch { repo.discard(pending) }
            runCurrent()
            assertTrue(repo.observeAttempting().first())
            h.applied.state.value = "T0"
            discard.join()
            runCurrent()

            assertFalse(repo.observeAttempting().first())
            assertEquals("T0", h.tokens.currentToken())
            assertEquals(1, notes.syncs)
        }
}
