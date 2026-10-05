package com.hancekim.mynote.core.ai.insight

import com.hancekim.mynote.core.domain.model.WrongAnswer
import com.hancekim.mynote.core.domain.stats.DigestTotals
import com.hancekim.mynote.core.domain.stats.DigestTrend
import com.hancekim.mynote.core.domain.stats.WeeklyDigest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

enum class Fix(
    val label: String,
) {
    None("none"),
    Shorter("shorter"),
    NoDigits("no-digits"),
    Complete("complete"),
}

internal object InsightPrompts {
    const val SUMMARY_SYSTEM =
        "You summarize one study week for a small card in a study app. " +
            "You reply with one JSON object and nothing else."
    const val PATTERN_SYSTEM =
        "You find one mistake pattern in a learner's wrong quiz answers. " +
            "You reply with one JSON object and nothing else."

    const val EXAMPLE_HEADLINE = "swift 노트에 시간을 가장 많이 썼습니다."
    const val EXAMPLE_DETAIL = "지난주보다 더 읽었습니다. 옵셔널 체이닝을 마저 읽고 linux 노트도 열어 보세요."
    const val EXAMPLE_SENTENCE = "rebase 와 merge 중 히스토리를 다시 쓰는 쪽을 반대로 고르고 있습니다."
    val EXAMPLE_TAGS = listOf("rebase", "히스토리")
    val EXAMPLE_SUMMARY_TERMS = listOf("swift", "linux", "옵셔널 체이닝")
    val EXAMPLE_PATTERN_TERMS = listOf("rebase", "merge", "히스토리", "리베이스", "git")

    private const val EXAMPLE_DIGEST =
        """{"week":"2025-03-03","days":2,"daysLeft":0,"notes":6,"minutes":47,"quizzes":1,"accuracy":62,""" +
            """"prev":{"notes":2,"minutes":13,"quizzes":0,"accuracy":null},""" +
            """"change":{"notes":4,"minutes":34,"quizzes":1,"accuracy":null},"trend":"more",""" +
            """"categories":[{"id":"linux","minutes":5,"quiz":null},{"id":"swift","minutes":42,"quiz":"5/8"}],""" +
            """"top":"swift","least":"linux","stalled":["옵셔널 체이닝"],"reread":null}"""

    private const val EXAMPLE_WRONG =
        "1. [git · 리베이스 기초] Q: 공유 브랜치에 써도 안전한 명령은? | picked: rebase | correct: merge\n" +
            "2. [git · 리베이스 기초] Q: 커밋 히스토리를 새로 쓰는 명령은? | picked: merge | correct: rebase"

    private const val QUESTION_CHARS = 80
    private const val CHOICE_CHARS = 40
    private const val TITLE_CHARS = 30
    private const val WRONG_INPUT_CHARS = 1_500
    private const val MIN_WRONG = 3

    fun digestJson(digest: WeeklyDigest): String =
        buildJsonObject {
            put("week", digest.weekStart.toString())
            put("days", digest.days)
            put("daysLeft", digest.daysLeft)
            putTotals(digest.current)
            put("prev", digest.previous?.let { totalsObject(it) } ?: JsonNull)
            digest.change?.let { put("change", totalsObject(it)) }
            put("trend", digest.trend?.word())
            putJsonArray("categories") {
                digest.categories.forEach { category ->
                    addJsonObject {
                        put("id", category.id)
                        put("minutes", category.minutes)
                        put("quiz", if (category.asked > 0) "${category.correct}/${category.asked}" else null)
                    }
                }
            }
            put("top", digest.top)
            put("least", digest.least)
            putJsonArray("stalled") { digest.stalled.forEach { add(it) } }
            put("reread", digest.reread)
        }.toString()

    fun summary(
        data: String,
        fix: Fix,
    ): String =
        buildString {
            appendLine("Summarize one week of a learner's study log.")
            appendLine()
            appendLine("Rules:")
            appendLine("- Do not write any digits. The app already shows every number next to your sentence.")
            appendLine("- Say amounts in words (the most, a little, more or less than last week from \"trend\").")
            appendLine("- Mention only note titles and category ids that appear in DATA.")
            appendLine(
                "- headline: one sentence, at most 40 characters, about where the study time went. Name \"top\".",
            )
            appendLine(
                "- detail: one or two sentences, at most 80 characters, with one concrete next step " +
                    "(finish a \"stalled\" note or open the \"least\" category).",
            )
            appendLine("- focus: the \"least\" id, or null when \"least\" is null.")
            appendLine("- Polite Korean (~습니다, ~하세요). Write headline and detail in KOREAN.")
            appendLine("- Output one JSON object only. No prose, no code fence.")
            when (fix) {
                Fix.None -> {
                    Unit
                }

                Fix.Shorter -> {
                    appendLine(
                        "- Your last answer was too long. headline at most 30 characters, detail at most 60.",
                    )
                }

                Fix.NoDigits -> {
                    appendLine("- Your last answer had digits. Write it again with no digits at all.")
                }

                Fix.Complete -> {
                    appendLine("- Your last answer missed a field. Output headline, detail and focus.")
                }
            }
            appendLine()
            appendLine("Example with different data. Do not copy its sentences.")
            appendLine("DATA: $EXAMPLE_DIGEST")
            appendLine(
                "OUTPUT: {\"headline\":\"$EXAMPLE_HEADLINE\",\"detail\":\"$EXAMPLE_DETAIL\",\"focus\":\"linux\"}",
            )
            appendLine()
            appendLine("DATA: $data")
            append("OUTPUT:")
        }

    fun wrongLines(
        wrong: List<WrongAnswer>,
        titles: Map<String, String>,
    ): String {
        val lines =
            wrong.mapIndexed { index, answer ->
                val where =
                    titles[answer.noteId]?.let { "${answer.category} · ${it.clip(TITLE_CHARS)}" } ?: answer.category
                "${index + 1}. [$where] Q: ${answer.question.clip(QUESTION_CHARS)} | " +
                    "picked: ${answer.picked.clip(CHOICE_CHARS)} | correct: ${answer.answer.clip(CHOICE_CHARS)}"
            }
        var kept = lines
        while (kept.size > MIN_WRONG && kept.sumOf { it.length + 1 } > WRONG_INPUT_CHARS) kept = kept.dropLast(1)
        return kept.joinToString("\n")
    }

    fun pattern(
        lines: String,
        fix: Fix,
    ): String =
        buildString {
            appendLine("Find one pattern in a learner's wrong quiz answers.")
            appendLine()
            appendLine("Rules:")
            appendLine("- Compare each wrong pick with the correct answer. Find what the mistakes have in common.")
            appendLine("- tags: 1 to 3 keywords, each at most 12 characters.")
            appendLine(
                "- Copy each tag exactly as written in WRONG, in the same language and spelling. Do not translate.",
            )
            appendLine(
                "- sentence: one sentence, at most 100 characters, naming the confusion. Use your tags in the sentence.",
            )
            appendLine("- No general study advice. Never invent numbers.")
            appendLine("- Polite Korean (~습니다). Write the sentence in KOREAN.")
            appendLine("- Output one JSON object only. No prose, no code fence.")
            if (fix == Fix.Shorter) {
                appendLine(
                    "- Your last answer was too long. sentence at most 70 characters, tags at most 8 characters.",
                )
            }
            appendLine()
            appendLine("Example with different data. Do not copy its sentences.")
            appendLine("WRONG:")
            appendLine(EXAMPLE_WRONG)
            appendLine(
                "OUTPUT: {\"tags\":[\"rebase\",\"히스토리\"],\"sentence\":\"$EXAMPLE_SENTENCE\"}",
            )
            appendLine()
            appendLine("WRONG:")
            appendLine(lines)
            append("OUTPUT:")
        }

    private fun DigestTrend.word(): String =
        when (this) {
            DigestTrend.More -> "more"
            DigestTrend.Less -> "less"
            DigestTrend.Same -> "same"
        }

    private fun JsonObjectBuilder.putTotals(totals: DigestTotals) {
        put("notes", totals.notes)
        put("minutes", totals.minutes)
        put("quizzes", totals.quizzes)
        put("accuracy", totals.accuracy)
    }

    private fun totalsObject(totals: DigestTotals): JsonObject = buildJsonObject { putTotals(totals) }

    private fun String.clip(max: Int): String {
        val flat = replace(WHITESPACE, " ").trim()
        return if (flat.length <= max) flat else flat.take(max - 1) + "…"
    }

    private val WHITESPACE = Regex("\\s+")
}
