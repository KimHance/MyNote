package com.hancekim.mynote.core.ai.download.decision

internal sealed interface Verdict {
    data class Append(val at: Long) : Verdict

    data object StartOver : Verdict

    data object Complete : Verdict

    data class RetryLater(val afterSeconds: Long?) : Verdict

    data class GiveUp(val reason: String) : Verdict
}

internal data class ResponseFacts(
    val status: Int,
    val contentRange: String? = null,
    val retryAfterSeconds: Long? = null,
)

internal data class ContentRange(val start: Long?, val total: Long?)

private val CONTENT_RANGE = Regex("""^bytes (?:(\d+)-(\d+)|\*)/(?:(\d+)|\*)$""")

internal fun parseContentRange(value: String?): ContentRange? {
    val match = CONTENT_RANGE.matchEntire(value?.trim().orEmpty()) ?: return null
    return ContentRange(
        start = match.groupValues[1].toLongOrNull(),
        total = match.groupValues[3].toLongOrNull(),
    )
}

internal fun judge(askedFrom: Long, facts: ResponseFacts): Verdict {
    val range = parseContentRange(facts.contentRange)
    return when (facts.status) {
        206 -> if (range?.start == askedFrom) Verdict.Append(askedFrom) else Verdict.StartOver
        200 -> if (askedFrom == 0L) Verdict.Append(0L) else Verdict.StartOver
        416 -> if (range?.total == askedFrom) Verdict.Complete else Verdict.StartOver
        408, 429, 500, 502, 503, 504 -> Verdict.RetryLater(facts.retryAfterSeconds)
        else -> Verdict.GiveUp("HTTP ${facts.status}")
    }
}
