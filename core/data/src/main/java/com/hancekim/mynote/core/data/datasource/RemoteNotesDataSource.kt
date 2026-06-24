package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.data.dto.IndexEntryDto
import com.hancekim.mynote.core.data.dto.NoteDto

/**
 * 원격 노트 소스 추상화(인터페이스는 data 소유, 구현은 :core:network).
 * - [getIndex] : notes/index.json 카탈로그(메타 + path + hash)
 * - [getNote]  : 개별 노트 본문(path 로 GET)
 */
interface RemoteNotesDataSource {
    suspend fun getIndex(): List<IndexEntryDto>
    suspend fun getNote(path: String): NoteDto
}
