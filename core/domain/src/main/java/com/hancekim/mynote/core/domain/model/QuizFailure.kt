package com.hancekim.mynote.core.domain.model

sealed class QuizFailure(
    message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    class Parse(
        detail: String,
    ) : QuizFailure("parse: $detail")

    class Empty(
        detail: String,
    ) : QuizFailure("empty: $detail")

    class Engine(
        cause: Throwable?,
    ) : QuizFailure("engine failed to load or run", cause)
}
