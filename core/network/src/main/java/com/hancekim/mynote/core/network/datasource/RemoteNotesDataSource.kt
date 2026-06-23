package com.hancekim.mynote.core.network.datasource

import com.hancekim.mynote.core.data.datasource.NotesDataSource
import com.hancekim.mynote.core.data.dto.CategoryDto
import com.hancekim.mynote.core.network.api.GithubRawApi
import javax.inject.Inject

/**
 * GitHub raw 에서 노트 DTO 를 가져오는 원격 소스([NotesDataSource] 구현).
 * data 의 DataModule 에서 LocalAssetNotesDataSource 대신 이걸 바인딩하면 원격으로 전환된다.
 */
class RemoteNotesDataSource @Inject constructor(
    private val api: GithubRawApi,
) : NotesDataSource {

    override suspend fun getCategories(): List<CategoryDto> = api.getNotes()
}
