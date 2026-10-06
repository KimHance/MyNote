package com.hancekim.mynote.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.hancekim.mynote.core.database.entity.SourceEntity
import com.hancekim.mynote.core.database.entity.SourceNoteDerivedRow
import com.hancekim.mynote.core.database.entity.SourceNoteEntity
import com.hancekim.mynote.core.database.entity.SourceNoteListRow
import com.hancekim.mynote.core.database.entity.SourceNoteStaleRow
import com.hancekim.mynote.core.database.entity.SourceNoteStateRow
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NoteDao {
    @Query(
        "SELECT note_key, source_id, path, group_name, title, date, blocks_json FROM source_notes " +
            "WHERE source_id NOT IN (SELECT id FROM sources WHERE config_json LIKE '%' || :pendingMarker || '%') " +
            "ORDER BY epoch_day IS NULL, epoch_day DESC, first_seen_at DESC, title",
    )
    fun observeNotes(pendingMarker: String): Flow<List<SourceNoteListRow>>

    @Query("SELECT raw FROM source_notes WHERE note_key = :key AND length(CAST(raw AS BLOB)) <= :maxBytes")
    suspend fun loadRaw(
        key: String,
        maxBytes: Int,
    ): String?

    @Query("SELECT * FROM sources ORDER BY id")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT COUNT(*) FROM source_notes WHERE source_id = :sourceId")
    suspend fun noteCount(sourceId: String): Int

    @Query("UPDATE sources SET config_json = :configJson WHERE id = :sourceId")
    suspend fun updateConfig(
        sourceId: String,
        configJson: String?,
    )

    @Query("DELETE FROM source_notes WHERE source_id = :sourceId")
    suspend fun deleteNotesOf(sourceId: String)

    @Query("DELETE FROM sources WHERE id = :sourceId")
    suspend fun deleteSourceRow(sourceId: String)

    @Transaction
    suspend fun deleteSource(sourceId: String) {
        deleteNotesOf(sourceId)
        deleteSourceRow(sourceId)
    }

    @Transaction
    suspend fun promote(
        sourceId: String,
        configJson: String,
        replaceIds: List<String>,
    ) {
        replaceIds.forEach { deleteSource(it) }
        updateConfig(sourceId, configJson)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureSource(source: SourceEntity)

    @Query("SELECT * FROM sources ORDER BY id")
    suspend fun sources(): List<SourceEntity>

    @Query(
        "SELECT doc_id, revision, raw IS NOT NULL AS has_raw, first_seen_at " +
            "FROM source_notes WHERE source_id = :sourceId",
    )
    suspend fun loadSourceState(sourceId: String): List<SourceNoteStateRow>

    @Upsert
    suspend fun upsertAll(notes: List<SourceNoteEntity>)

    @Query("DELETE FROM source_notes WHERE source_id = :sourceId AND doc_id IN (:docIds)")
    suspend fun deleteDocs(
        sourceId: String,
        docIds: List<String>,
    )

    @Transaction
    suspend fun applySourceSync(
        sourceId: String,
        upserts: List<SourceNoteEntity>,
        deleteDocIds: List<String>,
    ) {
        if (upserts.isNotEmpty()) upsertAll(upserts)
        deleteDocIds.chunked(DELETE_CHUNK).forEach { deleteDocs(sourceId, it) }
    }

    @Query(
        "SELECT note_key, path, raw FROM source_notes " +
            "WHERE parser_version < :parserVersion AND raw IS NOT NULL AND length(CAST(raw AS BLOB)) <= :maxRawBytes",
    )
    suspend fun loadStale(
        parserVersion: Int,
        maxRawBytes: Int,
    ): List<SourceNoteStaleRow>

    @Query(
        "UPDATE source_notes SET blocks_json = :blocksJson, excerpt = :excerpt, parser_version = :parserVersion " +
            "WHERE note_key = :key",
    )
    suspend fun updateDerived(
        key: String,
        blocksJson: String,
        excerpt: String?,
        parserVersion: Int,
    )

    @Transaction
    suspend fun replaceDerived(rows: List<SourceNoteDerivedRow>) {
        rows.forEach { updateDerived(it.key, it.blocksJson, it.excerpt, it.parserVersion) }
    }

    companion object {
        const val DELETE_CHUNK = 900
    }
}
