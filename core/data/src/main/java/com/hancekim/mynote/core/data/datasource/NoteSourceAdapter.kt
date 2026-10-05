package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind

interface NoteSourceAdapter {
    val kind: SourceKind

    suspend fun list(source: SourceRecord): List<SourceEntry>

    suspend fun fetch(
        source: SourceRecord,
        entry: SourceEntry,
    ): String
}

data class SourceRecord(
    val id: SourceId,
    val kind: SourceKind,
    val configJson: String?,
)

data class SourceEntry(
    val docId: String,
    val path: String,
    val revision: String,
    val category: String,
    val title: String?,
    val date: String?,
)
