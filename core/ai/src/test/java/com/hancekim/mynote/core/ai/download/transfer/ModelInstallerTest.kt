package com.hancekim.mynote.core.ai.download.transfer

import com.hancekim.mynote.core.ai.download.decision.ExpectedModel
import com.hancekim.mynote.core.ai.download.decision.HEADER_CONTENT_RANGE
import com.hancekim.mynote.core.ai.download.decision.HEADER_RANGE
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private lateinit var expected: ExpectedModel
    private lateinit var installer: ModelInstaller

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        files = ModelFiles(temp.newFolder("models"), MODEL)
        expected = ExpectedModel(totalBytes = CONTENT_BYTES.size.toLong(), sha256 = shaOf(CONTENT))
        installer = installerFor(files)
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
            assertFalse(files.legacyPlan.exists())
            assertEquals(1, hub.bodyRequests)
        }

    @Test
    fun `MD-1 받기는 HEAD 없이 본문 요청부터 한다`() =
        runTest {
            val hub = serve(CONTENT)

            assertEquals(InstallResult.Installed, install())

            assertEquals(listOf("GET"), hub.methods)
        }

    @Test
    fun `MD-1 허브 헤더가 없어도 받는다`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body(Buffer().write(CONTENT_BYTES))
                    .build(),
            )

            assertEquals(InstallResult.Installed, install())

            assertEquals(CONTENT, files.target.readText())
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `MD-2 기대 크기와 해시가 맞으면 자리에 들어간다`() =
        runTest {
            serve(CONTENT)

            assertEquals(InstallResult.Installed, install())

            assertEquals(CONTENT, files.target.readText())
            assertFalse(files.part.exists())
        }

    @Test
    fun `MD-2 해시가 다르면 버리고 처음부터 받는다`() =
        runTest {
            val hub = serve(SAME_SIZE_OTHER, CONTENT)
            val targetAtVerify = mutableListOf<Boolean>()

            val result = install(onVerifying = { targetAtVerify += files.target.exists() })

            assertEquals(InstallResult.Installed, result)
            assertEquals(CONTENT, files.target.readText())
            assertEquals(listOf(false, false), targetAtVerify)
            assertEquals(listOf<String?>(null, null), hub.ranges)
        }

    @Test
    fun `MD-2 끝내 안 맞으면 8라운드에서 멈춘다`() =
        runTest {
            val hub = serve(SAME_SIZE_OTHER)

            assertEquals(InstallResult.Failed(ModelInstaller.REASON_TOO_MANY_ROUNDS), install())

            assertTrue(hub.bodyRequests in 2..ModelInstaller.MAX_ROUNDS)
            assertFalse(files.target.exists())
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
            val hub = serve(CONTENT)

            assertEquals(InstallResult.Installed, install())

            assertEquals(CONTENT, files.target.readText())
            assertEquals("bytes=10-", hub.lastRange)
        }

    @Test
    fun `MD-4 옛 plan 이 있어도 조각을 이어 받고 plan 을 남기지 않는다`() =
        runTest {
            listOf(OTHER_COMMIT_PLAN, BROKEN_PLAN).forEachIndexed { index, legacy ->
                val device = ModelFiles(temp.newFolder("legacy-$index"), MODEL)
                device.part.writeBytes(CONTENT_BYTES.copyOf(10))
                device.legacyPlan.writeText(legacy)
                val hub = serve(CONTENT)

                assertEquals(legacy, InstallResult.Installed, installerFor(device).install(url()) { _, _ -> })

                assertEquals(legacy, "bytes=10-", hub.ranges.first())
                assertEquals(legacy, CONTENT, device.target.readText())
                assertFalse(legacy, device.legacyPlan.exists())
            }
        }

    @Test
    fun `MD-5 다른 파일의 조각은 섞이지 않고 처음부터 다시 받는다`() =
        runTest {
            files.part.writeText("XXXXX")
            val hub = serve(CONTENT)

            assertEquals(InstallResult.Installed, install())

            assertEquals(CONTENT, files.target.readText())
            assertEquals(2, hub.bodyRequests)
            assertEquals(listOf("bytes=5-", null), hub.ranges)
        }

    @Test
    fun `해시가 안 맞는 본문은 자리에 넣지 않는다`() =
        runTest {
            serve(SAME_SIZE_OTHER)

            val result = install()

            assertTrue(result is InstallResult.Failed)
            assertFalse(files.target.exists())
        }

    @Test
    fun `빈 용량이 모자라면 한 바이트도 받지 않는다`() =
        runTest {
            val hub = serve(CONTENT)
            val tight = installerFor(files) { 1L }

            val result = tight.install(url()) { _, _ -> }

            assertEquals(InstallResult.Failed(ModelInstaller.REASON_NO_SPACE), result)
            assertEquals(0, hub.bodyRequests)
        }

    @Test
    fun `이미 받아 둔 만큼은 용량 계산에서 빠진다`() =
        runTest {
            files.part.writeBytes(CONTENT_BYTES.copyOf(CONTENT_BYTES.size - 1))
            serve(CONTENT)
            val tight = installerFor(files) { 1L }

            val result = tight.install(url()) { _, _ -> }

            assertEquals(InstallResult.Installed, result)
        }

    @Test
    fun `MD-3 공간이 모자라면 한 번도 요청하지 않는다`() =
        runTest {
            serve(CONTENT)
            val tight = installerFor(files) { expected.totalBytes - 1 }

            val result = tight.install(url()) { _, _ -> }

            assertEquals(InstallResult.Failed(ModelInstaller.REASON_NO_SPACE), result)
            assertEquals(0, server.requestCount)
        }

    @Test
    fun `MD-3 받아 둔 조각만큼은 덜 요구한다`() =
        runTest {
            files.part.writeBytes(CONTENT_BYTES.copyOf(10))
            serve(CONTENT)

            val short = installerFor(files) { expected.totalBytes - 11 }.install(url()) { _, _ -> }

            assertEquals(InstallResult.Failed(ModelInstaller.REASON_NO_SPACE), short)
            assertEquals(0, server.requestCount)

            val exact = installerFor(files) { expected.totalBytes - 10 }.install(url()) { _, _ -> }

            assertEquals(InstallResult.Installed, exact)
        }

    @Test
    fun `없는 파일은 포기한다`() =
        runTest {
            serve(CONTENT, bodyStatus = 404)

            assertTrue(install() is InstallResult.Failed)
        }

    @Test
    fun `MD-7 429 는 Retry-After 와 무관하게 재시도한다`() =
        runTest {
            serve(CONTENT, bodyStatus = 429, retryAfter = "120")
            val withHeader = install()
            serve(CONTENT, bodyStatus = 429)
            val withoutHeader = install()

            assertEquals(InstallResult.Retry, withHeader)
            assertEquals(withHeader, withoutHeader)

            serve(CONTENT, bodyStatus = 404)
            assertEquals(InstallResult.Failed("HTTP 404"), install())
            serve(CONTENT, bodyStatus = 403)
            assertEquals(InstallResult.Failed("HTTP 403"), install())
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
            serve("")

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

    @Test
    fun `MD-10 파일 확인 중 취소하면 자리에 넣지 않고 조각을 남긴다`() =
        runTest {
            serve(CONTENT)
            lateinit var job: Job

            job = launch { install(onVerifying = { job.cancel() }) }
            job.join()

            assertTrue(job.isCancelled)
            assertFalse(files.target.exists())
            assertEquals(CONTENT_BYTES.size.toLong(), files.partBytes())
        }

    @Test
    fun `MD-10 해시는 덩어리마다 취소를 확인한다`() {
        val file = temp.newFile("big").apply { writeBytes(ByteArray(300 * 1024)) }
        var chunks = 0

        ModelFiles.sha256Of(file) { chunks++ }

        assertTrue(chunks > 1)
    }

    private suspend fun install(
        onVerifying: () -> Unit = {},
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ) = installer.install(
        url = url(),
        onVerifying = onVerifying,
        onProgress = onProgress,
    )

    private fun installerFor(
        target: ModelFiles,
        allocatableBytes: () -> Long = { Long.MAX_VALUE },
    ) = ModelInstaller(ModelDownloader(OkHttpClient()), target, expected, allocatableBytes)

    private fun url(): String = server.url("/resolve/$COMMIT/$MODEL").toString()

    private fun serve(
        vararg bodies: String,
        bodyStatus: Int = 0,
        retryAfter: String? = null,
    ): FakeHub {
        val hub =
            FakeHub(
                bodies = bodies.map { it.toByteArray() },
                bodyStatus = bodyStatus,
                retryAfter = retryAfter,
            )
        server.dispatcher = hub
        return hub
    }

    private fun shaOf(text: String): String =
        ModelFiles.sha256Of(File(temp.root, "probe-${text.hashCode()}").apply { writeText(text) })

    private class FakeHub(
        private val bodies: List<ByteArray>,
        private val bodyStatus: Int,
        private val retryAfter: String?,
    ) : Dispatcher() {
        val methods = mutableListOf<String>()
        val ranges = mutableListOf<String?>()
        val bodyRequests get() = ranges.size
        val lastRange get() = ranges.lastOrNull()

        override fun dispatch(request: RecordedRequest): MockResponse {
            methods += request.method
            val range = request.headers[HEADER_RANGE]
            ranges += range
            if (bodyStatus != 0) {
                return MockResponse
                    .Builder()
                    .code(bodyStatus)
                    .apply { retryAfter?.let { setHeader("Retry-After", it) } }
                    .build()
            }
            val served = bodies[(ranges.size - 1).coerceAtMost(bodies.lastIndex)]
            val from = range?.removePrefix("bytes=")?.removeSuffix("-")?.toIntOrNull() ?: 0
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
        const val CONTENT = "핸스노트 퀴즈 모델 가중치 덩어리 - 테스트용 본문입니다"
        const val OTHER_COMMIT = "0000000000000000000000000000000000000000"
        const val OTHER_SHA = "ffff38105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
        const val OTHER_COMMIT_PLAN = """{"commit":"$OTHER_COMMIT","sha256":"$OTHER_SHA","totalBytes":2588147712}"""
        const val BROKEN_PLAN = "{ 이건 JSON 이 아니다"
        val CONTENT_BYTES = CONTENT.toByteArray()
        val SAME_SIZE_OTHER = "Y".repeat(CONTENT_BYTES.size)
    }
}
