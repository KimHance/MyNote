package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.ConnectionIdGenerator
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.InvalidDraftException
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteOrigin
import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import com.hancekim.mynote.core.domain.model.connectBandNoteCount
import com.hancekim.mynote.core.format.DeviceNoteMarkdown
import com.hancekim.mynote.core.format.NoteFormat
import com.hancekim.mynote.core.format.PARSER_VERSION
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private const val DEFAULT_GROUP = "메모"

private val OTHER = RepoConnection("other", "notes", "main", "notes/index.json", isPublic = true)

private class DeviceHarness(
    val local: FakeLocal = FakeLocal(connected = false),
    val bookmarks: FakeBookmarkStore = FakeBookmarkStore(),
    val adapter: FakeAdapter = FakeAdapter(emptyList()),
    docIds: List<String> = (1..20).map { "d$it" },
    connectionIds: List<String> = listOf("c1", "c2", "c3"),
) {
    val tokens = FakeTokenStore()
    val state = FakeStateStore()
    private val applied = FakeAppliedToken().also { tokens.mirror = it.state }
    private val docQueue = ArrayDeque(docIds)
    private val connectionQueue = ArrayDeque(connectionIds)
    var clock = 1_000L
    var day: LocalDate = LocalDate.of(2026, 10, 14)

    val devices =
        DeviceNotesRepositoryImpl(
            local,
            bookmarks,
            NoteFormat(),
            DeviceNoteMarkdown(),
            { docQueue.removeFirst() },
            { clock++ },
            { day },
        )

    fun recovery() = ConnectionRecovery(local, tokens, bookmarks, state)

    fun notes() = NotesRepositoryImpl(setOf(adapter), local, NoteFormat(), recovery()::ensure) { clock }

    fun connections() =
        ConnectionRepositoryImpl(
            setOf(adapter),
            local,
            tokens,
            bookmarks,
            state,
            ConnectionIdGenerator { connectionQueue.removeFirst() },
            NoteFormat(),
            recovery(),
            applied,
        ) { clock }

    suspend fun save(
        title: String,
        body: String = "본문 $title",
        group: String = "",
    ): String = devices.create(DeviceNoteDraft(title = title, group = group, body = body), DEFAULT_GROUP).getOrThrow()

    suspend fun categories(): List<Category> = notes().observeCategories().first()

    suspend fun note(key: String): Note = categories().flatMap { it.notes }.single { it.id == key }

    fun deviceKeys(): Set<String> =
        local.store.value.keys
            .filterTo(HashSet()) { it.startsWith("local/") }

    fun repositoryKeys(): Set<String> =
        local.store.value.keys
            .filterNotTo(HashSet()) { it.startsWith("local/") }
}

private suspend fun ConnectionRepositoryImpl.connect(connection: RepoConnection): PendingConnection {
    val pending = begin(connection, null)
    syncPending(pending).getOrThrow()
    confirm(pending)
    return pending
}

private fun markdown(body: String): List<NoteBlock> = NoteFormat().parse("n.md", body)

private fun legacy(): SourceRecord = SourceRecord(S0, SourceKind.GitHub, null)

class DeviceNotesTest {
    @Test
    fun `LN-3 저장하면 기기 노트가 하나 생기고 리더 블록이 마크다운 결과와 같다`() =
        runTest {
            val h = DeviceHarness()
            val body = "# 제목\n\n문단 **굵게** 와 `코드`\n\n- 하나\n- 둘\n"

            val key = h.save("첫 노트", body)

            assertEquals(setOf(key), h.deviceKeys())
            val note = h.note(key)
            assertEquals(NoteOrigin.Device, note.origin)
            assertEquals("첫 노트", note.title)
            assertEquals(markdown(body), note.blocks)
            assertEquals(
                "d1.md",
                h.local.store.value
                    .getValue(key)
                    .path,
            )
        }

    @Test
    fun `LN-3 미리보기와 저장 뒤 리더가 같다`() =
        runTest {
            val h = DeviceHarness()
            val bodies =
                listOf(
                    "문단\n\n| a | b |\n|---|--:|\n| 1 | 2 |\n",
                    "---\ntitle: x\n---\n가",
                    "> 인용\n>\n> - [x] 체크\n",
                    "> ".repeat(40) + "깊음",
                )
            bodies.forEachIndexed { index, body ->
                val preview = h.devices.preview(body)
                val key = h.save("노트 $index", body)
                assertEquals(body, preview, h.note(key).blocks)
            }
        }

    @Test
    fun `LN-3 본문 첫 줄 구분선은 앞머리로 먹히지 않는다`() =
        runTest {
            val h = DeviceHarness()

            val key = h.save("구분선", "---\ntitle: x\n---\n가")

            val blocks = h.note(key).blocks
            assertEquals(NoteBlock.Divider, blocks.first())
            assertTrue(blocks.toString(), blocks.any { it.toString().contains("title: x") })
            assertEquals("---\ntitle: x\n---\n가", h.devices.load(key)?.body)
        }

    @Test
    fun `LN-3 크기 경계는 저장 원문 기준으로 리더 한도와 같다`() =
        runTest {
            val h = DeviceHarness()
            val largest = "a".repeat(524_280)

            val key = h.save("가장 큰 본문", largest)

            val blocks = h.note(key).blocks
            assertFalse(blocks.any { it is NoteBlock.Unsupported && it.reason == UnsupportedReason.LimitExceeded })
            listOf(524_281, 524_289).forEach { size ->
                val failure =
                    h.devices
                        .create(
                            DeviceNoteDraft("큼", "", "a".repeat(size)),
                            DEFAULT_GROUP,
                        ).exceptionOrNull()
                assertTrue("$size", (failure as InvalidDraftException).check.bodyTooLarge)
            }
            assertEquals(1, h.deviceKeys().size)
        }

    @Test
    fun `LN-3 제목 본문 공백은 저장할 수 없다`() =
        runTest {
            val h = DeviceHarness()

            val blankTitle = h.devices.create(DeviceNoteDraft(" \n ", "", "본문"), DEFAULT_GROUP).exceptionOrNull()
            val blankBody = h.devices.create(DeviceNoteDraft("제목", "", " \t\n"), DEFAULT_GROUP).exceptionOrNull()

            assertTrue((blankTitle as InvalidDraftException).check.titleBlank)
            assertTrue((blankBody as InvalidDraftException).check.bodyBlank)
            assertTrue(h.deviceKeys().isEmpty())
        }

    @Test
    fun `LN-3 다른 한도를 넘는 본문은 상자로 저장된다`() =
        runTest {
            val h = DeviceHarness()

            val key = h.save("깊은 노트", "> ".repeat(33) + "깊음")

            val box = h.note(key).blocks.single() as NoteBlock.Unsupported
            assertEquals(UnsupportedReason.LimitExceeded, box.reason)
        }

    @Test
    fun `LN-3 제목과 그룹은 한 줄 200자로 저장된다`() =
        runTest {
            val h = DeviceHarness()

            val key = h.save("줄\n바꿈" + "가".repeat(1_000_000), body = "본문", group = " 그룹\r\n이름 ")

            val row =
                h.local.store.value
                    .getValue(key)
            assertEquals("줄 바꿈" + "가".repeat(196), row.title)
            assertEquals("그룹 이름", row.group)
        }

    @Test
    fun `LN-4 그룹 칸을 비우면 기본 그룹이다`() =
        runTest {
            val h = DeviceHarness()

            val key = h.save("그룹 없음", group = "   ")

            assertEquals(DEFAULT_GROUP, h.categories().single { cat -> cat.notes.any { it.id == key } }.name)
        }

    @Test
    fun `LN-4 같은 이름 그룹은 저장소 노트와 한 그룹이다`() =
        runTest {
            val h = DeviceHarness(local = FakeLocal(initial = listOf(cached("a", "h1")), connected = true))

            val same = h.save("같은 그룹", group = " android ")
            val other = h.save("다른 그룹", group = "Android")

            val android = h.categories().filter { it.name == "android" }
            assertEquals(1, android.size)
            assertEquals(setOf("s0/a", same), android.single().notes.mapTo(HashSet()) { it.id })
            assertEquals("Android", h.categories().single { cat -> cat.notes.any { it.id == other } }.name)
        }

    @Test
    fun `LN-4 고쳐도 날짜는 만든 날이다`() =
        runTest {
            val h = DeviceHarness()
            val key = h.save("날짜")
            val firstSeen =
                h.local.store.value
                    .getValue(key)
                    .firstSeenAt

            h.day = LocalDate.of(2026, 10, 15)
            h.devices.update(key, DeviceNoteDraft("날짜 고침", "", "새 본문"), DEFAULT_GROUP).getOrThrow()

            val row =
                h.local.store.value
                    .getValue(key)
            assertEquals("2026.10.14", row.date)
            assertEquals(LocalDate.of(2026, 10, 14).toEpochDay(), row.epochDay)
            assertEquals(firstSeen, row.firstSeenAt)
            assertEquals(LocalDate.of(2026, 10, 14), h.devices.load(key)?.date)
        }

    @Test
    fun `LN-4 가져온 글의 날짜가 있으면 그 날이다`() =
        runTest {
            val h = DeviceHarness()

            val key =
                h.devices
                    .create(DeviceNoteDraft("가져옴", "", "본문", LocalDate.of(2025, 1, 2)), DEFAULT_GROUP)
                    .getOrThrow()

            assertEquals("2025.01.02", h.note(key).date)
        }

    @Test
    fun `LN-5 기기 노트 키는 저장소 노트 키와 겹치지 않는다`() =
        runTest {
            val h = DeviceHarness(local = FakeLocal(initial = listOf(cached("a", "h1"))), docIds = listOf("a"))

            val key = h.save("같은 문서 id")

            assertEquals("local/a", key)
            assertNotEquals(SourceId.Legacy, SourceId.Device)
            assertEquals(setOf("s0/a", "local/a"), h.local.store.value.keys)
            assertEquals(NoteOrigin.Repository, h.note("s0/a").origin)
            assertEquals(NoteOrigin.Device, h.note("local/a").origin)
        }

    @Test
    fun `LN-5 같은 제목도 다른 키다`() =
        runTest {
            val h = DeviceHarness()

            val first = h.save("같은 제목")
            val second = h.save("같은 제목")

            assertNotEquals(first, second)
            assertEquals(setOf(first, second), h.deviceKeys())
        }

    @Test
    fun `LN-5 고쳐도 키와 북마크가 그대로다`() =
        runTest {
            val h = DeviceHarness()
            val key = h.save("고칠 노트", group = "옛 그룹")
            h.bookmarks.put(key, 7L)

            h.devices.update(key, DeviceNoteDraft("새 제목", "새 그룹", "새 본문"), DEFAULT_GROUP).getOrThrow()

            assertEquals(setOf(key), h.deviceKeys())
            assertEquals(mapOf(key to 7L), h.bookmarks.entries.value)
        }

    @Test
    fun `LN-5 연결 식별자 생성기가 기기 소스 id 를 주면 피한다`() =
        runTest {
            val h = DeviceHarness(connectionIds = listOf(SourceId.DEVICE_ID, "c1"))
            h.save("기기 노트")

            val pending = h.connections().begin(OTHER, null)

            assertEquals(SourceId("c1"), pending.sourceId)
        }

    @Test
    fun `LN-9 저장소 노트에는 고치기와 지우기가 없다`() =
        runTest {
            val h = DeviceHarness(local = FakeLocal(initial = listOf(cached("a", "h1"))))
            h.bookmarks.put("s0/a", 1L)
            val before = h.local.store.value

            val update = h.devices.update("s0/a", DeviceNoteDraft("고침", "", "본문"), DEFAULT_GROUP)
            val delete = h.devices.delete("s0/a")

            assertTrue(update.isFailure)
            assertTrue(delete.isFailure)
            assertNull(h.devices.load("s0/a"))
            assertEquals(before, h.local.store.value)
            assertEquals(setOf("s0/a"), h.bookmarks.entries.value.keys)
            assertFalse(h.note("s0/a").origin.editable)
        }

    @Test
    fun `LN-9 고쳐 저장하면 리더가 새 내용이다`() =
        runTest {
            val h = DeviceHarness()
            val key = h.save("옛 제목", "옛 본문", group = "옛 그룹")

            h.devices.update(key, DeviceNoteDraft("새 제목", "새 그룹", "# 새 본문\n\n문단"), DEFAULT_GROUP).getOrThrow()

            val category = h.categories().single()
            assertEquals("새 그룹", category.name)
            assertEquals("새 제목", category.notes.single().title)
            assertEquals(markdown("# 새 본문\n\n문단"), category.notes.single().blocks)
            assertEquals(
                DeviceNoteDraft("새 제목", "새 그룹", "# 새 본문\n\n문단", LocalDate.of(2026, 10, 14)),
                h.devices.load(key),
            )
        }

    @Test
    fun `LN-9 고쳐도 북마크 학습 기록 날짜가 그대로다`() =
        runTest {
            val h = DeviceHarness()
            val key = h.save("노트")
            h.bookmarks.put(key, 3L)
            val before =
                h.local.store.value
                    .getValue(key)

            h.day = LocalDate.of(2027, 1, 1)
            h.devices.update(key, DeviceNoteDraft("노트 고침", "", "새 본문"), DEFAULT_GROUP).getOrThrow()

            val after =
                h.local.store.value
                    .getValue(key)
            assertEquals(before.date, after.date)
            assertEquals(before.epochDay, after.epochDay)
            assertEquals(before.firstSeenAt, after.firstSeenAt)
            assertEquals(before.path, after.path)
            assertEquals(mapOf(key to 3L), h.bookmarks.entries.value)
        }

    @Test
    fun `LN-9 다시 해석하기와 겹쳐 고쳐도 새 본문이 남는다`() =
        runTest {
            val h = DeviceHarness()
            val key = h.save("노트", "옛 본문")
            h.local.store.value =
                h.local.store.value + (
                    key to
                        h.local.store.value
                            .getValue(key)
                            .copy(parserVersion = 0)
                )
            h.local.onLoadStale = {
                h.local.onLoadStale = null
                h.devices.update(key, DeviceNoteDraft("노트", "", "새 본문"), DEFAULT_GROUP).getOrThrow()
            }

            h.notes().sync().getOrThrow()

            assertEquals(markdown("새 본문"), h.note(key).blocks)
            assertEquals(
                PARSER_VERSION,
                h.local.store.value
                    .getValue(key)
                    .parserVersion,
            )
        }

    @Test
    fun `LN-11 지우면 노트와 북마크만 지우고 기록은 남는다`() =
        runTest {
            val h =
                DeviceHarness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L)),
                )
            val gone = h.save("지울 노트")
            val kept = h.save("남는 노트")
            h.bookmarks.put(gone, 2L)
            h.bookmarks.put(kept, 3L)

            h.devices.delete(gone).getOrThrow()

            assertEquals(setOf(kept), h.deviceKeys())
            assertEquals(setOf("s0/a"), h.repositoryKeys())
            assertEquals(setOf("s0/a", kept), h.bookmarks.entries.value.keys)
            assertTrue(h.devices.delete(gone).isFailure)
        }

    @Test
    fun `LN-11 마지막 기기 노트를 지우면 노트가 0 이다`() =
        runTest {
            val h = DeviceHarness()
            val key = h.save("하나뿐")

            h.devices.delete(key).getOrThrow()

            assertTrue(h.categories().isEmpty())
        }

    @Test
    fun `LN-11 노트 삭제가 실패하면 북마크는 그대로다`() =
        runTest {
            val local = FakeLocal(connected = false, deleteError = IllegalStateException("디스크"))
            val h = DeviceHarness(local = local)
            val key = h.save("노트")
            h.bookmarks.put(key, 1L)

            assertTrue(h.devices.delete(key).isFailure)

            assertEquals(setOf(key), h.deviceKeys())
            assertEquals(setOf(key), h.bookmarks.entries.value.keys)
        }

    @Test
    fun `LN-12 동기화는 기기 노트를 건드리지 않는다`() =
        runTest {
            val h =
                DeviceHarness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"), cached("b", "h1"))),
                    adapter = FakeAdapter(listOf(entry("a", "h2"))),
                    docIds = listOf("a", "b", "z"),
                )
            val keys = listOf(h.save("a"), h.save("b"), h.save("z"))
            h.bookmarks.put(keys.first(), 1L)
            val before =
                keys.associateWith {
                    h.local.store.value
                        .getValue(it)
                }

            val outcome = h.notes().sync().getOrThrow()

            assertEquals(listOf("s0/b"), outcome.deleted)
            assertEquals(
                before,
                keys.associateWith {
                    h.local.store.value
                        .getValue(it)
                },
            )
            assertEquals(setOf(keys.first()), h.bookmarks.entries.value.keys)
        }

    @Test
    fun `LN-12 시작 복구는 기기 노트를 지우지 않는다`() =
        runTest {
            val h = DeviceHarness()
            val keys = List(3) { h.save("노트 $it") }
            h.bookmarks.put(keys.first(), 1L)

            h.recovery().run()

            assertEquals(keys.toSet(), h.deviceKeys())
            assertEquals(
                SourceKind.Local,
                h.local.sourceRows
                    .getValue(SourceId.Device)
                    .kind,
            )
            assertEquals(setOf(keys.first()), h.bookmarks.entries.value.keys)
        }

    @Test
    fun `LN-12 기기 노트만 있으면 첫 연결은 이어받기가 아니다`() =
        runTest {
            val h = DeviceHarness(adapter = FakeAdapter(listOf(entry("r", "h1"))))
            val keys = List(3) { h.save("노트 $it") }
            h.bookmarks.put(keys.first(), 1L)
            val connections = h.connections()

            assertEquals(ConnectionState.None, connections.observeConnection().first())
            assertEquals(0, connections.countErasable().notes)
            assertEquals(0, connections.countAdoptionErasable(emptySet()).notes)
            val pending = connections.connect(CONNECTION)

            assertEquals(PendingConnection.Kind.New, pending.kind)
            assertEquals(keys.toSet(), h.deviceKeys())
            assertEquals(setOf("c1/r"), h.repositoryKeys())
            assertEquals(setOf(keys.first()), h.bookmarks.entries.value.keys)
        }

    @Test
    fun `LN-12 이어받기 개수에 기기 노트가 없다`() =
        runTest {
            val h =
                DeviceHarness(
                    local =
                        FakeLocal(
                            initial = listOf(cached("a", "h1"), cached("b", "h1"), cached("c", "h1")),
                            connected = false,
                            sources = listOf(legacy()),
                        ),
                    bookmarks = FakeBookmarkStore(mapOf("s0/c" to 1L)),
                    adapter = FakeAdapter(listOf(entry("a", "h1"))),
                )
            val keys = List(2) { h.save("기기 $it") }
            h.bookmarks.put(keys.first(), 2L)
            val connections = h.connections()

            val count = connections.countAdoptionErasable(setOf("a"))
            val pending = connections.connect(CONNECTION)

            assertEquals(2, count.notes)
            assertEquals(1, count.bookmarks)
            assertEquals(PendingConnection.Kind.Adopt, pending.kind)
            assertEquals(S0, pending.sourceId)
            assertEquals(setOf("s0/a"), h.repositoryKeys())
            assertEquals(keys.toSet(), h.deviceKeys())
            assertEquals(setOf(keys.first()), h.bookmarks.entries.value.keys)
        }

    @Test
    fun `LN-12 바꾸기와 끊기는 기기 노트를 남긴다`() =
        runTest {
            val h =
                DeviceHarness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"), cached("b", "h1"))),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L)),
                    adapter = FakeAdapter(listOf(entry("n", "h1"))),
                )
            val keys = List(3) { h.save("기기 $it") }
            h.bookmarks.put(keys.first(), 2L)
            val connections = h.connections()

            assertEquals(2, connections.countErasable().notes)
            assertEquals(1, connections.countErasable().bookmarks)
            connections.connect(OTHER)

            assertEquals(setOf("c1/n"), h.repositoryKeys())
            assertEquals(keys.toSet(), h.deviceKeys())
            assertEquals(1, connections.countErasable().notes)
            assertEquals(0, connections.countErasable().bookmarks)

            connections.disconnect().getOrThrow()

            assertTrue(h.repositoryKeys().isEmpty())
            assertEquals(keys.toSet(), h.deviceKeys())
            assertEquals(setOf(keys.first()), h.bookmarks.entries.value.keys)
            assertEquals(ConnectionState.None, connections.observeConnection().first())
        }

    @Test
    fun `LN-12 끊기가 중간에 죽어도 기기 노트는 남는다`() =
        runTest {
            val h =
                DeviceHarness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L)),
                )
            val keys = List(3) { h.save("기기 $it") }
            h.bookmarks.put(keys.first(), 2L)
            h.state.markDisconnect(S0.value)
            h.local.deleteSource(S0)

            h.recovery().run()

            assertTrue(h.repositoryKeys().isEmpty())
            assertEquals(keys.toSet(), h.deviceKeys())
            assertEquals(setOf(keys.first()), h.bookmarks.entries.value.keys)
            assertNull(h.state.disconnect)
        }

    @Test
    fun `LN-12 시드 연결은 기기 노트를 이어받지 않는다`() =
        runTest {
            val h = DeviceHarness(adapter = FakeAdapter(listOf(entry("r", "h1"))))
            val keys = List(2) { h.save("기기 $it") }
            val connections = h.connections()

            assertTrue(connections.isSeedEligible())
            val pending = connections.connect(CONNECTION)

            assertEquals(PendingConnection.Kind.New, pending.kind)
            assertEquals(keys.toSet(), h.deviceKeys())
        }

    @Test
    fun `RC-15 바꾸기 개수에 기기 노트와 그 북마크가 들지 않는다`() =
        runTest {
            val h =
                DeviceHarness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L)),
                )
            repeat(4) { h.bookmarks.put(h.save("기기 $it"), 2L) }

            val count = h.connections().countErasable()

            assertEquals(1, count.notes)
            assertEquals(1, count.bookmarks)
        }

    @Test
    fun `RC-17 끊으면 저장소 노트만 지우고 기기 노트와 그 북마크는 남는다`() =
        runTest {
            val h =
                DeviceHarness(
                    local = FakeLocal(initial = listOf(cached("a", "h1"))),
                    bookmarks = FakeBookmarkStore(mapOf("s0/a" to 1L)),
                )
            val key = h.save("기기")
            h.bookmarks.put(key, 2L)

            h.connections().disconnect().getOrThrow()

            assertEquals(setOf(key), h.local.store.value.keys)
            assertEquals(setOf(key), h.bookmarks.entries.value.keys)
            assertEquals(listOf(key), h.categories().flatMap { cat -> cat.notes.map { it.id } })
        }

    @Test
    fun `RC-19 이어받기 대상은 연결 정보 없는 저장소 노트뿐이다`() =
        runTest {
            val deviceOnly = DeviceHarness()
            deviceOnly.save("기기")
            assertEquals(PendingConnection.Kind.New, deviceOnly.connections().begin(CONNECTION, null).kind)

            val withLegacy =
                DeviceHarness(
                    local =
                        FakeLocal(
                            initial = listOf(cached("a", "h1")),
                            connected = false,
                            sources = listOf(legacy()),
                        ),
                )
            withLegacy.save("기기")
            val pending = withLegacy.connections().begin(CONNECTION, null)
            assertEquals(PendingConnection.Kind.Adopt, pending.kind)
            assertEquals(S0, pending.sourceId)
        }

    @Test
    fun `RC-1 띠가 세는 노트는 연결 정보 없는 저장소 노트뿐이다`() =
        runTest {
            val deviceOnly = DeviceHarness()
            repeat(2) { deviceOnly.save("기기 $it") }
            val none = deviceOnly.connections().observeConnection().first()
            assertEquals(ConnectionState.None, none)
            assertEquals(0, deviceOnly.categories().connectBandNoteCount(none))

            val withLegacy =
                DeviceHarness(
                    local =
                        FakeLocal(
                            initial = listOf(cached("a", "h1"), cached("b", "h1")),
                            connected = false,
                            sources = listOf(legacy()),
                        ),
                )
            repeat(3) { withLegacy.save("기기 $it") }
            val legacyState = withLegacy.connections().observeConnection().first()
            assertEquals(2, withLegacy.categories().connectBandNoteCount(legacyState))

            val connected = DeviceHarness(local = FakeLocal(initial = listOf(cached("a", "h1"))))
            connected.save("기기")
            val state = connected.connections().observeConnection().first()
            assertTrue(state is ConnectionState.Connected)
            assertEquals(0, connected.categories().connectBandNoteCount(state))
        }

    @Test
    fun `LN-13 기기 노트가 없는 이전 판 데이터는 그대로 읽힌다`() =
        runTest {
            val notes = (1..22).map { cached("n$it", "h1") }
            val connected =
                DeviceHarness(
                    local = FakeLocal(initial = notes),
                    bookmarks = FakeBookmarkStore(mapOf("s0/n1" to 1L)),
                )
            val state = connected.connections().observeConnection().first()
            val listed = connected.categories().flatMap { it.notes }
            assertTrue(state is ConnectionState.Connected)
            assertEquals(22, listed.size)
            assertTrue(listed.all { it.origin == NoteOrigin.Repository })
            assertEquals(0, connected.categories().connectBandNoteCount(state))
            assertEquals(setOf("s0/n1"), connected.bookmarks.entries.value.keys)

            val unlinked =
                DeviceHarness(local = FakeLocal(initial = notes, connected = false, sources = listOf(legacy())))
            val none = unlinked.connections().observeConnection().first()
            assertEquals(ConnectionState.None, none)
            assertEquals(22, unlinked.categories().connectBandNoteCount(none))
        }

    @Test
    fun `LN-14 기기 노트도 검색과 북마크가 같다`() =
        runTest {
            val h = DeviceHarness()
            val key = h.save("Kotlin 코루틴 정리", group = "kotlin")
            val bookmarks = BookmarkRepositoryImpl(h.bookmarks)

            val found = h.categories().flatMap { it.notes }.filter { it.title.contains("코루틴") }
            bookmarks.add(
                com.hancekim.mynote.core.domain.model
                    .Bookmark(key, 9L),
            )
            val marked = bookmarks.observeBookmarks().first()
            bookmarks.remove(listOf(key))

            assertEquals(listOf(key), found.map { it.id })
            assertEquals(listOf(key), marked.map { it.noteId })
            assertTrue(bookmarks.observeBookmarks().first().isEmpty())
        }

    @Test
    fun `LN-14 기기 노트도 재변환된다`() =
        runTest {
            val h = DeviceHarness()
            val key = h.save("재변환", "# 제목\n\n문단")
            h.local.store.value =
                h.local.store.value +
                (
                    key to
                        h.local.store.value
                            .getValue(key)
                            .copy(parserVersion = 0, blocksJson = "[]")
                )

            h.notes().sync().getOrThrow()

            assertEquals(0, h.adapter.lists)
            assertEquals(
                PARSER_VERSION,
                h.local.store.value
                    .getValue(key)
                    .parserVersion,
            )
            assertEquals(markdown("# 제목\n\n문단"), h.note(key).blocks)
        }

    @Test
    fun `LN-14 기기 노트의 첫 문장은 저장소 노트와 같은 규칙이다`() =
        runTest {
            val h = DeviceHarness()
            val body = "# 제목\n\n첫 문장입니다. 둘째 문장."

            val key = h.save("첫 문장", body)

            assertEquals(
                NoteFormat().derive("n.md", body).excerpt,
                h.local.store.value
                    .getValue(key)
                    .excerpt,
            )
        }

    @Test
    fun `SY-3 기기 노트만 있으면 동기화 요청도 오류도 없다`() =
        runTest {
            val h = DeviceHarness()
            h.save("기기")

            val outcome = h.notes().sync()

            assertTrue(outcome.isSuccess)
            assertEquals(0, h.adapter.lists)
            assertEquals(setOf(SourceId.Device), h.local.sourceRows.keys)
        }

    @Test
    fun `LN-5 기기 소스 행은 연결이 아니다`() =
        runTest {
            val h = DeviceHarness()
            h.save("기기")

            assertEquals(ConnectionState.None, h.connections().observeConnection().first())
            assertEquals(
                null,
                ConnectionConfigCodec.decode(
                    h.local.sourceRows
                        .getValue(SourceId.Device)
                        .configJson,
                ),
            )
        }
}
