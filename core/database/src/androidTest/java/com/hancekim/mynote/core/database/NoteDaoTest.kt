package com.hancekim.mynote.core.database

import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.entity.ReadSessionEntity
import com.hancekim.mynote.core.database.entity.SourceEntity
import com.hancekim.mynote.core.database.entity.SourceNoteEntity
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.RepoConnection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private fun row(
    source: String,
    docId: String,
    title: String = docId,
    epochDay: Long? = 20_000L,
    firstSeenAt: Long = 1L,
) = SourceNoteEntity(
    key = "$source/$docId",
    sourceId = source,
    docId = docId,
    path = null,
    group = "g",
    title = title,
    date = null,
    epochDay = epochDay,
    firstSeenAt = firstSeenAt,
    revision = "r",
    fetchedAt = null,
    parserVersion = 1,
    raw = "{}",
    blocksJson = "[]",
    excerpt = null,
)

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {
    private lateinit var database: NotesDatabase
    private lateinit var dao: NoteDao

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    NotesDatabase::class.java,
                ).build()
        dao = database.noteDao()
    }

    @After
    fun close() {
        database.close()
    }

    private suspend fun keys(): List<String> =
        dao
            .observeNotes(
                ConnectionConfigCodec.PENDING_MARKER,
            ).first()
            .map { it.key }

    @Test
    fun `applySourceSync 는 upsert 와 delete 를 한 번에 반영한다`() =
        runTest {
            dao.upsertAll(listOf(row("s0", "old")))

            dao.applySourceSync("s0", listOf(row("s0", "new")), listOf("old"))

            assertEquals(listOf("s0/new"), keys())
        }

    @Test
    fun `삭제할 키가 900개를 넘어도 나눠서 지운다`() =
        runTest {
            val ids = (1..1_200).map { "n$it" }
            dao.upsertAll(ids.map { row("s0", it) } + row("s0", "keep"))

            dao.applySourceSync("s0", emptyList(), ids)

            assertEquals(listOf("s0/keep"), keys())
        }

    @Test
    fun `다른 소스의 같은 docId 는 지우지 않는다`() =
        runTest {
            dao.upsertAll(listOf(row("s0", "a"), row("s1", "a")))

            dao.applySourceSync("s0", emptyList(), listOf("a"))

            assertEquals(listOf("s1/a"), keys())
        }

    @Test
    fun `정렬은 epoch_day 내림차순 NULL 마지막 처음 본 시각 제목 순이다`() =
        runTest {
            dao.upsertAll(
                listOf(
                    row("s0", "noDate", epochDay = null, firstSeenAt = 9L),
                    row("s0", "older", epochDay = 19_000L),
                    row("s0", "b", title = "b", firstSeenAt = 1L),
                    row("s0", "a", title = "a", firstSeenAt = 1L),
                    row("s0", "newer", firstSeenAt = 5L),
                ),
            )

            assertEquals(listOf("s0/newer", "s0/a", "s0/b", "s0/older", "s0/noDate"), keys())
        }

    @Test
    fun `소스 상태는 그 소스 행만 읽는다`() =
        runTest {
            dao.upsertAll(listOf(row("s0", "a"), row("s1", "b"), row("s0", "c").copy(raw = null)))

            val state = dao.loadSourceState("s0").associateBy { it.docId }

            assertEquals(setOf("a", "c"), state.keys)
            assertEquals(true, state.getValue("a").hasRaw)
            assertEquals(false, state.getValue("c").hasRaw)
        }

    @Test
    fun `NF-8 한도 안 최악 크기 행도 목록 원문 재변환 행 전체 읽기를 지난다`() =
        runTest {
            val limits = NoteLimits.Default
            val raw = "가".repeat(limits.maxBytes / 3) + "a".repeat(limits.maxBytes % 3)
            val blocksJson = "b".repeat(limits.maxCacheBytes)
            dao.upsertAll(
                listOf(
                    row("s0", "worst", title = "제목".repeat(150)).copy(
                        path = "notes/" + "그룹".repeat(50) + "/worst.md",
                        date = "2026.10.07",
                        revision = "f".repeat(64),
                        raw = raw,
                        blocksJson = blocksJson,
                        excerpt = "요".repeat(140),
                    ),
                ),
            )

            assertEquals(
                blocksJson,
                dao
                    .observeNotes(ConnectionConfigCodec.PENDING_MARKER)
                    .first()
                    .single()
                    .blocksJson,
            )
            assertEquals(raw, dao.loadRaw("s0/worst", limits.maxCacheBytes))
            assertEquals(raw, dao.loadStale(2, limits.maxCacheBytes).single().raw)
            val rowBytes =
                database.useReaderConnection { connection ->
                    connection.usePrepared("SELECT * FROM source_notes WHERE note_key = 's0/worst'") { statement ->
                        statement.step()
                        (0 until statement.getColumnCount()).sumOf { column ->
                            if (statement.isNull(column)) 0 else statement.getText(column).toByteArray().size
                        }
                    }
                }
            assertTrue("$rowBytes", rowBytes >= limits.maxBytes + limits.maxCacheBytes)
        }

    @Test
    fun `NF-8 커서 창보다 큰 원문은 읽지 않고 목록과 재변환은 그대로 돈다`() =
        runTest {
            val limit = NoteLimits.Default.maxCacheBytes
            dao.upsertAll(listOf(row("s0", "huge").copy(raw = "a".repeat(3 * 1_024 * 1_024)), row("s0", "ok")))

            assertEquals(setOf("s0/huge", "s0/ok"), keys().toSet())
            assertNull(dao.loadRaw("s0/huge", limit))
            assertEquals("{}", dao.loadRaw("s0/ok", limit))
            assertEquals(listOf("s0/ok"), dao.loadStale(2, limit).map { it.key })
        }

    private fun source(
        id: String,
        configJson: String?,
    ) = SourceEntity(id = id, kind = "github", configJson = configJson, addedAt = 1L)

    private val connection = RepoConnection("o", "r", "main", "notes/index.json", isPublic = true)
    private val pendingConfig = ConnectionConfigCodec.encode(connection, pending = true)
    private val confirmedConfig = ConnectionConfigCodec.encode(connection, pending = false)

    @Test
    fun `RC-15 미확정 소스의 노트는 관찰에서 빠지고 센 개수에는 든다`() =
        runTest {
            dao.ensureSource(source("s0", confirmedConfig))
            dao.ensureSource(source("c1", pendingConfig))
            dao.upsertAll(listOf(row("s0", "a"), row("c1", "x"), row("c1", "y")))

            assertEquals(listOf("s0/a"), keys())
            assertEquals(2, dao.noteCount("c1"))
        }

    @Test
    fun `RC-15 확정 중 구독자는 새 노트와 옛 노트를 같이 보지 않는다`() =
        runTest {
            dao.ensureSource(source("s0", confirmedConfig))
            dao.ensureSource(source("c1", pendingConfig))
            dao.upsertAll(listOf(row("s0", "a"), row("c1", "x")))
            val seen = mutableListOf<Set<String>>()
            val collector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    dao.observeNotes(ConnectionConfigCodec.PENDING_MARKER).collect { rows ->
                        seen +=
                            rows.map { it.sourceId }.toSet()
                    }
                }

            dao.promote("c1", confirmedConfig, listOf("s0"))
            collector.cancel()

            assertEquals(setOf("c1"), seen.last())
            assertEquals(true, seen.none { "s0" in it && "c1" in it })
            assertEquals(listOf("c1"), dao.sources().map { it.id })
        }

    @Test
    fun `RC-16 소스를 지워도 학습 기록은 남는다`() =
        runTest {
            dao.ensureSource(source("s0", confirmedConfig))
            dao.upsertAll(listOf(row("s0", "a")))
            val log = database.studyLogDao()
            log.insertSession(
                ReadSessionEntity(
                    noteId = "s0/a",
                    category = "g",
                    startedAt = 1L,
                    endedAt = 2L,
                    activeMs = 1L,
                    maxPercent = 50,
                ),
            )

            dao.deleteSource("s0")

            assertEquals(emptyList<String>(), keys())
            assertEquals(1, log.observeSessions(0L, 10L).first().size)
        }

    @Test
    fun `RC-17 소스를 지우면 그 소스의 노트와 행만 사라진다`() =
        runTest {
            dao.ensureSource(source("s0", confirmedConfig))
            dao.ensureSource(SourceEntity(id = "s1", kind = "link", configJson = null, addedAt = 1L))
            dao.upsertAll(listOf(row("s0", "a"), row("s1", "clip")))

            dao.deleteSource("s0")

            assertEquals(listOf("s1/clip"), keys())
            assertEquals(listOf("s1"), dao.sources().map { it.id })
        }
}
