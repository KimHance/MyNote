package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.SyncOutcome
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun observeCategories(): Flow<List<Category>>

    suspend fun sync(): Result<SyncOutcome>
}
