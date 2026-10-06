package com.hancekim.mynote.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

private val ENGINE_ENTRY = Regex("""(?m)^jetbrainsMarkdown\s*=\s*"([^"]+)"""")

private fun catalog(): File =
    generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .map { File(it, "gradle/libs.versions.toml") }
        .first { it.isFile }

class EngineVersionTest {
    @Test
    fun `NF-9 카탈로그의 엔진 판은 해석 판 번호가 기준으로 삼은 판과 같다`() {
        val catalogued = ENGINE_ENTRY.find(catalog().readText())?.groupValues?.get(1)

        assertEquals(
            "엔진 판을 바꿨으면 PARSER_VERSION 을 올리고 PARSER_ENGINE 을 맞춘다",
            PARSER_ENGINE,
            catalogued,
        )
    }
}
