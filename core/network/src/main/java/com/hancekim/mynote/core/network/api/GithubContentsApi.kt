package com.hancekim.mynote.core.network.api

import com.hancekim.mynote.core.data.dto.IndexNoteDto
import com.hancekim.mynote.core.data.dto.NoteDto
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * GitHub Contents API. private 레포의 파일을 토큰으로 읽는다.
 *   GET /repos/{owner}/{repo}/contents/{path}?ref={branch}
 *   Accept: application/vnd.github.raw+json  → 본문을 raw(파일 그대로)로 받음
 *
 * index 와 노트는 같은 엔드포인트의 path 차이일 뿐이다. owner/repo/branch 는 동적(config),
 * path 도 동적(index = 고정 "notes/index.json", 노트 = index 가 준 path).
 */
interface GithubContentsApi {

    @GET("repos/{owner}/{repo}/contents/notes/index.json")
    @Headers("Accept: application/vnd.github.raw+json")
    suspend fun getIndex(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ref") branch: String,
    ): Map<String, List<IndexNoteDto>>

    @GET("repos/{owner}/{repo}/contents/{path}")
    @Headers("Accept: application/vnd.github.raw+json")
    suspend fun getNote(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String, // "notes/<group>/<id>.json" — 슬래시 보존
        @Query("ref") branch: String,
    ): NoteDto

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}
