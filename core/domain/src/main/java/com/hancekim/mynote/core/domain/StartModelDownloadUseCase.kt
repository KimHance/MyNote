package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.QuizModelSource
import javax.inject.Inject

class StartModelDownloadUseCase @Inject constructor(
    private val source: QuizModelSource,
) {
    operator fun invoke() = source.start()
}
