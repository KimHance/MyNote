package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.repository.QuizModelSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveModelStatusUseCase @Inject constructor(
    private val source: QuizModelSource,
) {
    operator fun invoke(): Flow<ModelStatus> = source.status()
}
