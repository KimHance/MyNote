package com.hancekim.mynote.core.network.api

import com.hancekim.mynote.core.data.dto.CategoryDto
import retrofit2.http.GET

/**
 * GitHub raw 에서 노트 JSON 을 가져오는 Retrofit API.
 * 실제 노트 repo 가 정해지면 [BASE_URL] 만 교체하면 된다(URL 은 설정값).
 */
interface GithubRawApi {

    @GET(NOTES_PATH)
    suspend fun getNotes(): List<CategoryDto>

    companion object {
        // TODO: 노트 데이터 repo 확정 시 교체 — https://raw.githubusercontent.com/<user>/<repo>/<branch>/
        const val BASE_URL = "https://raw.githubusercontent.com/KimHance/notes/main/"
        const val NOTES_PATH = "notes.json"
    }
}
