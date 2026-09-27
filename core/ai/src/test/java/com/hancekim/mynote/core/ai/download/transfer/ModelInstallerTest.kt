package com.hancekim.mynote.core.ai.download.transfer

import com.hancekim.mynote.core.ai.download.decision.DownloadPlan
import com.hancekim.mynote.core.ai.download.decision.HEADER_CONTENT_RANGE
import com.hancekim.mynote.core.ai.download.decision.HEADER_LINKED_ETAG
import com.hancekim.mynote.core.ai.download.decision.HEADER_LINKED_SIZE
import com.hancekim.mynote.core.ai.download.decision.HEADER_RANGE
import com.hancekim.mynote.core.ai.download.decision.HEADER_REPO_COMMIT
import com.hancekim.mynote.core.ai.download.decision.HEADER_RETRY_AFTER
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelInstallerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var files: ModelFiles
    private lateinit var installer: ModelInstaller
    private lateinit var sha: String

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        files = ModelFiles(temp.newFolder("models"), MODEL)
        installer = ModelInstaller(ModelDownloader(OkHttpClient()), files)
        sha = shaOf(CONTENT)
    }

    @After
    fun stop() = server.close()

    @Test
    fun `처음부터 받아 검증까지 마치면 자리에 들어간다`() =
        runTest {
            val hub = serve(CONTENT)

            assertEquals(InstallResult.Installed, install())

            assertEquals(CONTENT, files.target.readText())
            assertFalse(files.part.exists())
            assertFalse(files.plan.exists())
            assertEquals(1, hub.bodyRequests)
        }

    @Test
    fun `기기에 이미 맞는 파일이 있으면 본문을 받지 않는다`() =
        runTest {
            files.target.writeText(CONTENT)
            val hub = serve(CONTENT)

            assertEquals(InstallResult.Installed, install())

            assertEquals(0, hub.bodyRequests)
        }

    @Test
    fun `자리 파일이 크기만 같고 내용이 다르면 버리고 다시 받는다`() =
        runTest {
            files.target.writeBytes(ByteArray(CONTENT_BYTES.size) { 'X'.code.toByte() })
            val hub = serve(CONTENT)

            assertEquals(InstallResult.Installed, install())

            assertEquals(CONTENT, files.target.readText())
            assertEquals(1, hub.bodyRequests)
        }

    @Test
    fun `중간까지 받아 둔 것을 이어받는다`() =
        runTest {
            files.part.writeBytes(CONTENT_BYTES.copyOf(10))
            files.savePlan(DownloadPlan(COMMIT, sha, CONTENT_BYTES.size.toLong()))
            val hub = serve(CONTENT)

            assertEquals(InstallResult.Installed, install())

            assertEquals(CONTENT, files.target.readText())
            assertEquals("bytes=10-", hub.lastRange)
        }

    @Test
    fun `다른 커밋의 part 는 버리고 처음부터 받는다`() =
        runTest {
            files.part.writeText("낯선 조각")
            files.savePlan(DownloadPlan(OTHER_COMMIT, sha, CONTENT_BYTES.size.toLong()))
            val hub = serve(CONTENT)

            assertEquals(InstallResult.Installed, install())

            assertEquals(CONTENT, files.target.readText())
            assertEquals(null, hub.lastRange)
            assertEquals(1, hub.bodyRequests)
        }

    @Test
    fun `처음부터 받아야 하는데 오류가 나면 낡은 part 를 남기지 않는다`() =
        runTest {
            files.part.writeText("낯선 조각")
            files.savePlan(DownloadPlan(OTHER_COMMIT, sha, CONTENT_BYTES.size.toLong()))
            serve(CONTENT, bodyStatus = 404)

            assertTrue(install() is InstallResult.Failed)

            assertEquals(0L, files.partBytes())
        }

    @Test
    fun `해시가 안 맞는 본문은 자리에 넣지 않는다`() =
        runTest {
            serve(CONTENT, bodyOverride = "Y".repeat(CONTENT_BYTES.size))

            val result = install()

            assertTrue(result is InstallResult.Failed)
            assertFalse(files.target.exists())
        }

    @Test
    fun `빈 용량이 모자라면 한 바이트도 받지 않는다`() =
        runTest {
            val hub = serve(CONTENT)
            val tight = ModelInstaller(ModelDownloader(OkHttpClient()), files) { 1L }

            val result = tight.install(server.url("/resolve/$COMMIT/$MODEL").toString()) { _, _ -> }

            assertEquals(InstallResult.Failed(ModelInstaller.REASON_NO_SPACE), result)
            assertEquals(0, hub.bodyRequests)
        }

    @Test
    fun `이미 받아 둔 만큼은 용량 계산에서 빠진다`() =
        runTest {
            files.part.writeBytes(CONTENT_BYTES.copyOf(CONTENT_BYTES.size - 1))
            files.savePlan(DownloadPlan(COMMIT, sha, CONTENT_BYTES.size.toLong()))
            serve(CONTENT)
            val tight = ModelInstaller(ModelDownloader(OkHttpClient()), files) { 1L }

            val result = tight.install(server.url("/resolve/$COMMIT/$MODEL").toString()) { _, _ -> }

            assertEquals(InstallResult.Installed, result)
        }

    @Test
    fun `신분증을 못 읽으면 받지 않고 실패한다`() =
        runTest {
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest) = MockResponse.Builder().code(302).build()
                }

            assertEquals(InstallResult.Failed(ModelInstaller.REASON_NO_PLAN), install())
        }

    @Test
    fun `없는 파일은 포기한다`() =
        runTest {
            serve(CONTENT, bodyStatus = 404)

            assertTrue(install() is InstallResult.Failed)
        }

    @Test
    fun `429 는 대기 시간을 안고 재시도로 넘어간다`() =
        runTest {
            serve(CONTENT, bodyStatus = 429, retryAfter = 295L)

            assertEquals(InstallResult.Retry(295L), install())
        }

    @Test
    fun `진행률은 받은 바이트와 전체 크기를 함께 올린다`() =
        runTest {
            serve(CONTENT)
            val seen = mutableListOf<Pair<Long, Long>>()

            install(onProgress = { received, total -> seen += received to total })

            assertTrue(seen.isNotEmpty())
            val total = CONTENT_BYTES.size.toLong()
            assertEquals(total to total, seen.last())
            assertTrue(seen.all { it.second == total })
        }

    @Test
    fun `한 바이트도 안 오면 무한히 돌지 않는다`() =
        runTest {
            serve(CONTENT, bodyOverride = "")

            assertEquals(InstallResult.Failed(ModelInstaller.REASON_NO_PROGRESS), install())
        }

    @Test
    fun `해시를 재기 전에 검증 단계를 알린다`() =
        runTest {
            serve(CONTENT)
            val phases = mutableListOf<Long>()

            install(onVerifying = { phases += files.partBytes() })

            assertEquals(listOf(CONTENT_BYTES.size.toLong()), phases)
        }

    private suspend fun install(
        onVerifying: () -> Unit = {},
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ) = installer.install(
        url = server.url("/resolve/$COMMIT/$MODEL").toString(),
        onVerifying = onVerifying,
        onProgress = onProgress,
    )

    private fun serve(
        content: String,
        bodyOverride: String? = null,
        bodyStatus: Int = 0,
        retryAfter: Long? = null,
    ): FakeHub {
        val hub =
            FakeHub(
                content = content.toByteArray(),
                sha = shaOf(content),
                bodyOverride = bodyOverride?.toByteArray(),
                bodyStatus = bodyStatus,
                retryAfter = retryAfter,
            )
        server.dispatcher = hub
        return hub
    }

    private fun shaOf(text: String): String =
        ModelFiles.sha256Of(File(temp.root, "probe-${text.hashCode()}").apply { writeText(text) })

    private class FakeHub(
        private val content: ByteArray,
        private val sha: String,
        private val bodyOverride: ByteArray?,
        private val bodyStatus: Int,
        private val retryAfter: Long?,
    ) : Dispatcher() {
        var bodyRequests = 0
        var lastRange: String? = null

        override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.method == "HEAD") {
                return MockResponse
                    .Builder()
                    .code(302)
                    .setHeader("Location", "https://cdn.example.test/blob")
                    .setHeader(HEADER_LINKED_SIZE, content.size.toString())
                    .setHeader(HEADER_LINKED_ETAG, "\"$sha\"")
                    .setHeader(HEADER_REPO_COMMIT, COMMIT)
                    .build()
            }
            bodyRequests++
            lastRange = request.headers[HEADER_RANGE]
            if (bodyStatus != 0) {
                return MockResponse
                    .Builder()
                    .code(bodyStatus)
                    .apply { retryAfter?.let { setHeader(HEADER_RETRY_AFTER, it.toString()) } }
                    .build()
            }
            val served = bodyOverride ?: content
            val from = lastRange?.removePrefix("bytes=")?.removeSuffix("-")?.toIntOrNull() ?: 0
            if (from == 0) {
                return MockResponse
                    .Builder()
                    .code(200)
                    .body(Buffer().write(served))
                    .build()
            }
            return MockResponse
                .Builder()
                .code(206)
                .setHeader(HEADER_CONTENT_RANGE, "bytes $from-${served.size - 1}/${served.size}")
                .body(Buffer().write(served, from, served.size - from))
                .build()
        }
    }

    private companion object {
        const val MODEL = "gemma-4-E2B-it.litertlm"
        const val COMMIT = "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1"
        const val OTHER_COMMIT = "0000000000000000000000000000000000000000"
        const val CONTENT = "핸스노트 퀴즈 모델 가중치 덩어리 - 테스트용 본문입니다"
        val CONTENT_BYTES = CONTENT.toByteArray()
    }
}
