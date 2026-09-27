package com.hancekim.mynote.core.ai.download

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class OkHttpRedirectProbeTest {
    private lateinit var hub: MockWebServer
    private lateinit var cdn: MockWebServer

    @Before
    fun start() {
        hub = MockWebServer().apply { start() }
        cdn = MockWebServer().apply { start() }
    }

    @After
    fun stop() {
        hub.close()
        cdn.close()
    }

    @Test
    fun `호스트가 바뀌는 302 를 넘어도 Range 헤더가 살아남는다`() {
        cdn.enqueue(
            MockResponse
                .Builder()
                .code(206)
                .body("tail")
                .build(),
        )
        hub.enqueue(redirectTo(otherHostUrl()))

        call {
            header("Range", "bytes=1100000000-")
            header("Accept-Encoding", "identity")
        }

        hub.takeRequest()
        val atCdn = cdn.takeRequest()

        assertEquals("bytes=1100000000-", atCdn.headers["Range"])
        assertEquals("identity", atCdn.headers["Accept-Encoding"])
    }

    @Test
    fun `호스트가 바뀌면 Authorization 은 따라가지 않는다`() {
        cdn.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body("body")
                .build(),
        )
        hub.enqueue(redirectTo(otherHostUrl()))

        call { header("Authorization", "Bearer never-leak-this") }

        hub.takeRequest()

        assertNull(cdn.takeRequest().headers["Authorization"])
    }

    @Test
    fun `호스트가 같고 포트만 달라도 Authorization 은 떨어진다`() {
        cdn.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body("body")
                .build(),
        )
        hub.enqueue(redirectTo(cdn.url("/blob").toString()))

        call { header("Authorization", "Bearer same-host-other-port") }

        hub.takeRequest()

        assertNull(cdn.takeRequest().headers["Authorization"])
    }

    private fun otherHostUrl(): String =
        cdn
            .url("/blob")
            .newBuilder()
            .host("127.0.0.1")
            .build()
            .toString()

    private fun redirectTo(location: String): MockResponse =
        MockResponse
            .Builder()
            .code(302)
            .setHeader("Location", location)
            .build()

    private fun call(configure: Request.Builder.() -> Unit) {
        val request =
            Request
                .Builder()
                .url(hub.url("/resolve/main/model"))
                .apply(configure)
                .build()
        OkHttpClient().newCall(request).execute().close()
    }
}
