package com.hancekim.mynote.core.network.api

import com.hancekim.mynote.core.data.dto.IndexNoteDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

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
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") branch: String,
    ): ResponseBody

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}
