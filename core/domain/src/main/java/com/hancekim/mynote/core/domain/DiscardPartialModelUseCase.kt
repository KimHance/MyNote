package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.QuizModelSource
import javax.inject.Inject

class DiscardPartialModelUseCase @Inject constructor(
    private val source: QuizModelSource,
) {
    operator fun invoke() = source.discardPartial()
}
