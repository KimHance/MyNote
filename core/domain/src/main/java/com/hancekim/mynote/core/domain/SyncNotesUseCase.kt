package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.NotesRepository
import javax.inject.Inject

/** 원격 index 기준으로 로컬 캐시를 reconcile 하고 변경 요약을 돌려준다(앱 startup/수동 새로고침에서 호출). */
class SyncNotesUseCase @Inject constructor(
    private val repository: NotesRepository,
) {
    suspend operator fun invoke(): Result<SyncOutcome> = repository.sync()
}
