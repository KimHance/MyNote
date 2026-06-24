package com.hancekim.mynote.core.network.datasource

import com.hancekim.mynote.core.data.datasource.RemoteNotesDataSource
import com.hancekim.mynote.core.data.dto.IndexEntryDto
import com.hancekim.mynote.core.data.dto.NoteDto
import com.hancekim.mynote.core.network.api.GithubRawApi
import javax.inject.Inject

/** GitHub 에서 index/note 를 가져오는 [RemoteNotesDataSource] 구현. */
class RemoteNotesDataSourceImpl @Inject constructor(
    private val api: GithubRawApi,
) : RemoteNotesDataSource {

    // index.json 은 group 키 → 노트 배열 오브젝트. group 을 항목에 펼쳐 평탄화한다(reconcile 은 평탄형만 본다).
    override suspend fun getIndex(): List<IndexEntryDto> =
        api.getIndex().flatMap { (group, notes) ->
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

    override suspend fun getNote(path: String): NoteDto = api.getNote(path)
}
