package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.AttemptRecord
import com.hancekim.mynote.core.data.datasource.BookmarkStore
import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.CachedNoteState
import com.hancekim.mynote.core.data.datasource.ConnectionStateStore
import com.hancekim.mynote.core.data.datasource.DeviceNoteEdit
import com.hancekim.mynote.core.data.datasource.DeviceNoteInsert
import com.hancekim.mynote.core.data.datasource.DeviceNoteRow
import com.hancekim.mynote.core.data.datasource.ListedNote
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.NoteSourceAdapter
import com.hancekim.mynote.core.data.datasource.RederivedNote
import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.data.datasource.StaleNote
import com.hancekim.mynote.core.data.datasource.TokenStore
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.format.PARSER_VERSION
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import java.util.Collections

internal val CONNECTION = RepoConnection("o", "r", "main", "notes/index.json", isPublic = true)
internal val CONNECTED_S0 = SourceRecord(SourceId.Legacy, SourceKind.GitHub, ConnectionConfigCodec.encode(CONNECTION))

internal val S0 = SourceId.Legacy
internal val LINK = SourceId("s1")

internal fun entry(
    id: String,
    revision: String,
    date: String = "2026.01.01",
) = SourceEntry(
    docId = id,
    path = "notes/android/$id.json",
    revision = revision,
    category = "android",
    title = id,
    date = date,
)

internal fun body(text: String) =
    """{"id":"x","title":"x","date":"x","blocks":[{"type":"paragraph","spans":[{"text":"$text"}]}]}"""

internal fun cached(
    id: String,
    revision: String?,
    source: SourceId = S0,
    raw: String? = body(id),
    parserVersion: Int = PARSER_VERSION,
    blocksJson: String = "[]",
    firstSeenAt: Long = 1L,
) = CachedNote(
    key = "${source.value}/$id",
    sourceId = source.value,
    docId = id,
    path = null,
    group = "android",
    title = id,
    date = "2026.01.01",
    epochDay = null,
    firstSeenAt = firstSeenAt,
    revision = revision,
    fetchedAt = null,
    parserVersion = parserVersion,
    raw = raw,
    blocksJson = blocksJson,
    excerpt = null,
)

internal class FakeAdapter(
    var entries: List<SourceEntry>,
    val docs: Map<String, String> = emptyMap(),
    internal var listError: Throwable? = null,
    internal val failOn: String? = null,
    internal val gate: Int = 0,
    override val kind: SourceKind = SourceKind.GitHub,
    private val onFetch: (suspend () -> Unit)? = null,
) : NoteSourceAdapter {
    val fetched: MutableList<String> = Collections.synchronizedList(mutableListOf())
    var lists = 0
    val listed: MutableList<SourceRecord> = Collections.synchronizedList(mutableListOf())
    internal val arrived = MutableStateFlow(0)

    override suspend fun list(source: SourceRecord): List<SourceEntry> {
        lists++
        listed += source
        listError?.let { throw it }
        return entries
    }

    override suspend fun fetch(
        source: SourceRecord,
        entry: SourceEntry,
    ): String {
        fetched += entry.docId
        onFetch?.invoke()
        if (gate > 0) {
            arrived.update { it + 1 }
            withTimeout(2_000) { arrived.first { it >= gate } }
        }
        if (entry.docId == failOn) error("fetch 실패: ${entry.docId}")
        return docs[entry.docId] ?: body(entry.docId)
    }
}

internal class FakeLocal(
    initial: List<CachedNote> = emptyList(),
    sources: List<SourceRecord> = emptyList(),
    connected: Boolean = true,
    internal val writeError: Throwable? = null,
    private val deleteError: Throwable? = null,
) : LocalNotesDataSource {
    var promoteError: Throwable? = null
    val store = MutableStateFlow(initial.associateBy { it.key })
    private val rows =
        MutableStateFlow(((if (connected) listOf(CONNECTED_S0) else emptyList()) + sources).associateBy { it.id })
    val sourceRows: Map<SourceId, SourceRecord> get() = rows.value
    var applyCalls = 0
    var promoteCalls = 0
    var onLoadStale: (suspend () -> Unit)? = null
    val rawReads: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private val visible = MutableStateFlow<List<CachedNote>>(emptyList())

    init {
        publish()
    }

    private fun publish() {
        val hidden =
            rows.value.values
                .filter {
                    ConnectionConfigCodec.decode(it.configJson)?.pending == true
                }.map { it.id.value }
        visible.value = store.value.values.filter { it.sourceId !in hidden }
    }

    override fun observeNotes(): Flow<List<ListedNote>> =
        visible.map { notes ->
            notes.map { ListedNote(it.key, it.sourceId, it.path, it.group, it.title, it.date, it.blocksJson) }
        }

    override suspend fun loadRaw(
        key: String,
        maxBytes: Int,
    ): String? {
        rawReads += key
        return store.value[key]?.raw?.takeIf { it.toByteArray().size <= maxBytes }
    }

    override fun observeSources(): Flow<List<SourceRecord>> = rows.map { it.values.sortedBy { r -> r.id.value } }

    override suspend fun ensureSource(record: SourceRecord) {
        rows.update { if (record.id in it) it else it + (record.id to record) }
        publish()
    }

    override suspend fun updateConfig(
        source: SourceId,
        configJson: String?,
    ) {
        rows.update { current ->
            current[source]?.let { current + (source to it.copy(configJson = configJson)) }
                ?: current
        }
        publish()
    }

    override suspend fun noteCount(source: SourceId): Int = store.value.values.count { it.sourceId == source.value }

    override suspend fun deleteSource(source: SourceId) {
        deleteError?.let { throw it }
        store.update { current -> current.filterValues { it.sourceId != source.value } }
        rows.update { it - source }
        publish()
    }

    override suspend fun promote(
        source: SourceId,
        configJson: String,
        replace: List<SourceId>,
    ) {
        promoteCalls++
        promoteError?.let { throw it }
        val replaced = replace.map { it.value }.toSet()
        store.update { current -> current.filterValues { it.sourceId !in replaced } }
        rows.update { current ->
            val kept = current - replace.toSet()
            kept[source]?.let { kept + (source to it.copy(configJson = configJson)) } ?: kept
        }
        publish()
    }

    override suspend fun sources(): List<SourceRecord> = rows.value.values.sortedBy { it.id.value }

    override suspend fun loadSourceState(source: SourceId): Map<String, CachedNoteState> =
        store.value.values
            .filter { it.sourceId == source.value }
            .associate { it.docId to CachedNoteState(it.revision, it.raw != null, it.firstSeenAt) }

    override suspend fun applySourceSync(
        source: SourceId,
        upserts: List<CachedNote>,
        deleteDocIds: List<String>,
    ): Boolean {
        applyCalls++
        writeError?.let { throw it }
        if (source !in rows.value) return false
        store.update { current ->
            current + upserts.associateBy { it.key } - deleteDocIds.map { "${source.value}/$it" }.toSet()
        }
        publish()
        return true
    }

    override suspend fun deleteOrphanNotes() {
        store.update { current -> current.filterValues { row -> rows.value.keys.any { it.value == row.sourceId } } }
        publish()
    }

    override suspend fun loadStale(
        parserVersion: Int,
        maxRawBytes: Int,
    ): List<StaleNote> {
        val stale =
            store.value.values.mapNotNull { row ->
                row.raw
                    ?.takeIf { row.parserVersion < parserVersion && it.toByteArray().size <= maxRawBytes }
                    ?.let { StaleNote(row.key, row.path, it) }
            }
        onLoadStale?.invoke()
        return stale
    }

    override suspend fun replaceDerived(rows: List<RederivedNote>) {
        writeError?.let { throw it }
        store.update { current ->
            current +
                rows.mapNotNull { row ->
                    current[row.key]?.takeIf { it.parserVersion < row.parserVersion }?.let {
                        row.key to
                            it.copy(
                                blocksJson = row.blocksJson,
                                excerpt = row.excerpt,
                                parserVersion = row.parserVersion,
                            )
                    }
                }
        }
        publish()
    }

    override suspend fun insertDeviceNote(note: DeviceNoteInsert) {
        writeError?.let { throw it }
        val device = SourceRecord(SourceId.Device, SourceKind.Local, null)
        rows.update { if (device.id in it) it else it + (device.id to device) }
        val key = "${SourceId.Device.value}/${note.docId}"
        check(key !in store.value) { "duplicate key: $key" }
        store.update {
            it +
                (
                    key to
                        CachedNote(
                            key = key,
                            sourceId = SourceId.Device.value,
                            docId = note.docId,
                            path = note.path,
                            group = note.group,
                            title = note.title,
                            date = note.date,
                            epochDay = note.epochDay,
                            firstSeenAt = note.firstSeenAt,
                            revision = null,
                            fetchedAt = null,
                            parserVersion = note.parserVersion,
                            raw = note.raw,
                            blocksJson = note.blocksJson,
                            excerpt = note.excerpt,
                        )
                )
        }
        publish()
    }

    override suspend fun updateDeviceNote(edit: DeviceNoteEdit): Boolean {
        writeError?.let { throw it }
        val row = store.value[edit.key]?.takeIf { it.sourceId == SourceId.Device.value } ?: return false
        store.update {
            it +
                (
                    edit.key to
                        row.copy(
                            group = edit.group,
                            title = edit.title,
                            raw = edit.raw,
                            blocksJson = edit.blocksJson,
                            excerpt = edit.excerpt,
                            parserVersion = edit.parserVersion,
                        )
                )
        }
        publish()
        return true
    }

    override suspend fun deleteDeviceNote(key: String): Boolean {
        deleteError?.let { throw it }
        if (store.value[key]?.sourceId != SourceId.Device.value) return false
        store.update { it - key }
        publish()
        return true
    }

    override suspend fun loadDeviceNote(
        key: String,
        maxRawBytes: Int,
    ): DeviceNoteRow? =
        store.value[key]
            ?.takeIf { it.sourceId == SourceId.Device.value }
            ?.let { row ->
                row.raw
                    ?.takeIf { it.toByteArray().size <= maxRawBytes }
                    ?.let { DeviceNoteRow(row.key, row.group, row.title, row.date, it) }
            }
}

internal class FakeTokenStore(
    val initial: String? = null,
) : TokenStore {
    private val state = MutableStateFlow(initial)
    var mirror: MutableStateFlow<String?>? = null
    val writes = mutableListOf<String?>()

    override suspend fun currentToken(): String? = state.value

    override suspend fun saveToken(token: String?) {
        writes += token
        state.value = token
        mirror?.value = token
    }

    override fun observeToken(): Flow<String?> = state
}

internal class FakeBookmarkStore(
    initial: Map<String, Long> = emptyMap(),
) : BookmarkStore {
    val entries = MutableStateFlow(initial)
    var onRemoveByPrefix: (suspend () -> Unit)? = null

    override fun observe(): Flow<Map<String, Long>> = entries

    override suspend fun put(
        noteId: String,
        addedAt: Long,
    ) = entries.update { it + (noteId to addedAt) }

    override suspend fun remove(noteIds: Collection<String>) = entries.update { it - noteIds.toSet() }

    override suspend fun countByPrefix(prefix: String): Int = entries.value.keys.count { it.startsWith(prefix) }

    override suspend fun removeByPrefix(prefix: String) {
        onRemoveByPrefix?.invoke()
        entries.update { current -> current.filterKeys { !it.startsWith(prefix) } }
    }
}

internal class FakeStateStore : ConnectionStateStore {
    var used: Set<String> = emptySet()
    var attemptRecord: AttemptRecord? = null
    var disconnect: String? = null
    var seedBlocked = false
    var sweep: Set<String> = emptySet()

    override suspend fun usedSourceIds(): Set<String> = used

    override suspend fun addUsedSourceId(id: String) {
        used = used + id
    }

    override suspend fun attempt(): AttemptRecord? = attemptRecord

    override suspend fun saveAttempt(record: AttemptRecord) {
        attemptRecord = record
    }

    override suspend fun clearAttempt() {
        attemptRecord = null
    }

    override suspend fun disconnectMarker(): String? = disconnect

    override suspend fun markDisconnect(sourceId: String) {
        disconnect = sourceId
        seedBlocked = true
    }

    override suspend fun clearDisconnectMarker() {
        disconnect = null
    }

    override suspend fun clearDisconnectAndSeedBlock() {
        disconnect = null
        seedBlocked = false
    }

    override suspend fun isSeedBlocked(): Boolean = seedBlocked

    override suspend fun clearSeedBlock() {
        seedBlocked = false
    }

    override suspend fun bookmarkSweep(): Set<String> = sweep

    override suspend fun addBookmarkSweep(ids: Collection<String>) {
        sweep = sweep + ids
    }

    override suspend fun clearBookmarkSweep() {
        sweep = emptySet()
    }
}

internal class FakeAppliedToken(
    initial: String? = null,
) : AppliedTokenSource {
    val state = MutableStateFlow(initial)

    override fun applied(): Flow<String?> = state
}

internal class CountingNotes : NotesRepository {
    var syncs = 0

    override fun observeCategories(): Flow<List<Category>> = emptyFlow()

    override suspend fun sync(): Result<SyncOutcome> {
        syncs++
        return Result.success(SyncOutcome())
    }
}
