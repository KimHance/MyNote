package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.SyncOutcome
import kotlinx.coroutines.flow.Flow

/**
 * 노트 읽기 + 동기화 추상화(인터페이스는 domain 소유, DIP). 구현은 :core:data.
 * - [observeCategories] : 로컬 SSOT 관찰(Room Flow)
 * - [sync]              : 원격 index 와 로컬 해시 비교 후 reconcile. 변경 요약([SyncOutcome]) 반환.
 */
interface NotesRepository {
    fun observeCategories(): Flow<List<Category>>

    suspend fun sync(): Result<SyncOutcome>
}
