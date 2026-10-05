package com.hancekim.mynote.core.ai.insight

import android.util.Log
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.hancekim.mynote.core.ai.backendGuess
import com.hancekim.mynote.core.ai.benchmarkOrNull
import com.hancekim.mynote.core.ai.describe
import com.hancekim.mynote.core.ai.sendCancellable
import com.hancekim.mynote.core.ai.text
import com.hancekim.mynote.core.domain.model.InsightFailure
import com.hancekim.mynote.core.domain.model.WrongAnswer
import com.hancekim.mynote.core.domain.stats.WeeklyDigest
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

data class InsightAttempt(
    val fix: Fix,
    val promptChars: Int,
    val millis: Long,
    val raw: String,
    val outputTokens: Int?,
    val ttftSeconds: Double?,
    val backend: String?,
    val verdict: String,
)

data class InsightRun<T>(
    val value: T?,
    val failure: InsightFailure?,
    val attempts: List<InsightAttempt>,
)

class InsightGenerator @Inject constructor() {
    suspend fun summary(
        engine: Engine,
        digest: WeeklyDigest,
        maxTokens: Int = SUMMARY_TOKENS,
    ): InsightRun<SummaryDraft> {
        val data = InsightPrompts.digestJson(digest)
        val terms = digest.titles + digest.categoryIds
        return run(
            engine = engine,
            system = InsightPrompts.SUMMARY_SYSTEM,
            maxTokens = maxTokens,
            prompt = { fix -> InsightPrompts.summary(data, fix) },
        ) { raw ->
            InsightJsonParser.summary(raw, digest.categoryIds).mapCatching { checkSummary(it, terms) }
        }
    }

    suspend fun pattern(
        engine: Engine,
        wrong: List<WrongAnswer>,
        titles: Map<String, String> = emptyMap(),
        maxTokens: Int = PATTERN_TOKENS,
    ): InsightRun<PatternDraft> {
        val lines = InsightPrompts.wrongLines(wrong, titles)
        val source = GroundSource(patternSource(wrong, titles))
        val allowed = InsightGuards.numbersIn(source.raw)
        return run(
            engine = engine,
            system = InsightPrompts.PATTERN_SYSTEM,
            maxTokens = maxTokens,
            prompt = { fix -> InsightPrompts.pattern(lines, fix) },
        ) { raw ->
            InsightJsonParser.pattern(raw).mapCatching { checkPattern(it, source, allowed) }
        }
    }

    private suspend fun <T> run(
        engine: Engine,
        system: String,
        maxTokens: Int,
        prompt: (Fix) -> String,
        check: (String) -> Result<T>,
    ): InsightRun<T> {
        val attempts = mutableListOf<InsightAttempt>()
        var fix = Fix.None
        while (true) {
            val text = prompt(fix)
            val started = System.currentTimeMillis()
            val reply =
                try {
                    send(engine, system, maxTokens, text)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (cause: Exception) {
                    return InsightRun(null, InsightFailure.Engine(cause), attempts)
                }
            val millis = System.currentTimeMillis() - started
            val result = check(reply.raw)
            val failure =
                when {
                    result.isSuccess -> {
                        null
                    }

                    InsightJsonParser.truncated(reply.raw) -> {
                        InsightFailure.TooLong("truncated")
                    }

                    else -> {
                        result.exceptionOrNull() as? InsightFailure
                            ?: InsightFailure.Parse(result.exceptionOrNull()?.message.orEmpty())
                    }
                }
            val verdict = failure?.message ?: "ok"
            attempts +=
                InsightAttempt(
                    fix,
                    text.length,
                    millis,
                    reply.raw,
                    reply.outputTokens,
                    reply.ttft,
                    reply.backend,
                    verdict,
                )
            Log.i(TAG, "인사이트 생성 ${millis}ms · ${reply.raw.length}자 · ${reply.bench} · 판정 $verdict")
            if (failure == null) return InsightRun(result.getOrNull(), null, attempts)
            val next = failure.retryFix()
            if (next == null || fix != Fix.None) {
                val final = if (next != null) InsightFailure.Parse("still ${failure.message} after retry") else failure
                return InsightRun(null, final, attempts)
            }
            fix = next
        }
    }

    private fun InsightFailure.retryFix(): Fix? =
        when (this) {
            is InsightFailure.TooLong -> Fix.Shorter
            is InsightFailure.HasDigits -> Fix.NoDigits
            is InsightFailure.MissingField -> Fix.Complete
            else -> null
        }

    private suspend fun send(
        engine: Engine,
        system: String,
        maxTokens: Int,
        prompt: String,
    ): Reply =
        engine.createConversation(config(system, maxTokens)).use { conversation ->
            val raw = conversation.sendCancellable(prompt).text()
            val bench = conversation.benchmarkOrNull()
            Reply(
                raw = raw,
                outputTokens = bench?.lastDecodeTokenCount,
                ttft = bench?.timeToFirstTokenInSecond,
                backend = bench?.backendGuess(),
                bench = bench?.describe() ?: "벤치마크 꺼짐",
            )
        }

    private fun config(
        system: String,
        maxTokens: Int,
    ) = ConversationConfig(
        systemInstruction = Contents.of(system),
        thinkingConfig = ThinkingConfig(enableThinking = false),
        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
        maxOutputToken = maxTokens,
    )

    private class Reply(
        val raw: String,
        val outputTokens: Int?,
        val ttft: Double?,
        val backend: String?,
        val bench: String,
    )

    companion object {
        const val SUMMARY_TOKENS = 69
        const val PATTERN_TOKENS = 50
        private const val TAG = "MyNoteAi"
        private const val MAX_TAGS = 3
        private const val TAG_MAX = 12
        private val HANGUL = Regex("[가-힣]")
        private val LEAKED_KEY =
            Regex(
                "(?i)(?<![a-z])(least|top|stalled|reread|daysLeft|prev|change|trend|minutes|quizzes|accuracy)(?![a-z])",
            )

        internal fun patternSource(
            wrong: List<WrongAnswer>,
            titles: Map<String, String>,
        ): String =
            wrong.joinToString("\n") { answer ->
                listOfNotNull(answer.category, titles[answer.noteId], answer.question, answer.picked, answer.answer)
                    .joinToString(" ")
            }

        internal fun checkSummary(
            draft: SummaryDraft,
            terms: List<String>,
        ): SummaryDraft {
            val text = "${draft.headline} ${draft.detail}"
            if (!HANGUL.containsMatchIn(text)) throw InsightFailure.Parse("not korean")
            val bare = InsightGuards.stripTerms(text, terms)
            LEAKED_KEY.find(bare)?.let { throw InsightFailure.Parse("leaked key ${it.value}") }
            val leaked = InsightGuards.leaks(text, InsightPrompts.EXAMPLE_SUMMARY_TERMS, terms.joinToString(" "))
            if (leaked.isNotEmpty() ||
                InsightGuards.same(draft.headline, InsightPrompts.EXAMPLE_HEADLINE) ||
                InsightGuards.same(draft.detail, InsightPrompts.EXAMPLE_DETAIL)
            ) {
                throw InsightFailure.Parse("copied example $leaked")
            }
            if (InsightGuards.hasDigit(bare)) throw InsightFailure.HasDigits("outside titles")
            return draft
        }

        internal fun checkPattern(
            draft: PatternDraft,
            source: GroundSource,
            allowed: Set<Long>,
        ): PatternDraft {
            if (!HANGUL.containsMatchIn(draft.sentence)) throw InsightFailure.Parse("not korean")
            val unknown = InsightGuards.unknownNumbers("${draft.sentence} ${draft.tags.joinToString(" ")}", allowed)
            if (unknown.isNotEmpty()) throw InsightFailure.Parse("numbers not in answers $unknown")
            val leaked =
                InsightGuards.leaks(
                    "${draft.sentence} ${draft.tags.joinToString(" ")}",
                    InsightPrompts.EXAMPLE_PATTERN_TERMS,
                    source.raw,
                )
            if (leaked.isNotEmpty() || InsightGuards.same(draft.sentence, InsightPrompts.EXAMPLE_SENTENCE)) {
                throw InsightFailure.Parse("copied example $leaked")
            }
            val anchors = InsightGuards.groundedTerms(draft.sentence, source)
            if (anchors.isEmpty()) throw InsightFailure.Parse("ungrounded sentence")
            val sentence = GroundSource(draft.sentence)
            val tags =
                draft.tags
                    .filter { InsightGuards.groundedIn(it, source) }
                    .filter { InsightGuards.groundedIn(it, sentence) }
                    .ifEmpty { anchors.filter { it.length <= TAG_MAX }.take(MAX_TAGS) }
            if (tags.isEmpty()) throw InsightFailure.Parse("no tag shared by sentence and answers ${draft.tags}")
            return draft.copy(tags = tags)
        }
    }
}
