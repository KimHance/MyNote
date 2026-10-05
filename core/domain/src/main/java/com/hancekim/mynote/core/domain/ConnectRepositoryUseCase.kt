package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.connect.ConnectProgress
import com.hancekim.mynote.core.domain.connect.ConnectVerification
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ConnectRepositoryUseCase @Inject constructor(
    private val connections: ConnectionRepository,
    private val syncPending: SyncPendingConnectionUseCase,
) {
    operator fun invoke(ready: ConnectVerification.Ready): Flow<ConnectProgress> =
        flow {
            emit(ConnectProgress.Syncing)
            emit(run(ready))
        }

    private suspend fun run(ready: ConnectVerification.Ready): ConnectProgress {
        val pending =
            withContext(NonCancellable) { connections.begin(ready.connection, ready.candidateToken) }
        val synced =
            try {
                syncPending(pending)
            } catch (cancelled: CancellationException) {
                connections.discard(pending)
                throw cancelled
            }
        val outcome =
            synced.getOrElse { cause ->
                connections.discard(pending)
                return ConnectProgress.Failed(cause as? SyncFailure ?: SyncFailure.Server(cause))
            }
        return withContext(NonCancellable) {
            try {
                connections.confirm(pending)
                ConnectProgress.Connected(outcome)
            } catch (failure: Exception) {
                settledAfter(failure, pending, outcome)
            }
        }
    }

    private suspend fun settledAfter(
        failure: Exception,
        pending: PendingConnection,
        outcome: SyncOutcome,
    ): ConnectProgress {
        val current = runCatching { connections.observeConnection().first() }.getOrNull()
        val confirmed =
            current is ConnectionState.Connected &&
                current.sourceId == pending.sourceId &&
                current.connection == pending.connection
        return if (confirmed) {
            ConnectProgress.Connected(
                outcome,
            )
        } else {
            ConnectProgress.Failed(SyncFailure.Server(failure))
        }
    }
}
