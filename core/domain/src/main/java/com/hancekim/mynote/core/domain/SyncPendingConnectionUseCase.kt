package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import com.hancekim.mynote.core.domain.repository.TokenRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class SyncPendingConnectionUseCase @Inject constructor(
    private val connections: ConnectionRepository,
    private val tokens: TokenRepository,
    private val applied: AppliedTokenSource,
) {
    suspend operator fun invoke(pending: PendingConnection): Result<SyncOutcome> {
        val stored = tokens.currentToken()
        val settled = withTimeoutOrNull(APPLY_TIMEOUT_MS) { applied.applied().first { it == stored } }
        if (settled == null && stored != null) {
            return Result.failure(SyncFailure.Server(IllegalStateException("저장한 토큰이 적용되지 않았습니다")))
        }
        return connections.syncPending(pending)
    }

    private companion object {
        const val APPLY_TIMEOUT_MS = 5_000L
    }
}
