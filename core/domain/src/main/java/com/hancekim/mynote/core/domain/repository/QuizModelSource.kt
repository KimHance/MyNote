package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.ModelStatus
import kotlinx.coroutines.flow.Flow

interface QuizModelSource {
    fun status(): Flow<ModelStatus>

    fun start()

    fun cancel()

    fun discardPartial()

    fun deleteModel()
}
