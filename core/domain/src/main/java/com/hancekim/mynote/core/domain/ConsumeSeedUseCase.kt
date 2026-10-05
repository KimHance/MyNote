package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.ConnectionSeed
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SeedOutcome
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import com.hancekim.mynote.core.domain.repository.TokenRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ConsumeSeedUseCase @Inject constructor(
    private val seed: ConnectionSeed?,
    private val connections: ConnectionRepository,
    private val tokens: TokenRepository,
    private val syncPending: SyncPendingConnectionUseCase,
) {
    suspend operator fun invoke(): SeedOutcome {
        val seed = seed?.takeIf { it.owner.isNotBlank() && it.repo.isNotBlank() } ?: return SeedOutcome.NotApplicable
        if (!connections.isSeedEligible()) return SeedOutcome.NotApplicable

        val connection =
            RepoConnection(
                owner = seed.owner,
                repo = seed.repo,
                branch = seed.branch,
                indexPath = RepoConnection.DEFAULT_INDEX_PATH,
                isPublic = seed.token.isNullOrBlank(),
            )
        val tokenToStore = seed.token?.ifBlank { null }?.takeIf { tokens.currentToken() == null }
        val pending = connections.begin(connection, tokenToStore)
        return try {
            syncPending(pending).fold(
                onSuccess = { outcome ->
                    connections.confirm(pending)
                    SeedOutcome.Connected(outcome)
                },
                onFailure = { cause ->
                    connections.discard(pending)
                    SeedOutcome.Failed(cause)
                },
            )
        } catch (cancelled: CancellationException) {
            connections.discard(pending)
            throw cancelled
        }
    }
}
