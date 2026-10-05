package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.domain.model.SourceId
import kotlinx.coroutines.flow.Flow

interface LocalNotesDataSource {
    fun observeNotes(): Flow<List<CachedNote>>

    suspend fun ensureSource(record: SourceRecord)

    suspend fun sources(): List<SourceRecord>

    suspend fun loadSourceState(source: SourceId): Map<String, CachedNoteState>

    suspend fun applySourceSync(
        source: SourceId,
        upserts: List<CachedNote>,
        deleteDocIds: List<String>,
    )

    suspend fun loadStale(parserVersion: Int): List<CachedNote>

    suspend fun replaceDerived(rows: List<CachedNote>)
}
