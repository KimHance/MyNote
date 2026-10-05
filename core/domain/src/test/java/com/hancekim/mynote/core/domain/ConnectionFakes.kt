package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.TokenRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeTokens(
    initial: String? = null,
    private val applied: MutableStateFlow<String?>? = null,
) : TokenRepository {
    private val stored = MutableStateFlow(initial)
    val writes = mutableListOf<String?>()

    override suspend fun currentToken(): String? = stored.value

    override suspend fun saveToken(token: String?) {
        writes += token
        stored.value = token
        applied?.value = token
    }

    override fun observeToken(): Flow<String?> = stored
}

internal class FakeApplied(
    initial: String? = null,
) : AppliedTokenSource {
    val state = MutableStateFlow(initial)

    override fun applied(): Flow<String?> = state.asStateFlow()
}

internal class FakeConnections(
    private val tokens: FakeTokens,
    var connected: ConnectionState = ConnectionState.None,
    var seedBlocked: Boolean = false,
) : ConnectionRepository {
    val attempting = MutableStateFlow(false)
    val calls = mutableListOf<String>()
    var syncResult: Result<SyncOutcome> = Result.success(SyncOutcome(added = 1))
    var beginToken: String? = "unset"
    var firstSyncs = 0
    var firstSyncGate: CompletableDeferred<Unit>? = null
    var erasable = ErasureCount(0, 0)
    var countCalls = 0
    var confirmFailure: Exception? = null
    var confirmFailsAfterCommit = false
    var adoptionErasable: (Set<String>) -> ErasureCount = { ErasureCount(0, 0) }
    val adoptionKept = mutableListOf<Set<String>>()

    override fun observeConnection(): Flow<ConnectionState> = flowOf(connected)

    override fun observeAttempting(): Flow<Boolean> = attempting

    override suspend fun countErasable(): ErasureCount {
        countCalls++
        return erasable
    }

    override suspend fun countAdoptionErasable(keptDocIds: Set<String>): ErasureCount {
        adoptionKept += keptDocIds
        return adoptionErasable(keptDocIds)
    }

    override suspend fun isSeedEligible(): Boolean = connected is ConnectionState.None && !seedBlocked

    override suspend fun begin(
        connection: RepoConnection,
        token: String?,
    ): PendingConnection {
        calls += "begin"
        beginToken = token
        if (token != null) tokens.saveToken(token)
        attempting.value = true
        return PendingConnection(SourceId("c1"), connection, PendingConnection.Kind.New)
    }

    override suspend fun syncPending(pending: PendingConnection): Result<SyncOutcome> {
        calls += "syncPending"
        firstSyncs++
        firstSyncGate?.await()
        return syncResult
    }

    override suspend fun confirm(pending: PendingConnection) {
        calls += "confirm"
        if (!confirmFailsAfterCommit) confirmFailure?.let { throw it }
        connected = ConnectionState.Connected(pending.sourceId, pending.connection)
        attempting.value = false
        if (confirmFailsAfterCommit) confirmFailure?.let { throw it }
    }

    override suspend fun discard(pending: PendingConnection) {
        calls += "discard"
        attempting.value = false
    }

    override suspend fun disconnect(): Result<Unit> = Result.success(Unit)

    override suspend fun recover() {
        calls += "recover"
    }
}

internal class GatedNotes : NotesRepository {
    var syncs = 0
    var cancelled = 0
    var gate: CompletableDeferred<Unit>? = null

    override fun observeCategories(): Flow<List<Category>> = emptyFlow()

    override suspend fun sync(): Result<SyncOutcome> {
        syncs++
        try {
            gate?.await()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            cancelled++
            throw cancellation
        }
        return Result.success(SyncOutcome())
    }
}
