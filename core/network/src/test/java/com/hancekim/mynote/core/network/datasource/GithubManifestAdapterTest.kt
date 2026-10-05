package com.hancekim.mynote.core.network.datasource

import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.data.dto.NotesJson
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.network.api.GithubContentsApi
import com.hancekim.mynote.core.network.config.GithubConfig
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val SOURCE = SourceRecord(SourceId.Legacy, SourceKind.GitHub, configJson = null)

class GithubManifestAdapterTest {
    private val server = MockWebServer()
    private lateinit var adapter: GithubManifestAdapter

    @Before
    fun setUp() {
        server.start()
        val api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(NotesJson.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(GithubContentsApi::class.java)
        adapter = GithubManifestAdapter(api, GithubConfig(owner = "o", repo = "r", branch = "main"))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(body: String) = server.enqueue(MockResponse.Builder().body(body).build())

    @Test
    fun `index 의 그룹 키를 카테고리로 접어 목록을 만든다`() =
        runTest {
            enqueue(
                """
                {"android":[{"id":"nav3","title":"Navigation 3","date":"2026.06.18","path":"notes/android/nav3.json","hash":"h1"}],
                 "cs":[{"id":"http","title":"HTTP","date":"2026.04.22","path":"notes/cs/http.json","hash":"h2"}]}
                """.trimIndent(),
            )

            val entries = adapter.list(SOURCE)

            assertEquals(
                listOf(
                    SourceEntry("nav3", "notes/android/nav3.json", "h1", "android", "Navigation 3", "2026.06.18"),
                    SourceEntry("http", "notes/cs/http.json", "h2", "cs", "HTTP", "2026.04.22"),
                ),
                entries,
            )
        }

    @Test
    fun `본문은 해석하지 않고 원문 문자열로 돌려준다`() =
        runTest {
            enqueue("not json {")

            val raw = adapter.fetch(SOURCE, SourceEntry("a", "notes/android/a.json", "h", "android", "a", null))

            assertEquals("not json {", raw)
        }

    @Test
    fun `요청 경로와 ref 와 Accept 헤더가 기준선과 같다`() =
        runTest {
            enqueue("{}")
            enqueue("{}")

            adapter.list(SOURCE)
            adapter.fetch(SOURCE, SourceEntry("a", "notes/android/a.json", "h", "android", "a", null))

            val index = server.takeRequest()
            assertEquals("/repos/o/r/contents/notes/index.json?ref=main", index.target)
            assertEquals("application/vnd.github.raw+json", index.headers["Accept"])
            val note = server.takeRequest()
            assertEquals("/repos/o/r/contents/notes/android/a.json?ref=main", note.target)
            assertEquals("application/vnd.github.raw+json", note.headers["Accept"])
        }
}
