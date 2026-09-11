package com.hancekim.mynote.core.network.datasource

import com.hancekim.mynote.core.data.datasource.RemoteNotesDataSource
import com.hancekim.mynote.core.data.dto.IndexEntryDto
import com.hancekim.mynote.core.data.dto.NoteDto
import com.hancekim.mynote.core.network.api.GithubContentsApi
import com.hancekim.mynote.core.network.config.GithubConfig
import javax.inject.Inject

class RemoteNotesDataSourceImpl @Inject constructor(
    private val api: GithubContentsApi,
    private val config: GithubConfig,
) : RemoteNotesDataSource {
    override suspend fun getIndex(): List<IndexEntryDto> =
        api
            .getIndex(config.owner, config.repo, config.branch)
            .flatMap { (group, notes) ->
                notes.map { n ->
                    IndexEntryDto(
                        id = n.id,
                        group = group,
                        title = n.title,
                        date = n.date,
                        path = n.path,
                        hash = n.hash,
                    )
                }
            }

    override suspend fun getNote(path: String): NoteDto =
        api.getNote(config.owner, config.repo, path, config.branch)
}
