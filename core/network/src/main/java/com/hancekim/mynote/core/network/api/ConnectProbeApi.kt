package com.hancekim.mynote.core.network.api

import com.hancekim.mynote.core.data.dto.IndexNoteDto
import com.hancekim.mynote.core.network.model.GithubRepoDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

internal interface ConnectProbeApi {
    @GET("repos/{owner}/{repo}")
    @Headers("Accept: application/vnd.github+json")
    suspend fun getRepo(
        @Header("Authorization") authorization: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Response<GithubRepoDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    @Headers("Accept: application/vnd.github.raw+json")
    suspend fun getIndex(
        @Header("Authorization") authorization: String?,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") branch: String?,
    ): Response<Map<String, List<IndexNoteDto>>>
}
