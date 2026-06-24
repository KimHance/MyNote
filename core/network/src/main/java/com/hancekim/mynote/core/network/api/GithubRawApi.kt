package com.hancekim.mynote.core.network.api

import com.hancekim.mynote.core.data.dto.IndexNoteDto
import com.hancekim.mynote.core.data.dto.NoteDto
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * GitHub 노트 소스 Retrofit API.
 * - [getIndex] : notes/index.json (group 키 → 노트 배열인 JSON 오브젝트)
 * - [getNote]  : 개별 노트 본문(index 의 path 로 GET)
 * 실제 노트 repo/토큰 분기는 후속. 지금은 [BASE_URL] 설정값만 둔다.
 */
interface GithubRawApi {

    @GET("notes/index.json")
    suspend fun getIndex(): Map<String, List<IndexNoteDto>>

    @GET
    suspend fun getNote(@Url path: String): NoteDto

    companion object {
        // TODO: 노트 데이터 repo/토큰 확정 시 교체(private 면 Contents API 분기).
        const val BASE_URL = "https://raw.githubusercontent.com/KimHance/notes/main/"
    }
}
