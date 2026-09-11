package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalNotesDataSourceImpl @Inject internal constructor(
    private val dao: NoteDao,
) : LocalNotesDataSource {
    override fun observeNotes(): Flow<List<CachedNote>> =
        dao.observeNotes().map { rows ->
            rows.map { it.toCached() }
        }

    override suspend fun loadHashes(): Map<String, String> {
        val rows = dao.loadHashes()
        return rows.associate { it.id to it.contentHash }
    }

    override suspend fun applySync(upserts: List<CachedNote>, deleteIds: List<String>) =
        dao.applySync(upserts.map { it.toEntity() }, deleteIds)
}

private fun NoteEntity.toCached() =
    CachedNote(
        id = id,
        group = group,
        title = title,
        date = date,
        contentHash = contentHash,
        blocksJson = blocksJson,
    )

private fun CachedNote.toEntity() =
    NoteEntity(
        id = id,
        group = group,
        title = title,
        date = date,
        contentHash = contentHash,
        blocksJson = blocksJson,
    )
