package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.RemoteNotesDataSource
import com.hancekim.mynote.core.data.dto.toCategories
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * SSOT 는 Room. [observeCategories] 는 로컬 관찰, [sync] 는 원격 index 기준 reconcile.
 * 두 DataSource 구현체(network/database)는 모른다 — Hilt 가 :app 에서 바인딩.
 */
class NotesRepositoryImpl @Inject constructor(
    private val remote: RemoteNotesDataSource,
    private val local: LocalNotesDataSource,
    private val json: Json,
) : NotesRepository {

    override fun observeCategories(): Flow<List<Category>> =
        local.observeNotes()
            .map { it.toCategories(json) }
            .flowOn(Dispatchers.Default)

    override suspend fun sync(): Result<Unit> {
        val index = runCatching { remote.getIndex() }.getOrElse { return Result.failure(it) }
        val localHashes = local.loadHashes()
        val indexIds = index.map { it.id }.toSet()

        val errors = mutableListOf<Throwable>()
        for (entry in index) {
            if (localHashes[entry.id] == entry.hash) continue
            // 한 노트 실패(네트워크/미지 block type 역직렬화 등)가 전체 sync 를 끊지 않게 격리
            runCatching {
                val note = remote.getNote(entry.path)
                local.upsert(
                    CachedNote(
                        id = entry.id,
                        group = entry.group,
                        title = entry.title,
                        date = entry.date,
                        contentHash = entry.hash,
                        blocksJson = json.encodeToString(note.blocks),
                    ),
                )
            }.onFailure { errors += it }
        }

        // 삭제 동기화는 fetch 실패와 무관하게 항상 적용(유령 노트 방지)
        val toDelete = localHashes.keys - indexIds
        if (toDelete.isNotEmpty()) local.deleteByIds(toDelete.toList())

        return if (errors.isEmpty()) Result.success(Unit) else Result.failure(errors.first())
    }
}
