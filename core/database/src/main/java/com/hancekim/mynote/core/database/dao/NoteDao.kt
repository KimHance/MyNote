package com.hancekim.mynote.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.hancekim.mynote.core.database.entity.NoteEntity
import com.hancekim.mynote.core.database.entity.NoteHashRow
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY date DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT id, contentHash FROM notes")
    suspend fun loadHashes(): List<NoteHashRow>

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Transaction
    suspend fun applySync(upserts: List<NoteEntity>, deleteIds: List<String>) {
        if (upserts.isNotEmpty()) upsertAll(upserts)
        if (deleteIds.isNotEmpty()) deleteByIds(deleteIds)
    }
}
