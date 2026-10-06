package com.hancekim.mynote.core.domain.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateRepoTest {
    @Test
    fun `RC-26 값에서 생성 주소와 소개 주소를 만든다`() {
        val repo = parseTemplateRepo("a/b")

        assertEquals(TemplateRepo("a", "b"), repo)
        assertEquals("https://github.com/a/b/generate", repo?.generateUrl)
        assertEquals("https://github.com/a/b", repo?.introUrl)
    }

    @Test
    fun `RC-26 틀린 값은 모두 없음이다`() {
        val wrong =
            listOf(
                "",
                "   ",
                "a",
                "a/b/c",
                "a b/c",
                " a/b",
                "a/b ",
                "a/..",
                "a/.",
                "./b",
                "a/b.git",
                "https://github.com/a/b",
                "github.com/a/b",
                "a\"b/c",
                "a\\b/c",
                "a/b\n",
                "/b",
                "a/",
            )

        wrong.forEach { assertNull("'$it' 은 없음이어야 한다", parseTemplateRepo(it)) }
    }

    @Test
    fun `RC-26 템플릿 값이 있음이면 링크 파서도 받는다`() {
        val right = listOf("a/b", "my-org/notes-template", "A1/b.c_d", "a/.b", "a/b..c")

        right.forEach { value ->
            val template = parseTemplateRepo(value)
            assertTrue("'$value' 은 있음이어야 한다", template != null)
            val parsed = parseConnectLink(value) as ConnectLinkResult.Accepted
            assertEquals(ConnectLinkRepo(template!!.owner, template.repo), parsed.repo)
        }

        val exceptions =
            mapOf(
                "a/." to "저장소 이름이 .",
                "a/.." to "저장소 이름이 ..",
                "a/b.git" to "저장소 이름이 .git 으로 끝남",
                " a/b" to "앞 공백",
                "a/b " to "뒤 공백",
            )
        exceptions.forEach { (value, why) ->
            assertNull("$why: '$value'", parseTemplateRepo(value))
        }
    }
}
