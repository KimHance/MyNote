package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SyncOutcome
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    fun observeConnection(): Flow<ConnectionState>

    fun observeAttempting(): Flow<Boolean>

    suspend fun countErasable(): ErasureCount

    suspend fun isSeedEligible(): Boolean

    suspend fun begin(
        connection: RepoConnection,
        token: String?,
    ): PendingConnection

    suspend fun syncPending(pending: PendingConnection): Result<SyncOutcome>

    suspend fun confirm(pending: PendingConnection)

    suspend fun discard(pending: PendingConnection)

    suspend fun disconnect(): Result<Unit>

    suspend fun recover()
}
