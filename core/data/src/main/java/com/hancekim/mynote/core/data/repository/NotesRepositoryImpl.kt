package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.common.result.runCatchingCancellable
import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.RemoteNotesDataSource
import com.hancekim.mynote.core.data.dto.IndexEntryDto
import com.hancekim.mynote.core.data.dto.toCategories
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class NotesRepositoryImpl @Inject constructor(
    private val remote: RemoteNotesDataSource,
    private val local: LocalNotesDataSource,
    private val json: Json,
) : NotesRepository {
    override fun observeCategories(): Flow<List<Category>> =
        local
            .observeNotes()
            .map { it.toCategories(json) }
            .flowOn(Dispatchers.Default)

    override suspend fun sync(): Result<SyncOutcome> =
        withContext(Dispatchers.Default) {
            runCatchingCancellable {
                val index = remote.getIndex()
                val localHashes = local.loadHashes()

                val fetched = fetchAll(index.filter { localHashes[it.id] != it.hash })
                val deleteIds = (localHashes.keys - index.mapTo(mutableSetOf()) { it.id }).toList()

                if (fetched.isNotEmpty() || deleteIds.isNotEmpty()) {
                    local.applySync(fetched, deleteIds)
                }

                val added = fetched.count { localHashes[it.id] == null }
                SyncOutcome(added, fetched.size - added, deleteIds)
            }
        }

    private suspend fun fetchAll(entries: List<IndexEntryDto>): List<CachedNote> =
        entries
            .asFlow()
            .flatMapMerge { entry -> flow { emit(fetch(entry)) } }
            .toList()

    private suspend fun fetch(entry: IndexEntryDto): CachedNote {
        val note = remote.getNote(entry.path)
        return CachedNote(
            id = entry.id,
            group = entry.group,
            title = entry.title,
            date = entry.date,
            contentHash = entry.hash,
            blocksJson = json.encodeToString(note.blocks),
        )
    }
}
