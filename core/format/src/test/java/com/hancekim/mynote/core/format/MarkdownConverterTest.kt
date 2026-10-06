package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.TableAlign
import com.hancekim.mynote.core.domain.model.TableCell
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private fun t(text: String) = NoteSpan(text)

private fun b(text: String) = NoteSpan(text, SpanStyle.Bold)

private fun c(text: String) = NoteSpan(text, SpanStyle.Code)

private fun i(text: String) = NoteSpan(text, italic = true)

private fun s(text: String) = NoteSpan(text, strike = true)

private fun p(vararg spans: NoteSpan) = NoteBlock.Paragraph(spans.toList())

private fun item(
    vararg spans: NoteSpan,
    checked: Boolean? = null,
    children: List<NoteBlock> = emptyList(),
) = NoteListItem(spans.toList(), checked, children)

private fun cell(vararg spans: NoteSpan) = TableCell(spans.toList())

private fun limitNote(raw: String) =
    listOf(
        NoteBlock.Unsupported(UnsupportedReason.LimitExceeded, type = null, text = null, raw = raw),
    )

private fun depthOf(blocks: List<NoteBlock>): Int =
    blocks.maxOfOrNull { block ->
        val items =
            when (block) {
                is NoteBlock.BulletList -> block.items
                is NoteBlock.OrderedList -> block.items
                else -> emptyList()
            }
        1 + depthOf(items.flatMap { it.children })
    } ?: 0

private fun hasBox(blocks: List<NoteBlock>): Boolean =
    blocks.any { block ->
        when (block) {
            is NoteBlock.Unsupported -> true
            is NoteBlock.BulletList -> block.items.any { hasBox(it.children) }
            is NoteBlock.OrderedList -> block.items.any { hasBox(it.children) }
            else -> false
        }
    }

private fun indentedList(levels: Int): String = (0 until levels).joinToString("\n") { "  ".repeat(it) + "- x" }

private fun lazyList(levels: Int): String =
    (0 until levels).joinToString("\n") { "  ".repeat(it) + "- x\n1234567890) 이어지는 글" }

private fun frontMatterBundle(): File =
    generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .map { File(it, "notes-tests/fixtures/front-matter") }
        .first { File(it, "cases.json").isFile }

class MarkdownConverterTest {
    private val format = NoteFormat()

    private fun md(raw: String): List<NoteBlock> = format.parse("x.md", raw)

    private fun assertTable(
        cases: List<Pair<String, List<NoteBlock>>>,
    ) {
        cases.forEach { (raw, expected) -> assertEquals(raw, expected, md(raw)) }
    }

    @Test
    fun `NF-1 앞머리는 블록이 되지 않는다`() {
        val raw = "---\ntitle: 제목\ndate: 2026.10.07\n---\n# 본문\n\n문단"

        assertEquals(listOf(NoteBlock.Heading(1, listOf(t("본문"))), p(t("문단"))), md(raw))
    }

    @Test
    fun `NF-1 CRLF 앞머리도 블록이 되지 않는다`() {
        val lf = "---\ntitle: 제목\ndate: 2026.10.07\n---\n# 본문\n\n문단"

        assertEquals(md(lf), md(lf.replace("\n", "\r\n")))
        assertEquals(md(lf), md("\uFEFF" + lf.replace("\n", "\r\n")))
        assertEquals(md(lf), md(lf.replace("\n", "\r")))
    }

    @Test
    fun `NF-1 CRLF 본문은 LF 본문과 같은 블록이다`() {
        val lf = "a\nb  \nc\n\n- x\n  - y\n\n```\nval a = 1\n\nval b = 2\n```\n\n> q\n> r\n\n| h |\n|---|\n| 1 |\n"
        val expected = md(lf)

        assertEquals(5, expected.size)
        assertEquals(expected, md(lf.replace("\n", "\r\n")))
        assertEquals(expected, md(lf.replace("\n", "\r")))
    }

    @Test
    fun `NF-1 닫히지 않은 앞머리는 본문이다`() {
        assertEquals(
            listOf(NoteBlock.Divider, p(t("title: 제목")), NoteBlock.Heading(1, listOf(t("본문")))),
            md("---\ntitle: 제목\n# 본문"),
        )
    }

    @Test
    fun `NF-1 앞머리 경계 묶음`() {
        val bundle = frontMatterBundle()
        val cases = Json.parseToJsonElement(File(bundle, "cases.json").readText()).jsonArray.map { it.jsonObject }
        val files =
            bundle
                .listFiles()
                .orEmpty()
                .filter { it.extension == "md" }
                .map { it.name }

        assertTrue("앞머리 경계 묶음이 비었다", cases.isNotEmpty())
        assertTrue(cases.any { it.frontMatter() })
        assertTrue(cases.any { !it.frontMatter() })
        assertEquals(files.toSet(), cases.map { it.file() }.toSet())
        cases.forEach { case ->
            val raw = File(bundle, case.file()).readBytes().decodeToString()
            val source = markdownSource(raw)
            val body = case.getValue("body").jsonPrimitive.content

            assertEquals(case.file(), case.frontMatter(), source.frontMatter)
            assertEquals(case.file(), body, source.body)
            assertEquals(case.file(), md(body), md(raw))
        }
    }

    @Test
    fun `NF-3 지금 블록으로 가는 문법 표`() {
        val bullet = NoteBlock.BulletList(listOf(item(t("a"))))
        assertTable(
            listOf(
                "# a" to listOf(NoteBlock.Heading(1, listOf(t("a")))),
                "#### a" to listOf(NoteBlock.Heading(4, listOf(t("a")))),
                "###### a" to listOf(NoteBlock.Heading(6, listOf(t("a")))),
                "a\n===" to listOf(NoteBlock.Heading(1, listOf(t("a")))),
                "a\n---" to listOf(NoteBlock.Heading(2, listOf(t("a")))),
                "a" to listOf(p(t("a"))),
                "```kotlin\nval x = 1\n  y\nz\n```" to listOf(NoteBlock.Code("val x = 1\n  y\nz")),
                "    a\n      b" to listOf(NoteBlock.Code("a\n  b")),
                "> a\n>\n> b" to listOf(NoteBlock.Quote(listOf(t("a\nb")))),
                "> - x" to listOf(NoteBlock.Quote(listOf(t("x")))),
                "- a" to listOf(bullet),
                "* a" to listOf(bullet),
                "+ a" to listOf(bullet),
                "1. a" to listOf(NoteBlock.OrderedList(listOf(item(t("a"))))),
                "1) a" to listOf(NoteBlock.OrderedList(listOf(item(t("a"))))),
                "---" to listOf(NoteBlock.Divider),
                "***" to listOf(NoteBlock.Divider),
                "___" to listOf(NoteBlock.Divider),
                "\n\n\n" to emptyList(),
            ),
        )
    }

    @Test
    fun `NF-3 인용 안 문단이 아닌 것은 그 글자를 줄로 잇는다`() {
        fun quote(vararg spans: NoteSpan) = listOf(NoteBlock.Quote(spans.toList()))

        assertTable(
            listOf(
                "> # h\n> 문단" to quote(t("h\n문단")),
                "> ## **굵게** 제목" to quote(b("굵게"), t(" 제목")),
                "> - a\n>   - b\n> 1. c" to quote(t("a\nb\nc")),
                "> ```\n> x\n>   y\n>\n> z\n> ```" to quote(t("x\n  y\n\nz")),
                ">     a\n>       b" to quote(t("a\n  b")),
                "> <div>\n>   <p>x</p>\n> </div>" to quote(t("<div>\n  <p>x</p>\n</div>")),
                "> | h1 | h2 |\n> |---|---|\n> | a | b |" to quote(t("h1 | h2\na | b")),
                "> a\n>\n> ---\n>\n> b" to quote(t("a\nb")),
                "> [!NOTE]\n> a" to quote(t("[!NOTE]\na")),
                "> ```\n> ```\n> a" to quote(t("a")),
            ),
        )
    }

    @Test
    fun `NF-3 닫히지 않은 펜스도 마지막 줄바꿈이 없다`() {
        assertTable(
            listOf(
                "```\na\n" to listOf(NoteBlock.Code("a")),
                "```\na" to listOf(NoteBlock.Code("a")),
                "```\na\n\nb\n" to listOf(NoteBlock.Code("a\n\nb")),
                "```\na\n```\n" to listOf(NoteBlock.Code("a")),
                "- x\n\n  ```\n  y\n" to
                    listOf(NoteBlock.BulletList(listOf(item(t("x"), children = listOf(NoteBlock.Code("y")))))),
                "> ```\n> y\n" to listOf(NoteBlock.Quote(listOf(t("y")))),
            ),
        )
    }

    @Test
    fun `NF-4 글자 모양 표`() {
        assertTable(
            listOf(
                "**a**" to listOf(p(b("a"))),
                "__a__" to listOf(p(b("a"))),
                "`a`" to listOf(p(c("a"))),
                "*a*" to listOf(p(i("a"))),
                "_a_" to listOf(p(i("a"))),
                "~~a~~" to listOf(p(s("a"))),
                "~a~" to listOf(p(s("a"))),
                "x ~a~ y" to listOf(p(t("x "), s("a"), t(" y"))),
                "a*b*c" to listOf(p(t("a"), i("b"), t("c"))),
                "a\nb" to listOf(p(t("a b"))),
                "a  \nb" to listOf(p(t("a\nb"))),
                "a\\\nb" to listOf(p(t("a\nb"))),
            ),
        )
    }

    @Test
    fun `NF-4 겹친 모양`() {
        assertTable(
            listOf(
                "***a***" to listOf(p(NoteSpan("a", SpanStyle.Bold, italic = true))),
                "**a ~~b~~**" to listOf(p(b("a "), NoteSpan("b", SpanStyle.Bold, strike = true))),
                "**a `b`**" to listOf(p(b("a "), c("b"))),
                "*a `b`*" to listOf(p(i("a "), c("b"))),
                "~~a `b`~~" to listOf(p(s("a "), NoteSpan("b", SpanStyle.Code, strike = true))),
                "> *a*" to listOf(NoteBlock.Quote(listOf(i("a")))),
            ),
        )
    }

    @Test
    fun `NF-4 코드 글자는 원문 그대로다`() {
        assertTable(
            listOf(
                "`a*b*c`" to listOf(p(c("a*b*c"))),
                "`List<T>`" to listOf(p(c("List<T>"))),
                "`&amp;`" to listOf(p(c("&amp;"))),
                "`\\*`" to listOf(p(c("\\*"))),
                "`` a`b ``" to listOf(p(c("a`b"))),
                "`a\nb`" to listOf(p(c("a b"))),
            ),
        )
    }

    @Test
    fun `NF-5 표 블록`() {
        val raw =
            """
            | h1 | h2 | h3 |
            |:--|:-:|--:|
            | **a** | b |
            | 1 | 2 | 3 | 4 |
            | x \| y | `p\|q` | z |
            """.trimIndent()

        assertEquals(
            listOf(
                NoteBlock.Table(
                    header = listOf(cell(t("h1")), cell(t("h2")), cell(t("h3"))),
                    rows =
                        listOf(
                            listOf(cell(b("a")), cell(t("b")), cell()),
                            listOf(cell(t("1")), cell(t("2")), cell(t("3"))),
                            listOf(cell(t("x | y")), cell(c("p|q")), cell(t("z"))),
                        ),
                    align = listOf(TableAlign.Left, TableAlign.Center, TableAlign.Right),
                ),
            ),
            md(raw),
        )
        assertEquals(
            listOf(NoteBlock.Table(listOf(cell(t("h"))), emptyList(), listOf(TableAlign.None))),
            md("| h |\n|---|"),
        )
    }

    @Test
    fun `NF-6 하위 블록`() {
        assertEquals(
            listOf(
                NoteBlock.BulletList(
                    listOf(
                        item(
                            t("a"),
                            children =
                                listOf(
                                    NoteBlock.BulletList(
                                        listOf(
                                            item(
                                                t("b"),
                                                children = listOf(NoteBlock.OrderedList(listOf(item(t("c"))))),
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
                ),
            ),
            md("- a\n  - b\n    1. c"),
        )

        val raw = "- a\n\n  둘째\n\n  ```\n  x\n    y\n  ```\n\n  > q\n\n  | h |\n  |---|\n  | 1 |"
        assertEquals(
            listOf(
                NoteBlock.BulletList(
                    listOf(
                        item(
                            t("a"),
                            children =
                                listOf(
                                    p(t("둘째")),
                                    NoteBlock.Code("x\n  y"),
                                    NoteBlock.Quote(listOf(t("q"))),
                                    NoteBlock.Table(
                                        listOf(cell(t("h"))),
                                        listOf(listOf(cell(t("1")))),
                                        listOf(TableAlign.None),
                                    ),
                                ),
                        ),
                    ),
                ),
            ),
            md(raw),
        )
    }

    @Test
    fun `NF-6 첫 자식이 문단이 아닌 항목`() {
        assertEquals(
            listOf(
                NoteBlock.BulletList(
                    listOf(
                        item(),
                        item(children = listOf(NoteBlock.Heading(1, listOf(t("h"))))),
                        item(children = listOf(NoteBlock.Code("x"))),
                    ),
                ),
            ),
            md("-\n- # h\n- ```\n  x\n  ```"),
        )
    }

    @Test
    fun `NF-6 체크 항목`() {
        assertEquals(
            listOf(
                NoteBlock.BulletList(
                    listOf(item(t("a"), checked = true), item(t("b"), checked = false), item(t("c"), checked = true)),
                ),
            ),
            md("- [x] a\n- [ ] b\n- [X] c"),
        )
        assertEquals(
            listOf(NoteBlock.OrderedList(listOf(item(t("a"), checked = true), item(t("b"), checked = false)))),
            md("1. [x] a\n1. [ ] b"),
        )
    }

    @Test
    fun `NF-6 시작 번호`() {
        assertEquals(listOf(NoteBlock.OrderedList(listOf(item(t("a")), item(t("b"))), start = 3)), md("3. a\n4. b"))
        assertEquals(listOf(NoteBlock.OrderedList(listOf(item(t("a")), item(t("b"))), start = 9)), md("9) a\n10) b"))
    }

    @Test
    fun `NF-7 글자로 보이는 문법 표`() {
        val cases =
            listOf(
                "[문서](https://x)" to listOf(p(t("문서"))),
                "[**굵은** 링크](https://x)" to listOf(p(b("굵은"), t(" 링크"))),
                "[참조][r] 와 [r]\n\n[r]: https://y" to listOf(p(t("참조 와 r"))),
                "![그림](a.png)" to listOf(p(t("그림"))),
                "![](a.png)" to emptyList(),
                "<https://x>" to listOf(p(t("https://x"))),
                "<a@b.com>" to listOf(p(t("a@b.com"))),
                "https://x.com 와 www.y.com" to listOf(p(t("https://x.com 와 www.y.com"))),
                "**Box<String>**" to listOf(p(b("Box<String>"))),
                "a <br> b" to listOf(p(t("a <br> b"))),
                "<details>\n<summary>s</summary>\nbody\n</details>" to
                    listOf(NoteBlock.Code("<details>\n<summary>s</summary>\nbody\n</details>")),
                "<details>\n\nmid\n\n</details>" to
                    listOf(NoteBlock.Code("<details>"), p(t("mid")), NoteBlock.Code("</details>")),
                "가격 \$5 와 \$10" to listOf(p(t("가격 \$5 와 \$10"))),
                "\$x\$" to listOf(p(t("\$x\$"))),
                "\$\$\nx\n\$\$" to listOf(p(t("\$\$ x \$\$"))),
                "==a==" to listOf(p(t("==a=="))),
                "> [!NOTE]\n> a" to listOf(NoteBlock.Quote(listOf(t("[!NOTE]\na")))),
                "\\*a\\*" to listOf(p(t("*a*"))),
                "&amp; &quot; &#65; &bogus;" to listOf(p(t("& \" A &bogus;"))),
                "&#x1F600;" to listOf(p(t("\uD83D\uDE00"))),
            )

        assertTable(cases)
        cases.forEach { (raw, _) -> assertFalse(raw, hasBox(md(raw))) }
    }

    @Test
    fun `NF-8 32단은 읽고 33단은 한도 넘음이다`() {
        val quote32 = "> ".repeat(32) + "x"
        assertEquals(listOf(NoteBlock.Quote(listOf(t("x")))), md(quote32))
        assertEquals(32, depthOf(md("- ".repeat(32) + "x")))
        assertEquals(32, depthOf(md(indentedList(32))))
        assertFalse(hasBox(md("> - ".repeat(16) + "x")))

        listOf(
            "> ".repeat(33) + "x",
            "- ".repeat(33) + "x",
            "> - ".repeat(16) + "> x",
            indentedList(33),
            "> - ".repeat(5_000),
            ">".repeat(20_000),
            indentedList(1_000),
        ).forEach { raw -> assertEquals(raw.take(20), limitNote(raw), md(raw)) }
    }

    @Test
    fun `NF-8 512KB 를 넘으면 한도 넘음이다`() {
        val fits = "a".repeat(524_288)
        val over = "a".repeat(524_289)

        assertEquals(listOf(p(t(fits))), md(fits))
        assertEquals(limitNote(over), md(over))
    }

    @Test
    fun `NF-8 변환 결과의 블록 깊이는 32 를 넘지 않는다`() {
        listOf(33, 40, 64).forEach { levels ->
            val raw = lazyList(levels)

            assertFalse("$levels", exceedsMarkdownLimits(raw))
            assertEquals("$levels", limitNote(raw), md(raw))
        }
        assertEquals(32, depthOf(md(lazyList(32))))

        val small = GfmMarkdownConverter(NoteLimits(maxDepth = 2, maxKilobytes = 512))
        assertEquals(2, depthOf(small.convert("- a\n  - b")))
        assertEquals(limitNote("- a\n  - b\n    - c"), small.convert("- a\n  - b\n    - c"))
    }

    @Test
    fun `NF-8 깊은 강조와 괄호도 상자 없이 읽힌다`() {
        listOf(
            "*".repeat(20_000) + "a" + "*".repeat(20_000),
            "[".repeat(5_000),
            "~".repeat(2) + "a".repeat(10) + "~".repeat(2),
        ).forEach { raw ->
            val blocks = md(raw)

            assertTrue(raw.take(20), blocks.isNotEmpty())
            assertFalse(raw.take(20), hasBox(blocks))
        }
    }
}

private fun kotlinx.serialization.json.JsonObject.file(): String = getValue("file").jsonPrimitive.content

private fun kotlinx.serialization.json.JsonObject.frontMatter(): Boolean = getValue("frontMatter").jsonPrimitive.boolean
