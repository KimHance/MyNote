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
                ?: return Result.failure(QuizFailure.Parse("no JSON array in output"))

        val decoded =
            runCatching { json.decodeFromString<List<RawQuestion>>(array) }
                .getOrElse { return Result.failure(QuizFailure.Parse("decode failed: ${it.message}")) }

        val questions = decoded.mapNotNull { it.toDomainOrNull() }
        return if (questions.isEmpty()) {
            Result.failure(QuizFailure.Empty("no usable question after filtering"))
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
        val trimmed = choices.map { it.trim() }
        val index =
            when (answerIndex) {
                in trimmed.indices -> answerIndex
                trimmed.size -> answerIndex - 1
                else -> return null
            }
        val answer = trimmed[index]
        if (answer.isBlank()) return null

        val unique = trimmed.filter { it.isNotBlank() }.distinctBy { it.dedupeKey() }
        if (unique.size < 2) return null
        val answerAt = unique.indexOfFirst { it.dedupeKey() == answer.dedupeKey() }
        if (answerAt < 0) return null

        // 프롬프트로 4개를 요구해도 모델이 더 뱉을 때가 있다. 앞에서 자르되 정답이
        // 뒤에 있으면 잘려 나가므로 마지막 자리를 정답에 내준다 — 정답을 0번으로
        // 몰지 않는 이유는 위치가 고정되면 읽지 않고도 맞힐 수 있기 때문이다.
        val capped =
            when {
                unique.size <= MAX_CHOICES -> unique
                answerAt < MAX_CHOICES -> unique.take(MAX_CHOICES)
                else -> unique.take(MAX_CHOICES - 1) + unique[answerAt]
            }

        return QuizQuestion(
            question = question.trim(),
            choices = capped,
            answerIndex = capped.indexOfFirst { it.dedupeKey() == answer.dedupeKey() },
            explanation = explanation.trim(),
        )
    }

    private const val MAX_CHOICES = 4

    private val whitespace = Regex("\\s+")

    private fun String.dedupeKey(): String = lowercase().replace(whitespace, " ").trim()
}
