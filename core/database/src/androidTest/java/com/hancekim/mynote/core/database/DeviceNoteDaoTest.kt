package com.hancekim.mynote.core.database

import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.DeviceNoteEdit
import com.hancekim.mynote.core.data.datasource.DeviceNoteInsert
import com.hancekim.mynote.core.data.datasource.RederivedNote
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.datasource.LocalNotesDataSourceImpl
import com.hancekim.mynote.core.database.entity.SourceEntity
import com.hancekim.mynote.core.database.entity.SourceNoteEntity
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private val CONFIRMED = ConnectionConfigCodec.encode(RepoConnection("o", "r", "main", "notes/index.json", true))

private fun repoRow(
    docId: String,
    epochDay: Long = 20_000L,
    firstSeenAt: Long = 1L,
) = SourceNoteEntity(
    key = "s0/$docId",
    sourceId = "s0",
    docId = docId,
    path = "notes/g/$docId.md",
    group = "g",
    title = docId,
    date = "2024.10.04",
    epochDay = epochDay,
    firstSeenAt = firstSeenAt,
    revision = "r",
    fetchedAt = null,
    parserVersion = 2,
    raw = "---\ntitle: $docId\n---\n본문",
    blocksJson = "[]",
    excerpt = null,
)

private fun device(
    docId: String,
    epochDay: Long = 20_000L,
    firstSeenAt: Long = 10L,
    parserVersion: Int = 2,
) = DeviceNoteInsert(
    docId = docId,
    path = "$docId.md",
    group = "메모",
    title = "기기 $docId",
    date = "2024.10.04",
    epochDay = epochDay,
    firstSeenAt = firstSeenAt,
    parserVersion = parserVersion,
    raw = "---\n---\n옛 본문",
    blocksJson = """[{"type":"paragraph","spans":[{"text":"옛"}]}]""",
    excerpt = "옛",
)

@RunWith(AndroidJUnit4::class)
class DeviceNoteDaoTest {
    private lateinit var database: NotesDatabase
    private lateinit var dao: NoteDao
    private lateinit var local: LocalNotesDataSourceImpl

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    NotesDatabase::class.java,
                ).build()
        dao = database.noteDao()
        local = LocalNotesDataSourceImpl(dao)
    }

    @After
    fun close() {
        database.close()
    }

    private suspend fun keys(): List<String> = local.observeNotes().first().map { it.key }

    private suspend fun columns(
        key: String,
        names: String,
    ): List<String?> =
        database.useReaderConnection { connection ->
            connection.usePrepared("SELECT $names FROM source_notes WHERE note_key = ?") { statement ->
                statement.bindText(1, key)
                statement.step()
                (0 until statement.getColumnCount()).map { if (statement.isNull(it)) null else statement.getText(it) }
            }
        }

    private suspend fun fillPlaceholder(key: String) =
        database.useWriterConnection { connection ->
            connection.usePrepared("UPDATE source_notes SET summary = 'keep', tags_json = '[]' WHERE note_key = ?") {
                it.bindText(1, key)
                it.step()
            }
        }

    private suspend fun repository() {
        dao.ensureSource(SourceEntity("s0", "github", CONFIRMED, 1L))
        dao.upsertAll(listOf(repoRow("a"), repoRow("b")))
    }

    private fun edit(
        key: String,
        version: Int = 2,
    ) = DeviceNoteEdit(
        key = key,
        group = "새 그룹",
        title = "새 제목",
        raw = "---\n---\n새 본문",
        blocksJson = """[{"type":"paragraph","spans":[{"text":"새"}]}]""",
        excerpt = "새",
        parserVersion = version,
    )

    @Test
    fun `LN-5 기기 소스 행과 첫 노트는 한 트랜잭션에 들어간다`() =
        runTest {
            local.insertDeviceNote(device("x"))
            local.insertDeviceNote(device("y"))

            val sources = local.sources()
            assertEquals(listOf(SourceId.Device), sources.map { it.id })
            assertEquals(SourceKind.Local, sources.single().kind)
            assertNull(sources.single().configJson)

            local.deleteOrphanNotes()

            assertEquals(setOf("local/x", "local/y"), keys().toSet())
        }

    @Test
    fun `LN-4 같은 날짜 기기 노트는 나중에 만든 것이 먼저다`() =
        runTest {
            repository()
            local.insertDeviceNote(device("early", firstSeenAt = 10L))
            local.insertDeviceNote(device("late", firstSeenAt = 20L))
            local.insertDeviceNote(device("yesterday", epochDay = 19_999L, firstSeenAt = 30L))

            assertEquals(listOf("local/late", "local/early", "s0/a", "s0/b", "local/yesterday"), keys())
        }

    @Test
    fun `LN-9 고침 질의는 기기 노트만 바꾸고 날짜와 처음 본 시각을 지킨다`() =
        runTest {
            repository()
            local.insertDeviceNote(device("x"))
            fillPlaceholder("local/x")
            val keep = "date, epoch_day, first_seen_at, path, revision, fetched_at, summary, tags_json"
            val before = columns("local/x", keep)
            val repoBefore = columns("s0/a", "*")

            assertTrue(local.updateDeviceNote(edit("local/x")))
            assertFalse(local.updateDeviceNote(edit("s0/a")))

            assertEquals(before, columns("local/x", keep))
            assertEquals(
                listOf("새 그룹", "새 제목", "---\n---\n새 본문", "새", "2"),
                columns("local/x", "group_name, title, raw, excerpt, parser_version"),
            )
            assertEquals(repoBefore, columns("s0/a", "*"))
        }

    @Test
    fun `LN-9 고치기용 원문은 기기 노트만 읽는다`() =
        runTest {
            repository()
            local.insertDeviceNote(device("x"))

            val row = local.loadDeviceNote("local/x", 1_048_576)

            assertEquals("기기 x", row?.title)
            assertEquals("메모", row?.group)
            assertEquals("2024.10.04", row?.date)
            assertEquals("---\n---\n옛 본문", row?.raw)
            assertNull(local.loadDeviceNote("s0/a", 1_048_576))
            assertNull(local.loadDeviceNote("local/x", 4))
        }

    @Test
    fun `LN-9 재변환은 고침이 현재 판으로 쓴 행을 덮지 않는다`() =
        runTest {
            local.insertDeviceNote(device("x", parserVersion = 1))
            val stale = local.loadStale(parserVersion = 2, maxRawBytes = 1_048_576)
            assertEquals(listOf("local/x"), stale.map { it.key })

            local.updateDeviceNote(edit("local/x", version = 2))
            local.replaceDerived(stale.map { RederivedNote(it.key, """[{"type":"divider"}]""", "옛 해석", 2) })

            assertEquals(
                listOf("""[{"type":"paragraph","spans":[{"text":"새"}]}]""", "새", "2"),
                columns("local/x", "blocks_json, excerpt, parser_version"),
            )
        }

    @Test
    fun `LN-11 지움 질의는 기기 노트만 지운다`() =
        runTest {
            repository()
            local.insertDeviceNote(device("x"))

            assertFalse(local.deleteDeviceNote("s0/a"))
            assertTrue(local.deleteDeviceNote("local/x"))
            assertFalse(local.deleteDeviceNote("local/x"))

            assertEquals(listOf("s0/a", "s0/b"), keys())
            assertEquals(setOf("s0", SourceId.Device.value), local.sources().mapTo(HashSet()) { it.id.value })
        }

    @Test
    fun `LN-12 저장소 쪽 삭제 질의는 기기 노트 행을 지우지 않는다`() =
        runTest {
            repository()
            local.insertDeviceNote(device("a"))
            local.insertDeviceNote(device("b"))
            val pending =
                ConnectionConfigCodec.encode(
                    RepoConnection("x", "y", "", "notes/index.json", true),
                    pending = true,
                )
            dao.ensureSource(SourceEntity("c1", "github", pending, 2L))

            assertTrue(local.applySourceSync(SourceId.Legacy, emptyList(), listOf("a")))
            assertEquals(setOf("local/a", "local/b", "s0/b"), keys().toSet())

            local.promote(SourceId("c1"), CONFIRMED, listOf(SourceId.Legacy))
            assertEquals(setOf("local/a", "local/b"), keys().toSet())

            local.deleteSource(SourceId("c1"))
            local.deleteOrphanNotes()

            assertEquals(setOf("local/a", "local/b"), keys().toSet())
            assertEquals(2, local.noteCount(SourceId.Device))
            assertEquals(0, local.noteCount(SourceId.Legacy))
            assertEquals(emptyMap<String, Any>(), local.loadSourceState(SourceId.Legacy))
        }
}
