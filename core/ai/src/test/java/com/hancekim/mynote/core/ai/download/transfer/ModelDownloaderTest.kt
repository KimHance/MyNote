package com.hancekim.mynote.core.ai.download.transfer

import com.hancekim.mynote.core.ai.download.decision.DownloadPlan
import com.hancekim.mynote.core.ai.download.decision.ENCODING_IDENTITY
import com.hancekim.mynote.core.ai.download.decision.HEADER_ACCEPT_ENCODING
import com.hancekim.mynote.core.ai.download.decision.HEADER_CONTENT_RANGE
import com.hancekim.mynote.core.ai.download.decision.HEADER_LINKED_ETAG
import com.hancekim.mynote.core.ai.download.decision.HEADER_LINKED_SIZE
import com.hancekim.mynote.core.ai.download.decision.HEADER_RANGE
import com.hancekim.mynote.core.ai.download.decision.HEADER_REPO_COMMIT
import com.hancekim.mynote.core.ai.download.decision.HEADER_RETRY_AFTER
import com.hancekim.mynote.core.ai.download.decision.Verdict
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

class ModelDownloaderTest {
    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var downloader: ModelDownloader
    private lateinit var part: File

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        downloader = ModelDownloader(OkHttpClient())
        part = File(temp.root, "model.part")
    }

    @After
    fun stop() = server.close()

    @Test
    fun `처음부터 받을 때는 Range 없이 요청하고 그대로 쓴다`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body("hello")
                    .build(),
            )

            val verdict = downloader.fetchInto(url(), part, from = 0L) {}

            assertEquals(Verdict.Append(0L), verdict)
            assertEquals("hello", part.readText())
            assertNull(waitForRequest().headers[HEADER_RANGE])
        }

    @Test
    fun `이어받을 때는 Range 를 붙이고 뒤에 이어 쓴다`() =
        runTest {
            part.writeText("AAAA")
            server.enqueue(partialFrom(4, "BBBBB"))

            val verdict = downloader.fetchInto(url(), part, from = 4L) {}

            assertEquals(Verdict.Append(4L), verdict)
            assertEquals("AAAABBBBB", part.readText())
            assertEquals("bytes=4-", waitForRequest().headers[HEADER_RANGE])
        }

    @Test
    fun `압축을 끄라고 요청한다`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body("hello")
                    .build(),
            )

            downloader.fetchInto(url(), part, from = 0L) {}

            assertEquals(ENCODING_IDENTITY, waitForRequest().headers[HEADER_ACCEPT_ENCODING])
        }

    @Test
    fun `206 이 딴 자리에서 시작하면 한 바이트도 쓰지 않는다`() =
        runTest {
            part.writeText("AAAA")
            server.enqueue(partialFrom(0, "XXXXXXXXX"))

            val verdict = downloader.fetchInto(url(), part, from = 4L) {}

            assertEquals(Verdict.StartOver, verdict)
            assertEquals("AAAA", part.readText())
        }

    @Test
    fun `이어받기를 요청했는데 200 이 오면 한 바이트도 쓰지 않는다`() =
        runTest {
            part.writeText("AAAA")
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body("XXXXXXXXX")
                    .build(),
            )

            val verdict = downloader.fetchInto(url(), part, from = 4L) {}

            assertEquals(Verdict.StartOver, verdict)
            assertEquals("AAAA", part.readText())
        }

    @Test
    fun `429 는 서버가 말한 대기 시간을 그대로 전한다`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(429)
                    .setHeader(HEADER_RETRY_AFTER, "295")
                    .build(),
            )

            assertEquals(Verdict.RetryLater(295L), downloader.fetchInto(url(), part, from = 0L) {})
        }

    @Test
    fun `404 는 포기한다`() =
        runTest {
            server.enqueue(MockResponse.Builder().code(404).build())

            assertEquals(Verdict.GiveUp::class, downloader.fetchInto(url(), part, from = 0L) {}::class)
        }

    @Test
    fun `진행률은 파일 전체 기준 누적으로 올라간다`() =
        runTest {
            part.writeText("AAAA")
            server.enqueue(partialFrom(4, "B".repeat(5_000)))
            val seen = mutableListOf<Long>()

            downloader.fetchInto(url(), part, from = 4L) { seen += it }

            assertTrue(seen.isNotEmpty())
            assertEquals(5_004L, seen.last())
            assertEquals(seen.sorted(), seen)
            assertTrue(seen.first() > 4L)
        }

    @Test
    fun `본문이 끊겨도 받은 만큼은 남고 재시도 대상이 된다`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .chunkedBody(Buffer().writeUtf8(FULL_BODY), 4)
                    .onResponseBody(SocketEffect.CloseSocket())
                    .build(),
            )

            val verdict = downloader.fetchInto(url(), part, from = 0L) {}

            assertEquals(Verdict.RetryLater(null), verdict)
            assertTrue(part.readText().isNotEmpty())
            assertTrue(part.readText().length < FULL_BODY.length)
            assertTrue(FULL_BODY.startsWith(part.readText()))
        }

    @Test
    fun `plan 은 302 를 따라가지 않고 헤더에서 신분증을 읽는다`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(302)
                    .setHeader("Location", "https://cdn.example.test/blob")
                    .setHeader(HEADER_LINKED_SIZE, "2588147712")
                    .setHeader(HEADER_LINKED_ETAG, "\"$SHA\"")
                    .setHeader(HEADER_REPO_COMMIT, COMMIT)
                    .build(),
            )

            val plan = downloader.plan(url())

            assertEquals(DownloadPlan(COMMIT, SHA, 2_588_147_712L), plan)
            assertEquals("HEAD", waitForRequest().method)
        }

    @Test
    fun `헤더가 없으면 신분증이 없다`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(302)
                    .setHeader("Location", "https://x.test/b")
                    .build(),
            )

            assertNull(downloader.plan(url()))
        }

    private fun waitForRequest() =
        requireNotNull(server.takeRequest(5, TimeUnit.SECONDS)) { "요청이 서버에 도달하지 않았다" }

    private fun url() = server.url("/resolve/$COMMIT/model").toString()

    private fun partialFrom(start: Long, body: String): MockResponse =
        MockResponse
            .Builder()
            .code(206)
            .setHeader(HEADER_CONTENT_RANGE, "bytes $start-${start + body.length - 1}/$TOTAL")
            .body(body)
            .build()

    private companion object {
        const val TOTAL = 9L
        const val FULL_BODY = "0123456789ABCDEF"
        const val COMMIT = "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1"
        const val SHA = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
    }
}
