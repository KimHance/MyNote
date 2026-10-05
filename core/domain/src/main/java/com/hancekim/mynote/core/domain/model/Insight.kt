package com.hancekim.mynote.core.domain.model

import java.time.LocalDate

data class InsightStamp(
    val modelCommit: String,
    val schemaVersion: Int,
)

data class WeeklySummary(
    val weekStart: LocalDate,
    val generatedAt: Long,
    val headline: String,
    val detail: String,
    val focus: String?,
)

data class QuizPattern(
    val weekStart: LocalDate,
    val generatedAt: Long,
    val tags: List<String>,
    val sentence: String,
)

enum class InsightResult {
    Success,
    Failure,
    Skipped,
}

enum class InsightFailureKind {
    Parse,
    Engine,
    Timeout,
}

data class InsightStatus(
    val lastResult: InsightResult? = null,
    val lastSuccessAt: Long? = null,
    val lastFailureAt: Long? = null,
    val lastFailureKind: InsightFailureKind? = null,
    val lastSkippedAt: Long? = null,
    val logEpoch: Long = 0,
)

enum class InsightWorkState {
    Idle,
    Generating,
}

data class InsightSnapshot(
    val latest: WeeklySummary?,
    val pattern: QuizPattern?,
    val lastFailureAt: Long?,
    val generating: Boolean,
)

data class WrongAnswer(
    val noteId: String,
    val category: String,
    val question: String,
    val picked: String,
    val answer: String,
    val at: Long,
)

sealed class InsightFailure(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Parse(
        reason: String,
    ) : InsightFailure("parse: $reason")

    class TooLong(
        reason: String,
    ) : InsightFailure("too long: $reason")

    class HasDigits(
        reason: String,
    ) : InsightFailure("digits: $reason")

    class MissingField(
        reason: String,
    ) : InsightFailure("missing: $reason")

    class Engine(
        cause: Throwable,
    ) : InsightFailure("engine failed to load or run", cause)

    class Timeout : InsightFailure("insight work exceeded its time budget")
}
