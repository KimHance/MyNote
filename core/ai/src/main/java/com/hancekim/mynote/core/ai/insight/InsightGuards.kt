package com.hancekim.mynote.core.ai.insight

internal class GroundSource(
    val raw: String,
) {
    val corpus: String = InsightGuards.normalize(raw)
}

internal object InsightGuards {
    private val DIGITS = Regex("\\d+")
    private val ANY_DIGIT = Regex("\\d")
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
    private val SPACES = Regex("\\s+")
    private val LEADING = Regex("^[^\\p{L}\\p{N}(]+")
    private val TRAILING = Regex("[^\\p{L}\\p{N})]+$")
    private const val MIN_KEYWORD = 2
    private const val MIN_LATIN = 3

    private val PARTICLES =
        listOf(
            "으로부터",
            "에서는",
            "으로는",
            "에게서",
            "이라는",
            "이라고",
            "라는",
            "라고",
            "에서",
            "으로",
            "에게",
            "까지",
            "부터",
            "처럼",
            "보다",
            "이나",
            "와는",
            "과는",
            "와의",
            "과의",
            "들을",
            "들이",
            "들의",
            "은",
            "는",
            "이",
            "가",
            "을",
            "를",
            "와",
            "과",
            "의",
            "에",
            "로",
            "도",
            "만",
            "나",
        )

    private val STOPWORDS =
        setOf(
            "문제",
            "정답",
            "오답",
            "선택",
            "선택지",
            "고른",
            "고르는",
            "고르고",
            "골랐",
            "경향",
            "있습니다",
            "보기",
            "질문",
            "무엇",
            "무엇인가요",
            "경우",
            "것",
            "때",
            "개념",
            "차이",
            "구분",
            "헷갈",
            "헷갈리는",
            "혼동",
            "자주",
            "특히",
            "관련",
            "대한",
            "대해",
            "같은",
            "다른",
            "모두",
            "하는",
            "합니다",
            "입니다",
            "있는",
            "없는",
            "가장",
            "주요",
            "특징",
            "역할",
            "방법",
            "이유",
            "용도",
            "의미",
            "설명",
            "올바른",
            "적절한",
            "다음",
            "중",
            "두",
            "여러",
            "반대로",
            "잘못",
            "많습니다",
            "보입니다",
            "있어요",
            "합니다만",
            "the",
            "and",
            "for",
            "방식",
            "동작",
            "이해",
            "부족",
            "부족합니다",
            "혼동",
            "혼동하고",
            "사용",
            "시점",
            "범위",
            "용어",
            "잘못",
            "잘못되었습니다",
        )

    fun numbersIn(text: String): Set<Long> = DIGITS.findAll(text).mapNotNull { it.value.toLongOrNull() }.toSet()

    fun unknownNumbers(
        text: String,
        allowed: Set<Long>,
    ): Set<Long> = numbersIn(text) - allowed

    fun normalize(text: String): String = text.lowercase().replace(NON_WORD, "")

    fun grounded(
        term: String,
        corpus: String,
    ): Boolean {
        val key = normalize(term)
        if (key.length < MIN_KEYWORD) return false
        if (corpus.contains(key)) return true
        val stem = normalize(stripParticle(term))
        return stem.length >= MIN_KEYWORD && corpus.contains(stem)
    }

    fun keywordTerms(sentence: String): List<String> =
        sentence
            .split(SPACES)
            .map { edges(stripParticle(edges(it))) }
            .filter { it.length >= MIN_KEYWORD && it.lowercase() !in STOPWORDS && !it.all(Char::isDigit) }
            .distinctBy { it.lowercase() }

    fun leaks(
        text: String,
        exampleTerms: List<String>,
        input: String,
    ): List<String> =
        exampleTerms.filter {
            text.contains(it, ignoreCase = true) &&
                !input.contains(it, ignoreCase = true)
        }

    fun same(
        output: String,
        example: String,
    ): Boolean = normalize(output).let { it.isNotEmpty() && it == normalize(example) }

    private fun edges(token: String): String = token.replace(LEADING, "").replace(TRAILING, "")

    fun keywords(sentence: String): List<String> = keywordTerms(sentence).map { it.lowercase() }

    fun latin(term: String): Boolean = term.isNotBlank() && term.all { it.code < LATIN_LIMIT }

    fun groundedIn(
        term: String,
        source: GroundSource,
    ): Boolean {
        val trimmed = term.trim()
        if (trimmed.length == 1 && trimmed[0] in '가'..'힣') return source.corpus.contains(trimmed)
        if (!latin(trimmed)) return grounded(trimmed, source.corpus)
        if (trimmed.length < MIN_LATIN) return false
        return Regex("(?i)(?<![A-Za-z0-9])" + Regex.escape(trimmed) + "(?![A-Za-z0-9])").containsMatchIn(source.raw)
    }

    fun groundedTerms(
        sentence: String,
        source: GroundSource,
    ): List<String> = keywordTerms(sentence).filter { groundedIn(it, source) }

    fun stripTerms(
        text: String,
        terms: List<String>,
    ): String =
        terms
            .filter { it.isNotBlank() }
            .sortedByDescending { it.length }
            .fold(text) { rest, term -> rest.replace(term, " ", ignoreCase = true) }

    fun hasDigit(text: String): Boolean = ANY_DIGIT.containsMatchIn(text)

    fun groundedKeywords(
        sentence: String,
        corpus: String,
    ): List<String> = keywords(sentence).filter { corpus.contains(it) }

    private const val LATIN_LIMIT = 128

    private fun stripParticle(token: String): String {
        val particle = PARTICLES.firstOrNull { token.length > it.length + 1 && token.endsWith(it) } ?: return token
        return token.dropLast(particle.length)
    }
}
