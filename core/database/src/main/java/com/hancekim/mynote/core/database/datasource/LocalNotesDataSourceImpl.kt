package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room DAO 를 data 의 [LocalNotesDataSource] 로 어댑트. Entity 는 이 모듈 밖으로 안 나간다. */
class LocalNotesDataSourceImpl @Inject internal constructor(
    private val dao: NoteDao,
) : LocalNotesDataSource {

    override fun observeNotes(): Flow<List<CachedNote>> =
        dao.observeNotes().map { rows -> rows.map { it.toCached() } }

    override suspend fun loadHashes(): Map<String, String> =
        dao.loadHashes().associate { it.id to it.contentHash }

    override suspend fun upsert(note: CachedNote) = dao.upsert(note.toEntity())

    override suspend fun deleteByIds(ids: List<String>) = dao.deleteByIds(ids)
}

private fun NoteEntity.toCached() = CachedNote(
    id = id, group = group, title = title, date = date,
    contentHash = contentHash, blocksJson = blocksJson,
)

private fun CachedNote.toEntity() = NoteEntity(
    id = id, group = group, title = title, date = date,
    contentHash = contentHash, blocksJson = blocksJson,
)
