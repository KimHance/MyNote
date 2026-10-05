package com.hancekim.mynote.core.network.datasource

import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.NoteSourceAdapter
import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.network.api.GithubContentsApi
import javax.inject.Inject

class GithubManifestAdapter @Inject constructor(
    private val api: GithubContentsApi,
) : NoteSourceAdapter {
    override val kind: SourceKind = SourceKind.GitHub

    override suspend fun list(source: SourceRecord): List<SourceEntry> {
        val connection = connectionOf(source)
        return api
            .getIndex(connection.owner, connection.repo, connection.indexPath, connection.ref())
            .flatMap { (group, notes) ->
                notes.map { note ->
                    SourceEntry(
                        docId = note.id,
                        path = note.path,
                        revision = note.hash,
                        category = group,
                        title = note.title,
                        date = note.date,
                    )
                }
            }
    }

    override suspend fun fetch(
        source: SourceRecord,
        entry: SourceEntry,
    ): String {
        val connection = connectionOf(source)
        return api.getNote(connection.owner, connection.repo, entry.path, connection.ref()).use { it.string() }
    }

    private fun connectionOf(source: SourceRecord): RepoConnection =
        requireNotNull(
            ConnectionConfigCodec.decode(source.configJson),
        ) { "연결 정보가 없는 소스입니다: ${source.id.value}" }.connection
}

private fun RepoConnection.ref(): String? = branch.ifBlank { null }
