package com.hancekim.mynote.core.ai

import com.hancekim.mynote.core.domain.model.QuizFailure
import com.hancekim.mynote.core.domain.model.QuizQuestion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object QuizJsonParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun parse(raw: String): Result<List<QuizQuestion>> {
        val array =
            raw
                .substringAfter('[', missingDelimiterValue = "")
                .substringBeforeLast(']', missingDelimiterValue = "")
                .let { if (it.isBlank()) null else "[$it]" }
                ?: return Result.failure(QuizFailure.Parse("JSON 배열을 찾지 못했습니다"))

        val decoded =
            runCatching { json.decodeFromString<List<RawQuestion>>(array) }
                .getOrElse { return Result.failure(QuizFailure.Parse("JSON 파싱 실패: ${it.message}")) }

        val questions = decoded.mapNotNull { it.toDomainOrNull() }
        return if (questions.isEmpty()) {
            Result.failure(QuizFailure.Empty("쓸 수 있는 문제가 없습니다"))
        } else {
            Result.success(questions)
        }
    }

    @Serializable
    private data class RawQuestion(
        val question: String = "",
        val choices: List<String> = emptyList(),
        val answerIndex: Int = -1,
        val explanation: String = "",
    )

    private fun RawQuestion.toDomainOrNull(): QuizQuestion? {
        if (question.isBlank() || choices.size < 2) return null
        val index =
            when (answerIndex) {
                in choices.indices -> answerIndex
                choices.size -> answerIndex - 1
                else -> return null
            }
        return QuizQuestion(
            question = question.trim(),
            choices = choices.map { it.trim() },
            answerIndex = index,
            explanation = explanation.trim(),
        )
    }
}
