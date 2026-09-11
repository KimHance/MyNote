package com.hancekim.mynote.core.data.datasource

import kotlinx.coroutines.flow.Flow

interface LocalNotesDataSource {
    fun observeNotes(): Flow<List<CachedNote>>

    suspend fun loadHashes(): Map<String, String>

    suspend fun applySync(upserts: List<CachedNote>, deleteIds: List<String>)
}
