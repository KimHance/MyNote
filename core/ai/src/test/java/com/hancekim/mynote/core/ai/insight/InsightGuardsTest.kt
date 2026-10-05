package com.hancekim.mynote.core.ai.insight

import com.hancekim.mynote.core.domain.model.InsightFailure
import com.hancekim.mynote.core.domain.model.WrongAnswer
import com.hancekim.mynote.core.domain.stats.DigestCategory
import com.hancekim.mynote.core.domain.stats.DigestTotals
import com.hancekim.mynote.core.domain.stats.WeeklyDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightGuardsTest {
    private val digest =
        WeeklyDigest(
            weekStart = LocalDate.of(2026, 9, 21),
            days = 4,
            daysLeft = 0,
            current = DigestTotals(4, 82, 1, 100),
            previous = DigestTotals(5, 85, 4, 65),
            change = DigestTotals(-1, -3, -3, 35),
            categories =
                listOf(
                    DigestCategory("android", 30, 0, 0),
                    DigestCategory("cs", 12, 0, 0),
                    DigestCategory("datastructure", 18, 3, 3),
                    DigestCategory("kotlin", 22, 0, 0),
                ),
            stalled = listOf("HTTP 캐싱 기초", "Kotlin 제네릭 (기초~심화)"),
            reread = "Navigation 3 첫인상",
        )
    private val terms = digest.titles + digest.categoryIds

    private fun check(
        headline: String,
        detail: String = "cs 노트도 열어 보세요.",
    ): Result<SummaryDraft> = runCatching { InsightGenerator.checkSummary(SummaryDraft(headline, detail, null), terms) }

    @Test
    fun `요약 문장에 숫자를 쓰면 재시도 대상으로 거절한다`() {
        val failure = check("82분을 android 노트에 사용했습니다.").exceptionOrNull()

        assertTrue(failure is InsightFailure.HasDigits)
        assertTrue(check("정답률이 지난주보다 35%p 올랐습니다.").exceptionOrNull() is InsightFailure.HasDigits)
        assertTrue(check("android 에 집중했습니다.", "남은 3일 중 하루는 cs 를 여세요.").exceptionOrNull() is InsightFailure.HasDigits)
    }

    @Test
    fun `노트 제목 안의 숫자는 숫자로 치지 않는다`() {
        assertTrue(check("android 노트를 가장 오래 읽었습니다.", "Navigation 3 첫인상을 다시 보세요.").isSuccess)
        assertTrue(check("android 노트를 가장 오래 읽었습니다.", "HTTP 캐싱 기초를 마저 읽어 보세요.").isSuccess)
    }

    @Test
    fun `숫자 없이 말로 한 요약은 통과한다`() {
        assertTrue(check("android 노트에 시간을 가장 많이 썼습니다.", "지난주보다 조금 덜 읽었습니다. cs 노트를 열어 보세요.").isSuccess)
    }

    @Test
    fun `DATA 의 키 이름이 문장에 새어 나오면 버린다`() {
        assertTrue(check("android 에 집중했습니다.", "cs 노트와 least 카테고리를 열어 보세요.").isFailure)
        assertTrue(check("android 에 집중했습니다.", "stalled 노트를 마저 읽으세요.").isFailure)
        assertTrue(check("top 카테고리에 집중했습니다.").isFailure)
        assertTrue(check("android 에 집중했습니다.", "trend 가 좋습니다.").isFailure)
    }

    @Test
    fun `한국어가 아니면 버린다`() {
        assertTrue(check("You studied android this week.", "Open cs next.").isFailure)
    }

    @Test
    fun `예시 문장을 베끼면 버린다`() {
        assertTrue(check(InsightPrompts.EXAMPLE_HEADLINE).isFailure)
        assertTrue(check("android 에 썼습니다.", InsightPrompts.EXAMPLE_DETAIL).isFailure)
    }

    @Test
    fun `예시의 틀을 빌려 이 사용자의 값으로 쓰면 베낀 것이 아니다`() {
        assertTrue(check("cs 노트에 시간을 가장 많이 썼습니다.", "지난주보다 적었습니다. HTTP 캐싱 기초를 마무리하세요.").isSuccess)
        assertTrue(check("swift 노트에 시간을 가장 많이 썼습니다.").isFailure)
        assertTrue(check("android 에 집중했습니다.", "옵셔널 체이닝을 마저 읽어 보세요.").isFailure)
    }

    private val wrong =
        listOf(
            WrongAnswer("stack", "datastructure", "스택에서 마지막에 넣은 원소가 먼저 나오는 방식은?", "FIFO", "LIFO", 1L),
            WrongAnswer("queue-deque", "datastructure", "큐에서 먼저 넣은 원소가 먼저 나오는 방식은?", "LIFO", "FIFO", 2L),
            WrongAnswer("set", "datastructure", "HashSet 의 포함 검사 평균 복잡도는?", "O(log n)", "O(1)", 3L),
            WrongAnswer("generics", "kotlin", "꺼내기만 하는 타입 파라미터에 붙이는 키워드는?", "in", "out", 4L),
        )
    private val titles = mapOf("stack" to "스택 (Stack)", "queue-deque" to "큐 & 덱 (Queue & Deque)", "set" to "집합 (Set)")
    private val source = GroundSource(InsightGenerator.patternSource(wrong, titles))
    private val answerNumbers = InsightGuards.numbersIn(source.raw)

    private fun pattern(
        tags: List<String>,
        sentence: String,
    ) = runCatching { InsightGenerator.checkPattern(PatternDraft(tags, sentence), source, answerNumbers) }

    @Test
    fun `입력 오답에 등장하고 문장에도 나온 태그만 남긴다`() {
        val draft = pattern(listOf("LIFO", "FIFO", "자료구조"), "스택과 큐에서 LIFO 와 FIFO 를 반대로 고르고 있습니다.").getOrThrow()

        assertEquals(listOf("LIFO", "FIFO"), draft.tags)
    }

    @Test
    fun `노트 제목의 영어 표기가 근거가 되어 영어 태그가 살아남는다`() {
        val draft = pattern(listOf("Stack", "Queue"), "Stack 과 Queue 의 꺼내는 순서를 반대로 고르고 있습니다.").getOrThrow()

        assertEquals(listOf("Stack", "Queue"), draft.tags)
    }

    @Test
    fun `다른 퀴즈의 태그만 남아 문장과 어긋나면 문장 핵심어로 태그를 바꾼다`() {
        val draft = pattern(listOf("set"), "스택과 큐의 동작 방식을 반대로 고르고 있습니다.").getOrThrow()

        assertEquals(listOf("스택"), draft.tags)
        assertEquals(
            listOf("스택", "큐"),
            pattern(listOf("스택", "큐", "set"), "스택과 큐의 동작 방식을 반대로 고르고 있습니다.").getOrThrow().tags,
        )
    }

    @Test
    fun `두 글자 라틴 태그는 근거로 치지 않고 세 글자부터 단어 단위로 맞춘다`() {
        val draft = pattern(listOf("in", "out"), "제네릭 키워드 in 과 out 을 혼동하고 있습니다.").getOrThrow()

        assertEquals(listOf("out"), draft.tags)
        assertFalse(InsightGuards.groundedIn("ut", source))
        assertTrue(InsightGuards.groundedIn("lifo", source))
    }

    @Test
    fun `핵심어가 하나도 겹치지 않는 일반론은 버린다`() {
        assertTrue(pattern(listOf("LIFO"), "개념을 꼼꼼히 복습하면 좋습니다.").isFailure)
    }

    @Test
    fun `태그가 다 근거 없으면 문장 핵심어가 태그가 된다`() {
        assertEquals(listOf("스택"), pattern(listOf("암기", "복습"), "스택 문제를 반대로 고릅니다.").getOrThrow().tags)
    }

    @Test
    fun `패턴 문장에 오답에 없는 숫자가 나오면 버린다`() {
        assertTrue(pattern(listOf("LIFO"), "스택 문제 3개 중 2개를 틀렸습니다.").isFailure)
        assertTrue(pattern(listOf("HashSet"), "HashSet 의 O(1) 을 O(log n) 과 헷갈립니다.").isSuccess)
    }

    @Test
    fun `예시에만 있는 낱말이 나오면 베낀 것이다`() {
        assertTrue(pattern(InsightPrompts.EXAMPLE_TAGS, "스택과 큐를 바꿔 고릅니다.").isFailure)
        assertTrue(pattern(listOf("LIFO"), "스택에서 rebase 처럼 LIFO 를 반대로 고릅니다.").isFailure)
    }

    @Test
    fun `하이픈이 든 라틴 낱말도 그대로 근거로 맞춘다`() {
        val http =
            GroundSource(
                InsightGenerator.patternSource(
                    listOf(WrongAnswer("http-cache", "cs", "Cache-Control: no-cache 의 뜻은?", "저장하지 않는다", "재검증한다", 1L)),
                    mapOf("http-cache" to "HTTP 캐싱 기초"),
                ),
            )

        assertEquals(listOf("no-cache"), InsightGuards.groundedTerms("no-cache 와 no-store 를 혼동합니다.", http))
        assertEquals(
            listOf("no-cache"),
            InsightGenerator
                .checkPattern(PatternDraft(listOf("no-cache", "재검증"), "no-cache 의 뜻을 반대로 고릅니다."), http, emptySet())
                .tags,
        )
    }

    @Test
    fun `조사를 떼고 비교한다`() {
        assertTrue(InsightGuards.grounded("스택은", source.corpus))
        assertEquals(listOf("스택", "lifo"), InsightGuards.groundedKeywords("스택에서 LIFO를 고릅니다", source.corpus))
    }
}
