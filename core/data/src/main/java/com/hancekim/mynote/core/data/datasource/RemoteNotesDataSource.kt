package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.data.dto.IndexEntryDto
import com.hancekim.mynote.core.data.dto.NoteDto

interface RemoteNotesDataSource {
    suspend fun getIndex(): List<IndexEntryDto>

    suspend fun getNote(path: String): NoteDto
}
