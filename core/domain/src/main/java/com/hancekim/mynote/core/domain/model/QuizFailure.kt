package com.hancekim.mynote.core.domain.model

sealed class QuizFailure(
    message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    class Parse(
        detail: String,
    ) : QuizFailure(detail)

    class Empty(
        detail: String,
    ) : QuizFailure(detail)

    class Engine(
        cause: Throwable?,
    ) : QuizFailure("모델을 불러오지 못했습니다", cause)
}
