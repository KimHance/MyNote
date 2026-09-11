package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.NotesRepository
import javax.inject.Inject

class SyncNotesUseCase @Inject constructor(
    private val repository: NotesRepository,
) {
    suspend operator fun invoke(): Result<SyncOutcome> = repository.sync()
}
