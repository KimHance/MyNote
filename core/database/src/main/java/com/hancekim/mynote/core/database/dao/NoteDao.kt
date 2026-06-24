package com.hancekim.mynote.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.hancekim.mynote.core.database.entity.NoteEntity
import com.hancekim.mynote.core.database.entity.NoteHashRow
import kotlinx.coroutines.flow.Flow

/** 관찰은 Flow, 변경은 suspend. */
@Dao
internal interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY date DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT id, contentHash FROM notes")
    suspend fun loadHashes(): List<NoteHashRow>

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
