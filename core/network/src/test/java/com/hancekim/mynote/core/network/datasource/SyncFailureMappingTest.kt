package com.hancekim.mynote.core.network.datasource

import com.hancekim.mynote.core.data.connection.ConnectionConfigCodec
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.data.dto.NotesJson
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.network.api.GithubContentsApi
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

private val SOURCE =
    SourceRecord(
        SourceId.Legacy,
        SourceKind.GitHub,
        ConnectionConfigCodec.encode(RepoConnection("o", "r", "main", "notes/index.json", isPublic = true)),
    )

class SyncFailureMappingTest {
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun adapter(client: OkHttpClient = OkHttpClient()): GithubManifestAdapter =
        GithubManifestAdapter(
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(NotesJson.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(GithubContentsApi::class.java),
        )

    private fun respond(
        code: Int,
        body: String = "",
        vararg headers: Pair<String, String>,
    ) {
        val builder = MockResponse.Builder().code(code).body(body)
        headers.forEach { (name, value) -> builder.addHeader(name, value) }
        server.enqueue(builder.build())
    }

    private suspend fun listFailure(adapter: GithubManifestAdapter = adapter()): KClass<out Throwable> =
        try {
            adapter.list(SOURCE)
            error("실패해야 합니다")
        } catch (failure: SyncFailure) {
            failure::class
        }

    @Test
    fun `SY-5 실패 응답이 원인 종류로 갈린다`() =
        runTest {
            respond(401)
            assertEquals(SyncFailure.TokenRejected::class, listFailure())
            respond(404)
            assertEquals(SyncFailure.AccessDenied::class, listFailure())
            respond(403)
            assertEquals(SyncFailure.AccessDenied::class, listFailure())
            respond(429)
            assertEquals(SyncFailure.RateLimited::class, listFailure())
            respond(500)
            assertEquals(SyncFailure.Server::class, listFailure())
            respond(200, "목록이 아니다")
            assertEquals(SyncFailure.Format::class, listFailure())

            server.enqueue(
                MockResponse
                    .Builder()
                    .headersDelay(3, TimeUnit.SECONDS)
                    .code(200)
                    .build(),
            )
            val quick = OkHttpClient.Builder().readTimeout(200, TimeUnit.MILLISECONDS).build()
            assertEquals(SyncFailure.Server::class, listFailure(adapter(quick)))

            server.close()
            assertEquals(SyncFailure.Offline::class, listFailure())
        }

    @Test
    fun `SY-5 한도 규칙 3경우와 일반 403`() =
        runTest {
            respond(429)
            assertEquals(SyncFailure.RateLimited::class, listFailure())
            respond(403, "", "x-ratelimit-remaining" to "0")
            assertEquals(SyncFailure.RateLimited::class, listFailure())
            respond(403, "", "retry-after" to "60")
            assertEquals(SyncFailure.RateLimited::class, listFailure())
            respond(403, "", "x-ratelimit-remaining" to "7")
            assertEquals(SyncFailure.AccessDenied::class, listFailure())
        }

    @Test
    fun `SY-5 본문 수신 실패도 종류로 던지고 원인을 남긴다`() =
        runTest {
            respond(429)
            val failure =
                try {
                    adapter().fetch(
                        SOURCE,
                        com.hancekim.mynote.core.data.datasource
                            .SourceEntry("a", "notes/a.json", "h", "g", null, null),
                    )
                    null
                } catch (thrown: SyncFailure) {
                    thrown
                }

            assertTrue(failure is SyncFailure.RateLimited)
            assertTrue(failure?.cause is retrofit2.HttpException)
        }
}
