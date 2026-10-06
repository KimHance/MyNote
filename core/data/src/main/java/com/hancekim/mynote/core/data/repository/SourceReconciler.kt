package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.CachedNoteState
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.NoteSourceAdapter
import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.domain.model.NoteKey
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.format.NoteFormat
import com.hancekim.mynote.core.format.PARSER_VERSION
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

@OptIn(ExperimentalCoroutinesApi::class)
internal class SourceReconciler(
    private val local: LocalNotesDataSource,
    private val format: NoteFormat,
    private val now: () -> Long,
) {
    suspend fun reconcile(
        source: SourceRecord,
        adapter: NoteSourceAdapter,
    ): SyncOutcome {
        val entries = adapter.list(source)
        val state = local.loadSourceState(source.id)
        if (wipesEverything(entries.size, state.size)) {
            error("원격 index 가 비어 있어 중단합니다 — ${source.id.value} 로컬 ${state.size}건을 지키기 위해")
        }

        val targets = entries.filter { it.needsFetch(state[it.docId]) }
        val fetched = fetchAll(adapter, source, targets)
        val fetchedAt = now()
        val upserts = fetched.map { (entry, raw) -> derive(source.id, entry, raw, state[entry.docId], fetchedAt) }
        val listed = entries.mapTo(HashSet()) { it.docId }
        val deleteDocIds = state.keys.filterNot { it in listed }

        if (upserts.isNotEmpty() || deleteDocIds.isNotEmpty()) {
            local.applySourceSync(source.id, upserts, deleteDocIds)
        }

        val added = targets.count { state[it.docId] == null }
        val updated = targets.count { entry -> state[entry.docId]?.let { it.revision != entry.revision } == true }
        return SyncOutcome(added, updated, deleteDocIds.map { NoteKey(source.id, it).value })
    }

    private suspend fun fetchAll(
        adapter: NoteSourceAdapter,
        source: SourceRecord,
        entries: List<SourceEntry>,
    ): List<Pair<SourceEntry, String>> =
        entries
            .asFlow()
            .flatMapMerge { entry -> flow { emit(entry to adapter.fetch(source, entry)) } }
            .toList()

    private fun derive(
        source: SourceId,
        entry: SourceEntry,
        raw: String,
        previous: CachedNoteState?,
        fetchedAt: Long,
    ): CachedNote {
        val derived = format.derive(entry.path, raw)
        return CachedNote(
            key = NoteKey(source, entry.docId).value,
            sourceId = source.value,
            docId = entry.docId,
            path = entry.path,
            group = entry.category,
            title = entry.title ?: entry.docId,
            date = entry.date,
            epochDay = epochDayOf(entry.date),
            firstSeenAt = previous?.firstSeenAt ?: fetchedAt,
            revision = entry.revision,
            fetchedAt = fetchedAt,
            parserVersion = PARSER_VERSION,
            raw = raw,
            blocksJson = derived.blocksJson,
            excerpt = derived.excerpt,
        )
    }
}

private fun SourceEntry.needsFetch(previous: CachedNoteState?): Boolean =
    previous == null || previous.revision != revision || !previous.hasRaw

internal fun wipesEverything(
    remoteCount: Int,
    localCount: Int,
): Boolean = remoteCount == 0 && localCount > 0
