package com.hancekim.mynote.core.ai.insight

import com.hancekim.mynote.core.domain.model.InsightFailure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class SummaryDraft(
    val headline: String,
    val detail: String,
    val focus: String?,
)

data class PatternDraft(
    val tags: List<String>,
    val sentence: String,
)

internal object InsightJsonParser {
    const val HEADLINE_MAX = 48
    const val DETAIL_MAX = 100
    const val SENTENCE_MAX = 120
    const val TAG_MAX = 12
    const val MAX_TAGS = 3

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val whitespace = Regex("\\s+")

    fun summary(
        raw: String,
        categories: List<String>,
    ): Result<SummaryDraft> {
        val obj = objectOf(raw) ?: return parse("no JSON object")
        val headline = obj.text("headline") ?: return missing("headline")
        val detail = obj.text("detail") ?: return missing("detail")
        if (headline.length > HEADLINE_MAX) return tooLong("headline ${headline.length}")
        if (detail.length > DETAIL_MAX) return tooLong("detail ${detail.length}")
        val focus = obj.text("focus")?.let { wanted -> categories.firstOrNull { it.equals(wanted, ignoreCase = true) } }
        return Result.success(SummaryDraft(headline, detail, focus))
    }

    fun pattern(raw: String): Result<PatternDraft> {
        val obj = objectOf(raw) ?: return parse("no JSON object")
        val sentence = obj.text("sentence") ?: return missing("sentence")
        val tags =
            (obj["tags"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content?.flat() }
                ?.filter { it.isNotEmpty() }
                ?.take(MAX_TAGS)
                .orEmpty()
        if (tags.isEmpty()) return missing("tags")
        if (sentence.length > SENTENCE_MAX) return tooLong("sentence ${sentence.length}")
        val fitting = tags.filter { it.length <= TAG_MAX }
        if (fitting.isEmpty()) return tooLong("tags ${tags.map { it.length }}")
        return Result.success(PatternDraft(fitting, sentence))
    }

    fun truncated(raw: String): Boolean {
        val start = raw.indexOf('{')
        if (start < 0) return false
        var depth = 0
        var inString = false
        var escaped = false
        for (char in raw.substring(start)) {
            when {
                escaped -> escaped = false
                inString && char == '\\' -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> depth++
                !inString && char == '}' -> depth--
            }
        }
        return depth > 0
    }

    private fun objectOf(raw: String): JsonObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { json.parseToJsonElement(raw.substring(start, end + 1)) as? JsonObject }.getOrNull()
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.flat()
            ?.takeIf { it.isNotEmpty() }

    private fun String.flat(): String = replace(whitespace, " ").trim()

    private fun <T> parse(reason: String): Result<T> = Result.failure(InsightFailure.Parse(reason))

    private fun <T> tooLong(reason: String): Result<T> = Result.failure(InsightFailure.TooLong(reason))

    private fun <T> missing(field: String): Result<T> = Result.failure(InsightFailure.MissingField(field))
}
