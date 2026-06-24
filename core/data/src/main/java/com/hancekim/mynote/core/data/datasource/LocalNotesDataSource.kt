package com.hancekim.mynote.core.data.datasource

import kotlinx.coroutines.flow.Flow

/**
 * 로컬 캐시(Room) 추상화(인터페이스는 data 소유, 구현은 :core:database).
 * 관찰은 [observeNotes] Flow(SSOT), reconcile 비교는 [loadHashes] 경량 projection.
 */
interface LocalNotesDataSource {
    fun observeNotes(): Flow<List<CachedNote>>
    suspend fun loadHashes(): Map<String, String> // id -> contentHash
    suspend fun upsert(note: CachedNote)
    suspend fun deleteByIds(ids: List<String>)
}
