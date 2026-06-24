package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.RemoteNotesDataSource
import com.hancekim.mynote.core.data.dto.toCategories
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
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
        local.observeNotes().map { it.toCategories(json) }

    override suspend fun sync(): Result<Unit> = runCatching {
        val index = remote.getIndex()
        val localHashes = local.loadHashes()
        val indexIds = index.map { it.id }.toSet()

        // 신규/변경: 해시가 다른 항목만 본문까지 받아 갱신(같으면 skip → 불필요 fetch 차단)
        for (entry in index) {
            if (localHashes[entry.id] == entry.hash) continue
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
        }

        // 삭제 동기화: index 에 없는 로컬 노트 제거
        val toDelete = localHashes.keys - indexIds
        if (toDelete.isNotEmpty()) local.deleteByIds(toDelete.toList())
    }
}
