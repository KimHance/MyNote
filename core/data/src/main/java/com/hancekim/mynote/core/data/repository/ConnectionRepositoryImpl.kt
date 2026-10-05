package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.common.result.runCatchingCancellable
import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.AttemptRecord
import com.hancekim.mynote.core.data.datasource.BookmarkStore
import com.hancekim.mynote.core.data.datasource.ConnectionIdGenerator
import com.hancekim.mynote.core.data.datasource.ConnectionStateStore
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.NoteSourceAdapter
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.data.datasource.TokenStore
import com.hancekim.mynote.core.data.format.MyNoteJsonFormat
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ConnectionRepositoryImpl internal constructor(
    private val adapters: Set<NoteSourceAdapter>,
    private val local: LocalNotesDataSource,
    private val tokens: TokenStore,
    private val bookmarks: BookmarkStore,
    private val state: ConnectionStateStore,
    private val ids: ConnectionIdGenerator,
    json: Json,
    private val recovery: ConnectionRecovery,
    now: () -> Long,
) : ConnectionRepository {
    @Inject
    constructor(
        adapters: Set<@JvmSuppressWildcards NoteSourceAdapter>,
        local: LocalNotesDataSource,
        tokens: TokenStore,
        bookmarks: BookmarkStore,
        state: ConnectionStateStore,
        ids: ConnectionIdGenerator,
        json: Json,
        recovery: ConnectionRecovery,
    ) : this(adapters, local, tokens, bookmarks, state, ids, json, recovery, System::currentTimeMillis)

    private val reconciler = SourceReconciler(local, MyNoteJsonFormat(json), now)
    private val attempting = MutableStateFlow(false)

    override fun observeConnection(): Flow<ConnectionState> =
        flow {
            recovery.ensure()
            emitAll(
                local
                    .observeSources()
                    .map { sources ->
                        confirmedOf(sources)?.let { (record, connection) ->
                            ConnectionState.Connected(record.id, connection)
                        }
                            ?: ConnectionState.None
                    }.distinctUntilChanged(),
            )
        }

    override fun observeAttempting(): Flow<Boolean> = attempting.asStateFlow()

    override suspend fun countErasable(): ErasureCount {
        recovery.ensure()
        val sources = local.sources()
        val target = confirmedOf(sources)?.first ?: adoptableOf(sources) ?: return ErasureCount(0, 0)
        return ErasureCount(local.noteCount(target.id), bookmarks.countByPrefix("${target.id.value}/"))
    }

    override suspend fun isSeedEligible(): Boolean {
        recovery.ensure()
        return confirmedOf(local.sources()) == null && !state.isSeedBlocked()
    }

    override suspend fun begin(
        connection: RepoConnection,
        token: String?,
    ): PendingConnection {
        recovery.ensure()
        if (attempting.value) recovery.run()
        val sources = local.sources()
        val current = confirmedOf(sources)
        val (kind, id) =
            when {
                current != null && current.second.isSameRepositoryAs(connection) -> {
                    PendingConnection.Kind.SameRepository to current.first.id
                }

                current != null -> {
                    PendingConnection.Kind.New to newSourceId(sources)
                }

                else -> {
                    adoptableOf(sources)?.let { PendingConnection.Kind.Adopt to it.id }
                        ?: (PendingConnection.Kind.New to newSourceId(sources))
                }
            }
        state.saveAttempt(AttemptRecord(id.value, ConnectionConfigCodec.encode(connection), tokens.currentToken()))
        if (kind == PendingConnection.Kind.New) {
            local.ensureSource(
                SourceRecord(id, SourceKind.GitHub, ConnectionConfigCodec.encode(connection, pending = true)),
            )
        }
        if (token != null) tokens.saveToken(token)
        attempting.value = true
        return PendingConnection(id, connection, kind)
    }

    override suspend fun syncPending(pending: PendingConnection): Result<SyncOutcome> =
        withContext(Dispatchers.Default) {
            runCatchingCancellable {
                recovery.ensure()
                val adapter = adapters.firstOrNull { it.kind == SourceKind.GitHub } ?: error("GitHub 어댑터가 없습니다")
                val record =
                    SourceRecord(pending.sourceId, SourceKind.GitHub, ConnectionConfigCodec.encode(pending.connection))
                val outcome = reconciler.reconcile(record, adapter)
                if (pending.kind != PendingConnection.Kind.New && outcome.deleted.isNotEmpty()) {
                    bookmarks.remove(outcome.deleted)
                }
                outcome
            }
        }

    override suspend fun confirm(pending: PendingConnection) {
        val target = ConnectionConfigCodec.encode(pending.connection)
        if (pending.kind == PendingConnection.Kind.New) {
            val replace =
                local
                    .sources()
                    .filter {
                        it.kind == SourceKind.GitHub && it.id != pending.sourceId
                    }.map { it.id }
            if (replace.isNotEmpty()) state.addBookmarkSweep(replace.map { it.value })
            local.promote(pending.sourceId, target, replace)
            replace.forEach { bookmarks.removeByPrefix("${it.value}/") }
            if (replace.isNotEmpty()) state.clearBookmarkSweep()
        } else {
            local.updateConfig(pending.sourceId, target)
        }
        state.clearAttempt()
        state.clearSeedBlock()
        attempting.value = false
    }

    override suspend fun discard(pending: PendingConnection) {
        withContext(NonCancellable) {
            state.attempt()?.let { attempt ->
                if (tokens.currentToken() != attempt.tokenBefore) tokens.saveToken(attempt.tokenBefore)
            }
            if (pending.kind == PendingConnection.Kind.New) local.deleteSource(pending.sourceId)
            state.clearAttempt()
            attempting.value = false
        }
    }

    override suspend fun disconnect(): Result<Unit> =
        runCatchingCancellable {
            recovery.ensure()
            val target = confirmedOf(local.sources())?.first
            if (target != null) {
                state.markDisconnect(target.id.value)
                try {
                    local.deleteSource(target.id)
                } catch (failure: Throwable) {
                    withContext(NonCancellable) {
                        if (local.sources().any { it.id == target.id }) state.clearDisconnectAndSeedBlock()
                    }
                    throw failure
                }
                bookmarks.removeByPrefix("${target.id.value}/")
                tokens.saveToken(null)
                state.clearDisconnectMarker()
            }
        }

    override suspend fun recover() = recovery.ensure()

    private suspend fun newSourceId(sources: List<SourceRecord>): SourceId {
        val taken = state.usedSourceIds() + sources.map { it.id.value } + SourceId.Legacy.value
        repeat(MAX_ID_ATTEMPTS) {
            val candidate = ids.next()
            if (candidate.isNotBlank() && '/' !in candidate && candidate !in taken) {
                state.addUsedSourceId(candidate)
                return SourceId(candidate)
            }
        }
        error("연결 식별자를 만들지 못했습니다")
    }

    private suspend fun adoptableOf(sources: List<SourceRecord>): SourceRecord? =
        sources.firstOrNull {
            it.kind == SourceKind.GitHub && ConnectionConfigCodec.decode(it.configJson) == null &&
                local.noteCount(it.id) > 0
        }
}

internal fun confirmedOf(sources: List<SourceRecord>): Pair<SourceRecord, RepoConnection>? =
    sources.firstNotNullOfOrNull { record ->
        if (record.kind != SourceKind.GitHub) return@firstNotNullOfOrNull null
        ConnectionConfigCodec
            .decode(record.configJson)
            ?.takeIf { !it.pending }
            ?.let { record to it.connection }
    }

private const val MAX_ID_ATTEMPTS = 16
