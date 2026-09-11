package com.hancekim.mynote.core.network.datasource

import com.hancekim.mynote.core.data.datasource.RemoteNotesDataSource
import com.hancekim.mynote.core.data.dto.IndexEntryDto
import com.hancekim.mynote.core.data.dto.NoteDto
import com.hancekim.mynote.core.network.api.GithubContentsApi
import com.hancekim.mynote.core.network.config.GithubConfig
import javax.inject.Inject

/**
 * GitHub Contents API 로 index/note 를 가져오는 [RemoteNotesDataSource] 구현.
 * owner/repo/branch 는 [GithubConfig](동적), path 는 index = 고정 / 노트 = index 가 준 값.
 */
class RemoteNotesDataSourceImpl
    @Inject
    constructor(
        private val api: GithubContentsApi,
        private val config: GithubConfig,
    ) : RemoteNotesDataSource {
        // index.json 은 group 키 → 노트 배열 오브젝트. group 을 항목에 펼쳐 평탄화한다(reconcile 은 평탄형만 본다).
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
