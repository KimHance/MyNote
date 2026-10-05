package com.hancekim.mynote.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.hancekim.mynote.core.database.entity.SourceEntity
import com.hancekim.mynote.core.database.entity.SourceNoteEntity
import com.hancekim.mynote.core.database.entity.SourceNoteStateRow
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NoteDao {
    @Query(
        "SELECT * FROM source_notes " +
            "ORDER BY epoch_day IS NULL, epoch_day DESC, first_seen_at DESC, title",
    )
    fun observeNotes(): Flow<List<SourceNoteEntity>>

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

    @Query("SELECT * FROM source_notes WHERE parser_version < :parserVersion AND raw IS NOT NULL")
    suspend fun loadStale(parserVersion: Int): List<SourceNoteEntity>

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
    suspend fun replaceDerived(rows: List<SourceNoteEntity>) {
        rows.forEach { updateDerived(it.key, it.blocksJson, it.excerpt, it.parserVersion) }
    }

    companion object {
        const val DELETE_CHUNK = 900
    }
}
