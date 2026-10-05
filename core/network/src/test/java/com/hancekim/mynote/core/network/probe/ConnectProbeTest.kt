package com.hancekim.mynote.core.network.probe

import com.hancekim.mynote.core.data.dto.NotesJson
import com.hancekim.mynote.core.domain.connect.ConnectBranch
import com.hancekim.mynote.core.domain.connect.ConnectCandidate
import com.hancekim.mynote.core.domain.connect.ConnectLinkRepo
import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.domain.connect.ConnectProbeResult
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.network.config.GithubTokenHolder
import com.hancekim.mynote.core.network.datasource.GithubConnectProbe
import com.hancekim.mynote.core.network.di.NetworkModule
import com.hancekim.mynote.core.network.di.buildConnectProbeApi
import com.hancekim.mynote.core.network.interceptor.AuthInterceptor
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

private val LINK_REPO = ConnectLinkRepo("foo", "bar")
private val MAIN = listOf(ConnectCandidate(ConnectBranch.Named("main"), "notes/index.json"))
private const val STORED = "stored-token-value"
private const val CANDIDATE = "candidate-token-value"
private const val REPO_PUBLIC = """{"full_name":"Foo/Bar","private":false,"default_branch":"trunk"}"""
private const val REPO_PRIVATE = """{"full_name":"Foo/Bar","private":true,"default_branch":"trunk"}"""
private const val INDEX =
    """{"android":[{"id":"a","title":"A","date":"2026.01.01","path":"notes/a.json","hash":"h"}]}"""

class ConnectProbeTest {
    private val server = MockWebServer()
    private lateinit var probe: GithubConnectProbe

    @Before
    fun setUp() {
        server.start()
        probe = probeWith(NetworkModule.provideProbeOkHttpClient())
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun probeWith(client: OkHttpClient) =
        GithubConnectProbe(buildConnectProbeApi(client, NotesJson, server.url("/")))

    private fun respond(
        code: Int,
        body: String = "",
        vararg headers: Pair<String, String>,
    ) {
        val builder = MockResponse.Builder().code(code).body(body)
        headers.forEach { (name, value) -> builder.addHeader(name, value) }
        server.enqueue(builder.build())
    }

    private fun failure(result: ConnectProbeResult): ConnectProbeFailure = (result as ConnectProbeResult.Failed).failure

    private suspend fun verify(
        token: String? = null,
        candidates: List<ConnectCandidate> = MAIN,
    ) = probe.verify(LINK_REPO, candidates, token)

    @Test
    fun `RC-4 무인증 확인에는 저장된 토큰을 싣지 않는다`() =
        runTest {
            val holder = GithubTokenHolder().apply { update(STORED) }
            val mainClient = OkHttpClient.Builder().addInterceptor(AuthInterceptor(holder)).build()
            respond(200)
            mainClient.newCall(Request.Builder().url(server.url("/control")).build()).execute().close()
            assertEquals("Bearer $STORED", server.takeRequest().headers["Authorization"])

            respond(200, REPO_PUBLIC)
            respond(200, INDEX)
            val result = verify()

            assertTrue(result is ConnectProbeResult.Verified)
            repeat(2) { assertNull(server.takeRequest().headers["Authorization"]) }
        }

    @Test
    fun `RC-4 공개 저장소는 토큰 없이 연결된다`() =
        runTest {
            respond(200, REPO_PUBLIC)
            respond(200, INDEX)

            val result = verify()

            assertEquals(
                ConnectProbeResult.Verified(RepoConnection("Foo", "Bar", "main", "notes/index.json", isPublic = true)),
                result,
            )
            assertEquals("/repos/foo/bar", server.takeRequest().target)
            assertEquals("/repos/Foo/Bar/contents/notes/index.json?ref=main", server.takeRequest().target)
        }

    @Test
    fun `RC-5 무인증 404 는 토큰 요청 상태가 된다`() =
        runTest {
            respond(404)

            assertEquals(ConnectProbeFailure.NeedsToken, failure(verify()))
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `RC-6 후보 토큰으로 확인한다`() =
        runTest {
            respond(200, REPO_PRIVATE)
            respond(200, INDEX)

            val result = verify(token = CANDIDATE)

            assertEquals(
                ConnectProbeResult.Verified(RepoConnection("Foo", "Bar", "main", "notes/index.json", isPublic = false)),
                result,
            )
            repeat(2) { assertEquals("Bearer $CANDIDATE", server.takeRequest().headers["Authorization"]) }
        }

    @Test
    fun `RC-7 토큰 실패 5종이 서로 다른 결과다`() =
        runTest {
            val results = mutableListOf<ConnectProbeFailure>()
            respond(401)
            results += failure(verify(CANDIDATE))
            respond(404)
            results += failure(verify(CANDIDATE))
            respond(200, REPO_PRIVATE)
            respond(404)
            results += failure(verify(CANDIDATE))
            respond(429)
            results += failure(verify(CANDIDATE))
            respond(500)
            results += failure(verify(CANDIDATE))

            assertEquals(
                listOf(
                    ConnectProbeFailure.TokenRejected,
                    ConnectProbeFailure.RepoNotVisible,
                    ConnectProbeFailure.ManifestInaccessible,
                    ConnectProbeFailure.RateLimited,
                    ConnectProbeFailure.Transient,
                ),
                results,
            )
            assertEquals(5, results.toSet().size)
        }

    @Test
    fun `RC-7 한도는 429 또는 403+한도 헤더다`() =
        runTest {
            respond(429)
            assertEquals(ConnectProbeFailure.RateLimited, failure(verify()))
            respond(403, "", "x-ratelimit-remaining" to "0")
            assertEquals(ConnectProbeFailure.RateLimited, failure(verify()))
            respond(403, "", "retry-after" to "30")
            assertEquals(ConnectProbeFailure.RateLimited, failure(verify()))
            respond(403, "", "x-ratelimit-remaining" to "12")
            assertEquals(ConnectProbeFailure.NeedsToken, failure(verify()))
            respond(403)
            assertEquals(ConnectProbeFailure.RepoNotVisible, failure(verify(CANDIDATE)))

            respond(200, REPO_PUBLIC)
            respond(403, "", "x-ratelimit-remaining" to "0")
            assertEquals(ConnectProbeFailure.RateLimited, failure(verify()))
            respond(200, REPO_PUBLIC)
            respond(403)
            assertEquals(ConnectProbeFailure.ManifestInaccessible, failure(verify(CANDIDATE)))
        }

    @Test
    fun `RC-9 무인증 저장소는 열리고 목록이 없으면 지원하지 않는 형식이다`() =
        runTest {
            respond(200, REPO_PUBLIC)
            respond(404)

            assertEquals(ConnectProbeFailure.NoManifest, failure(verify()))
        }

    @Test
    fun `RC-10 읽을 수 없는 목록은 연결하지 않고 빈 목록은 연결한다`() =
        runTest {
            respond(200, REPO_PUBLIC)
            respond(200, "# 마크다운 안내")
            assertEquals(ConnectProbeFailure.MalformedManifest, failure(verify()))

            respond(200, REPO_PUBLIC)
            respond(200, """["목록이 배열이다"]""")
            assertEquals(ConnectProbeFailure.MalformedManifest, failure(verify()))

            respond(200, REPO_PUBLIC)
            respond(200, "{}")
            assertTrue(verify() is ConnectProbeResult.Verified)
        }

    @Test
    fun `RC-11 확인 중 연결이 안 되면 오프라인이고 시간 초과와 5xx 는 일시 오류다`() =
        runTest {
            respond(503)
            assertEquals(ConnectProbeFailure.Transient, failure(verify()))

            server.enqueue(
                MockResponse
                    .Builder()
                    .headersDelay(3, TimeUnit.SECONDS)
                    .code(200)
                    .build(),
            )
            val quick =
                NetworkModule
                    .provideProbeOkHttpClient()
                    .newBuilder()
                    .readTimeout(
                        200,
                        TimeUnit.MILLISECONDS,
                    ).build()
            assertEquals(ConnectProbeFailure.Transient, failure(probeWith(quick).verify(LINK_REPO, MAIN, null)))

            server.close()
            assertEquals(ConnectProbeFailure.Offline, failure(verify()))
        }

    @Test
    fun `D33 첫 후보가 맞으면 후보를 더 시도하지 않는다`() =
        runTest {
            val candidates =
                listOf(
                    ConnectCandidate(ConnectBranch.Named("feature/x"), "docs/index.json"),
                    ConnectCandidate(ConnectBranch.Named("feature"), "x/docs/index.json"),
                    ConnectCandidate(ConnectBranch.Named("feature/x/docs"), "notes/index.json"),
                )
            respond(200, REPO_PUBLIC)
            respond(200, INDEX)

            val result = verify(candidates = candidates) as ConnectProbeResult.Verified

            assertEquals("feature/x", result.connection.branch)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `D33 후보는 순서대로 시도하고 404 에서만 다음으로 넘어간다`() =
        runTest {
            val candidates =
                listOf(
                    ConnectCandidate(ConnectBranch.Named("feature/x"), "docs/index.json"),
                    ConnectCandidate(ConnectBranch.Named("feature"), "x/docs/index.json"),
                    ConnectCandidate(ConnectBranch.Named("feature/x/docs"), "notes/index.json"),
                )
            respond(200, REPO_PUBLIC)
            respond(404)
            respond(200, INDEX)

            val result = verify(candidates = candidates) as ConnectProbeResult.Verified

            assertEquals(
                RepoConnection("Foo", "Bar", "feature", "x/docs/index.json", isPublic = true),
                result.connection,
            )
            assertEquals(3, server.requestCount)

            respond(200, REPO_PUBLIC)
            respond(404)
            respond(500)
            assertEquals(ConnectProbeFailure.Transient, failure(verify(candidates = candidates)))
        }

    @Test
    fun `RC-2 기본 브랜치 후보는 저장소 응답의 기본 브랜치로 확정한다`() =
        runTest {
            respond(200, REPO_PUBLIC)
            respond(200, INDEX)

            val result =
                verify(candidates = listOf(ConnectCandidate(ConnectBranch.Default, "notes/index.json")))
                    as ConnectProbeResult.Verified

            assertEquals("trunk", result.connection.branch)
            server.takeRequest()
            assertEquals("/repos/Foo/Bar/contents/notes/index.json?ref=trunk", server.takeRequest().target)
        }
}
