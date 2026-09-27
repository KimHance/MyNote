package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.QuizModelSource
import javax.inject.Inject

class DeleteModelUseCase @Inject constructor(
    private val source: QuizModelSource,
) {
    suspend operator fun invoke() = source.deleteModel()
}
