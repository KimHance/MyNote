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
import com.hancekim.mynote.core.ai.download.decision.ResponseFacts
import com.hancekim.mynote.core.ai.download.decision.Verdict
import com.hancekim.mynote.core.ai.download.decision.judge
import com.hancekim.mynote.core.ai.download.decision.planFrom
import com.hancekim.mynote.core.ai.download.decision.rangeHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.appendingSink
import okio.buffer
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

internal class ModelDownloader(client: OkHttpClient) {
    private val streaming = client
    private val metadata =
        client
            .newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

    suspend fun plan(url: String): DownloadPlan? =
        withContext(Dispatchers.IO) {
            metadata
                .newCall(
                    Request
                        .Builder()
                        .url(url)
                        .head()
                        .build(),
                ).execute()
                .use { response ->
                    planFrom(
                        size = response.header(HEADER_LINKED_SIZE),
                        etag = response.header(HEADER_LINKED_ETAG),
                        commit = response.header(HEADER_REPO_COMMIT),
                    )
                }
        }

    suspend fun fetchInto(
        url: String,
        part: File,
        from: Long,
        onProgress: (Long) -> Unit,
    ): Verdict =
        withContext(Dispatchers.IO) {
            val call = streaming.newCall(requestFor(url, from))
            val cancelHook = currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
            try {
                call.execute().use { response ->
                    val verdict = judge(from, factsOf(response))
                    if (verdict is Verdict.Append) {
                        stream(response, part, verdict.at, onProgress)
                    }
                    verdict
                }
            } catch (cause: IOException) {
                currentCoroutineContext().ensureActive()
                Verdict.RetryLater(null)
            } finally {
                cancelHook.dispose()
            }
        }

    private fun requestFor(url: String, from: Long): Request =
        Request
            .Builder()
            .url(url)
            .header(HEADER_ACCEPT_ENCODING, ENCODING_IDENTITY)
            .apply { rangeHeader(from)?.let { header(HEADER_RANGE, it) } }
            .build()

    private fun factsOf(response: Response): ResponseFacts =
        ResponseFacts(
            status = response.code,
            contentRange = response.header(HEADER_CONTENT_RANGE),
            retryAfterSeconds = response.header(HEADER_RETRY_AFTER)?.trim()?.toLongOrNull(),
        )

    private suspend fun stream(
        response: Response,
        part: File,
        at: Long,
        onProgress: (Long) -> Unit,
    ) {
        RandomAccessFile(part, "rw").use { it.setLength(at) }
        val source = response.body.source()
        part.appendingSink().buffer().use { sink ->
            val chunk = Buffer()
            var written = at
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = source.read(chunk, CHUNK_BYTES)
                if (read == -1L) break
                sink.write(chunk, read)
                written += read
                onProgress(written)
            }
        }
    }

    private companion object {
        const val CHUNK_BYTES = 64L * 1024L
    }
}
