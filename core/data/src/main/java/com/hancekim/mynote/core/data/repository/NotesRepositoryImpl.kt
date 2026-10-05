package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.common.result.runCatchingCancellable
import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.NoteSourceAdapter
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.data.dto.toCategories
import com.hancekim.mynote.core.data.format.MyNoteJsonFormat
import com.hancekim.mynote.core.data.format.PARSER_VERSION
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class NotesRepositoryImpl internal constructor(
    private val adapters: Set<NoteSourceAdapter>,
    private val local: LocalNotesDataSource,
    json: Json,
    private val awaitRecovery: suspend () -> Unit,
    now: () -> Long,
) : NotesRepository {
    @Inject
    constructor(
        adapters: Set<@JvmSuppressWildcards NoteSourceAdapter>,
        local: LocalNotesDataSource,
        json: Json,
        recovery: ConnectionRecovery,
    ) : this(adapters, local, json, recovery::ensure, System::currentTimeMillis)

    private val format = MyNoteJsonFormat(json)
    private val reconciler = SourceReconciler(local, format, now)

    override fun observeCategories(): Flow<List<Category>> =
        local
            .observeNotes()
            .map { it.toCategories(format) }
            .flowOn(Dispatchers.Default)

    override suspend fun sync(): Result<SyncOutcome> =
        withContext(Dispatchers.Default) {
            runCatchingCancellable {
                awaitRecovery()
                rederiveStale()
                local
                    .sources()
                    .filter { it.isSyncable() }
                    .mapNotNull { source -> adapters.firstOrNull { it.kind == source.kind }?.let { source to it } }
                    .fold(SyncOutcome()) { total, (source, adapter) -> total + reconciler.reconcile(source, adapter) }
            }
        }

    private suspend fun rederiveStale() {
        val stale = local.loadStale(PARSER_VERSION)
        if (stale.isEmpty()) return
        local.replaceDerived(
            stale.mapNotNull { row ->
                val raw = row.raw ?: return@mapNotNull null
                val derived = format.derive(raw)
                row.copy(blocksJson = derived.blocksJson, excerpt = derived.excerpt, parserVersion = PARSER_VERSION)
            },
        )
    }
}

private fun SourceRecord.isSyncable(): Boolean =
    kind != SourceKind.GitHub || ConnectionConfigCodec.decode(configJson)?.pending == false

private operator fun SyncOutcome.plus(other: SyncOutcome): SyncOutcome =
    SyncOutcome(added + other.added, updated + other.updated, deleted + other.deleted)
