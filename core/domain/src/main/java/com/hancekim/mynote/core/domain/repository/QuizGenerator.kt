package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.QuizQuestion

interface QuizGenerator {
    suspend fun isAvailable(): Boolean

    suspend fun generate(
        noteTitle: String,
        noteText: String,
        count: Int,
    ): Result<List<QuizQuestion>>
}
