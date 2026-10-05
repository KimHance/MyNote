package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.CachedNoteState
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.entity.SourceEntity
import com.hancekim.mynote.core.database.entity.SourceNoteEntity
import com.hancekim.mynote.core.domain.model.SourceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalNotesDataSourceImpl @Inject internal constructor(
    private val dao: NoteDao,
) : LocalNotesDataSource {
    override fun observeNotes(): Flow<List<CachedNote>> =
        dao.observeNotes(ConnectionConfigCodec.PENDING_MARKER).map { rows -> rows.map { it.toCached() } }

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
    ) = dao.applySourceSync(source.value, upserts.map { it.toEntity() }, deleteDocIds)

    override suspend fun loadStale(parserVersion: Int): List<CachedNote> =
        dao.loadStale(parserVersion).map {
            it.toCached()
        }

    override suspend fun replaceDerived(rows: List<CachedNote>) = dao.replaceDerived(rows.map { it.toEntity() })
}

private fun SourceEntity.toRecord(): SourceRecord? =
    kind.toSourceKind()?.let { SourceRecord(SourceId(id), it, configJson) }

private fun SourceNoteEntity.toCached() =
    CachedNote(
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
