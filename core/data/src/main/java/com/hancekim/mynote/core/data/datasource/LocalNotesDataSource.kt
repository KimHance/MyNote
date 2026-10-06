package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.domain.model.SourceId
import kotlinx.coroutines.flow.Flow

interface LocalNotesDataSource {
    fun observeNotes(): Flow<List<ListedNote>>

    /**
     * 노트 하나의 원문을 읽는다.
     *
     * @param maxBytes 읽을 원문의 UTF-8 크기 상한. 넘거나 원문이 없으면 null
     */
    suspend fun loadRaw(
        key: String,
        maxBytes: Int,
    ): String?

    fun observeSources(): Flow<List<SourceRecord>>

    suspend fun ensureSource(record: SourceRecord)

    suspend fun updateConfig(
        source: SourceId,
        configJson: String?,
    )

    suspend fun noteCount(source: SourceId): Int

    suspend fun deleteSource(source: SourceId)

    suspend fun promote(
        source: SourceId,
        configJson: String,
        replace: List<SourceId>,
    )

    suspend fun sources(): List<SourceRecord>

    suspend fun loadSourceState(source: SourceId): Map<String, CachedNoteState>

    suspend fun applySourceSync(
        source: SourceId,
        upserts: List<CachedNote>,
        deleteDocIds: List<String>,
    )

    /**
     * 판 번호가 낮고 원문이 있는 행을 읽는다.
     *
     * @param maxRawBytes 읽을 원문의 UTF-8 크기 상한. 넘는 행은 빠진다
     */
    suspend fun loadStale(
        parserVersion: Int,
        maxRawBytes: Int,
    ): List<StaleNote>

    suspend fun replaceDerived(rows: List<RederivedNote>)
}
