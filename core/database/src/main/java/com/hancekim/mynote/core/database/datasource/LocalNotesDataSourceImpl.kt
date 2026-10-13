package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.CachedNoteState
import com.hancekim.mynote.core.data.datasource.DeviceNoteEdit
import com.hancekim.mynote.core.data.datasource.DeviceNoteInsert
import com.hancekim.mynote.core.data.datasource.DeviceNoteRow
import com.hancekim.mynote.core.data.datasource.ListedNote
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.RederivedNote
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.data.datasource.StaleNote
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.entity.SourceEntity
import com.hancekim.mynote.core.database.entity.SourceNoteDerivedRow
import com.hancekim.mynote.core.database.entity.SourceNoteEntity
import com.hancekim.mynote.core.domain.model.NoteKey
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalNotesDataSourceImpl @Inject internal constructor(
    private val dao: NoteDao,
) : LocalNotesDataSource {
    override fun observeNotes(): Flow<List<ListedNote>> =
        dao.observeNotes(ConnectionConfigCodec.PENDING_MARKER).map { rows ->
            rows.map { ListedNote(it.key, it.sourceId, it.path, it.group, it.title, it.date, it.blocksJson) }
        }

    override suspend fun loadRaw(
        key: String,
        maxBytes: Int,
    ): String? = dao.loadRaw(key, maxBytes)

    override fun observeSources(): Flow<List<SourceRecord>> =
        dao.observeSources().map { rows -> rows.mapNotNull { it.toRecord() } }

    override suspend fun updateConfig(
        source: SourceId,
        configJson: String?,
    ) = dao.updateConfig(source.value, configJson)

    override suspend fun noteCount(source: SourceId): Int = dao.noteCount(source.value)

    override suspend fun deleteSource(source: SourceId) = dao.deleteSource(source.value)

    override suspend fun promote(
        source: SourceId,
        configJson: String,
        replace: List<SourceId>,
    ) = dao.promote(source.value, configJson, replace.map { it.value })

    override suspend fun ensureSource(record: SourceRecord) =
        dao.ensureSource(
            SourceEntity(
                id = record.id.value,
                kind = record.kind.toColumn(),
                configJson = record.configJson,
                addedAt = System.currentTimeMillis(),
            ),
        )

    override suspend fun sources(): List<SourceRecord> = dao.sources().mapNotNull { it.toRecord() }

    override suspend fun loadSourceState(source: SourceId): Map<String, CachedNoteState> =
        dao.loadSourceState(source.value).associate {
            it.docId to
                CachedNoteState(it.revision, it.hasRaw, it.firstSeenAt)
        }

    override suspend fun applySourceSync(
        source: SourceId,
        upserts: List<CachedNote>,
        deleteDocIds: List<String>,
    ): Boolean = dao.applySourceSync(source.value, upserts.map { it.toEntity() }, deleteDocIds)

    override suspend fun deleteOrphanNotes() = dao.deleteOrphanNotes()

    override suspend fun loadStale(
        parserVersion: Int,
        maxRawBytes: Int,
    ): List<StaleNote> = dao.loadStale(parserVersion, maxRawBytes).map { StaleNote(it.key, it.path, it.raw) }

    override suspend fun replaceDerived(rows: List<RederivedNote>) =
        dao.replaceDerived(rows.map { SourceNoteDerivedRow(it.key, it.blocksJson, it.excerpt, it.parserVersion) })

    override suspend fun insertDeviceNote(note: DeviceNoteInsert) =
        dao.insertDeviceNote(
            SourceEntity(
                id = SourceId.Device.value,
                kind = SourceKind.Local.toColumn(),
                configJson = null,
                addedAt = note.firstSeenAt,
            ),
            SourceNoteEntity(
                key = NoteKey(SourceId.Device, note.docId).value,
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
            ),
        )

    override suspend fun updateDeviceNote(edit: DeviceNoteEdit): Boolean =
        dao.updateDeviceNote(
            key = edit.key,
            group = edit.group,
            title = edit.title,
            raw = edit.raw,
            blocksJson = edit.blocksJson,
            excerpt = edit.excerpt,
            parserVersion = edit.parserVersion,
        ) > 0

    override suspend fun deleteDeviceNote(key: String): Boolean = dao.deleteDeviceNote(key) > 0

    override suspend fun loadDeviceNote(
        key: String,
        maxRawBytes: Int,
    ): DeviceNoteRow? =
        dao.loadDeviceNote(key, maxRawBytes)?.let { DeviceNoteRow(it.key, it.group, it.title, it.date, it.raw) }
}

private fun SourceEntity.toRecord(): SourceRecord? =
    kind.toSourceKind()?.let { SourceRecord(SourceId(id), it, configJson) }

private fun CachedNote.toEntity() =
    SourceNoteEntity(
        key = key,
        sourceId = sourceId,
        docId = docId,
        path = path,
        group = group,
        title = title,
        date = date,
        epochDay = epochDay,
        firstSeenAt = firstSeenAt,
        revision = revision,
        fetchedAt = fetchedAt,
        parserVersion = parserVersion,
        raw = raw,
        blocksJson = blocksJson,
        excerpt = excerpt,
    )
