package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.InsightStamp
import com.hancekim.mynote.core.domain.model.InsightWorkState
import kotlinx.coroutines.flow.Flow

interface InsightScheduler {
    val stamp: InsightStamp

    suspend fun schedule()

    fun cancel()

    fun state(): Flow<InsightWorkState>
}
