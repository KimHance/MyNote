package com.hancekim.mynote.core.network.datasource

import com.hancekim.mynote.core.data.datasource.NoteSourceAdapter
import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.network.api.GithubContentsApi
import com.hancekim.mynote.core.network.config.GithubConfig
import javax.inject.Inject

class GithubManifestAdapter @Inject constructor(
    private val api: GithubContentsApi,
    private val config: GithubConfig,
) : NoteSourceAdapter {
    override val kind: SourceKind = SourceKind.GitHub

    override suspend fun list(source: SourceRecord): List<SourceEntry> =
        api
            .getIndex(config.owner, config.repo, config.branch)
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

    override suspend fun fetch(
        source: SourceRecord,
        entry: SourceEntry,
    ): String = api.getNote(config.owner, config.repo, entry.path, config.branch).use { it.string() }
}
