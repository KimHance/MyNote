package com.hancekim.mynote.insight

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.hancekim.mynote.core.ai.EngineUse
import com.hancekim.mynote.core.ai.ModelEngine
import com.hancekim.mynote.core.ai.insight.InsightAttempt
import com.hancekim.mynote.core.ai.insight.InsightGenerator
import com.hancekim.mynote.core.ai.insight.InsightRun
import com.hancekim.mynote.core.ai.insight.PatternDraft
import com.hancekim.mynote.core.ai.insight.SummaryDraft
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class InsightEvalTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val args = InstrumentationRegistry.getArguments()

    @Test
    fun `실제 모델로 주간 요약 10종과 패턴 6종을 평가한다`() =
        runBlocking {
            val round = args.getString("evalRound")
            assumeTrue("evalRound 인자를 줄 때만 돈다", round != null)
            val engine = ModelEngine(context)
            assumeTrue("모델 파일이 없는 기기", engine.isAvailable())
            WorkManager
                .getInstance(context)
                .cancelUniqueWork("study-insights")
                .result
                .get()

            val summaryTokens = args.getString("summaryTokens")?.toInt() ?: InsightGenerator.SUMMARY_TOKENS
            val patternTokens = args.getString("patternTokens")?.toInt() ?: InsightGenerator.PATTERN_TOKENS
            val generator = InsightGenerator()
            val summaries = JSONArray()
            val patterns = JSONArray()
            val extraSummaries = JSONArray()
            val extraPatterns = JSONArray()
            val calibration = JSONArray()
            val started = System.currentTimeMillis()

            val use =
                engine.tryUse(shouldRelease = { true }) { ready ->
                    digestCases.forEach { case ->
                        val run = generator.summary(ready, case.digest, summaryTokens)
                        summaries.put(summaryCase(case, run))
                        Log.i(TAG, "요약 ${case.id} · ${run.failure?.message ?: "ok"}")
                    }
                    extraDigestCases.forEach { case ->
                        extraSummaries.put(summaryCase(case, generator.summary(ready, case.digest, summaryTokens)))
                    }
                    if (args.getString("calibrate") == "true") {
                        listOf(digestCases[0], digestCases[4]).forEach { case ->
                            calibration.put(
                                calibrationJson(case.id, generator.summary(ready, case.digest, CALIBRATION_TOKENS)),
                            )
                        }
                        calibration.put(
                            calibrationJson(
                                wrongCases[1].id,
                                generator.pattern(ready, wrongCases[1].wrong, noteTitles, CALIBRATION_TOKENS),
                            ),
                        )
                    }
                    wrongCases.forEach { case ->
                        val run = generator.pattern(ready, case.wrong, noteTitles, patternTokens)
                        patterns.put(patternCase(case, run))
                        Log.i(TAG, "패턴 ${case.id} · ${run.failure?.message ?: "ok"}")
                    }
                    extraWrongCases.forEach { case ->
                        extraPatterns.put(
                            patternCase(case, generator.pattern(ready, case.wrong, noteTitles, patternTokens)),
                        )
                    }
                }
            val done = use as EngineUse.Done
            val summaryPass = (0 until summaries.length()).count { summaries.getJSONObject(it).getBoolean("pass") }
            val patternPass = (0 until patterns.length()).count { patterns.getJSONObject(it).getBoolean("pass") }
            val callMillis =
                (summaries.attempts() + patterns.attempts()).map { it.getLong("millis") }.sorted()
            val p95 = callMillis[((callMillis.size - 1) * P95).toInt()]
            val summaryDigits =
                (0 until summaries.length()).sumOf {
                    summaries
                        .getJSONObject(
                            it,
                        ).getInt("digitsOutsideTitles")
                }
            val firstTryDigits =
                (0 until summaries.length()).count {
                    summaries.getJSONObject(it).getInt("firstRawDigits") >
                        0
                }
            val tagMismatch =
                (0 until patterns.length()).count {
                    !patterns
                        .getJSONObject(
                            it,
                        ).getBoolean("tagsInSentence")
                }

            val report =
                JSONObject()
                    .put("round", round)
                    .put("device", android.os.Build.MODEL)
                    .put("summaryTokens", summaryTokens)
                    .put("patternTokens", patternTokens)
                    .put("initMillis", done.initMillis ?: 0L)
                    .put("released", done.released)
                    .put("totalMillis", System.currentTimeMillis() - started)
                    .put("summaryPass", summaryPass)
                    .put("patternPass", patternPass)
                    .put("callP95Millis", p95)
                    .put("summaryDigitsOutsideTitles", summaryDigits)
                    .put("summaryFirstTryWithDigits", firstTryDigits)
                    .put("patternTagSentenceMismatch", tagMismatch)
                    .put("summaries", summaries)
                    .put("patterns", patterns)
                    .put("extraSummaries", extraSummaries)
                    .put("extraPatterns", extraPatterns)
                    .put("calibration", calibration)
            val out = File(context.getExternalFilesDir(EVAL_DIR), "eval-round$round.json")
            out.writeText(report.toString(2))
            Log.i(
                TAG,
                "평가 끝 · round $round · 요약 $summaryPass/10 · 패턴 $patternPass/6 · p95 ${p95}ms · " +
                    "요약 숫자 $summaryDigits · 첫 시도 숫자 $firstTryDigits · 태그 불일치 $tagMismatch · $out",
            )

            assertTrue("주간 요약 통과 $summaryPass/10", summaryPass >= SUMMARY_MIN)
            assertTrue("패턴 통과 $patternPass/6", patternPass >= PATTERN_MIN)
            assertTrue("호출 p95 ${p95}ms", p95 <= P95_LIMIT_MILLIS)
            assertTrue("요약 문장 속 숫자 $summaryDigits", summaryDigits == 0)
            assertTrue("태그가 문장에 없는 패턴 $tagMismatch", tagMismatch == 0)
        }

    private fun calibrationJson(
        id: String,
        run: InsightRun<*>,
    ): JSONObject {
        val attempt = run.attempts.first()
        return JSONObject()
            .put("id", id)
            .put("budget", CALIBRATION_TOKENS)
            .put("outputTokens", attempt.outputTokens ?: JSONObject.NULL)
            .put("outputChars", attempt.raw.length)
            .put("raw", attempt.raw)
    }

    private fun summaryCase(
        case: DigestCase,
        run: InsightRun<SummaryDraft>,
    ): JSONObject {
        val terms = case.digest.titles + case.digest.categoryIds
        val value = run.value
        return caseJson(case.id, run, ::summaryJson)
            .put("digitsOutsideTitles", value?.let { digitsOutside("${it.headline} ${it.detail}", terms) } ?: 0)
            .put("firstRawDigits", run.attempts.firstOrNull()?.let { digitsOutside(it.raw, terms) } ?: 0)
    }

    private fun patternCase(
        case: WrongCase,
        run: InsightRun<PatternDraft>,
    ): JSONObject {
        val value = run.value
        val consistent = value == null || value.tags.all { value.sentence.contains(it, ignoreCase = true) }
        return caseJson(case.id, run, ::patternJson).put("tagsInSentence", consistent)
    }

    private fun digitsOutside(
        text: String,
        terms: List<String>,
    ): Int =
        terms
            .filter { it.isNotBlank() }
            .sortedByDescending { it.length }
            .fold(text) { rest, term -> rest.replace(term, " ", ignoreCase = true) }
            .count { it.isDigit() }

    private fun summaryJson(draft: SummaryDraft): JSONObject =
        JSONObject()
            .put("headline", draft.headline)
            .put("detail", draft.detail)
            .put("focus", draft.focus ?: JSONObject.NULL)

    private fun patternJson(draft: PatternDraft): JSONObject =
        JSONObject()
            .put("tags", JSONArray(draft.tags))
            .put("sentence", draft.sentence)

    private fun <T> caseJson(
        id: String,
        run: InsightRun<T>,
        value: (T) -> JSONObject,
    ): JSONObject =
        JSONObject()
            .put("id", id)
            .put("pass", run.value != null)
            .put("failure", run.failure?.message ?: JSONObject.NULL)
            .put("value", run.value?.let(value) ?: JSONObject.NULL)
            .put("attempts", JSONArray(run.attempts.map { it.toJson() }))

    private fun InsightAttempt.toJson(): JSONObject =
        JSONObject()
            .put("fix", fix.label)
            .put("promptChars", promptChars)
            .put("millis", millis)
            .put("outputTokens", outputTokens ?: JSONObject.NULL)
            .put("outputChars", raw.length)
            .put("ttftSeconds", ttftSeconds ?: JSONObject.NULL)
            .put("backend", backend ?: JSONObject.NULL)
            .put("verdict", verdict)
            .put("raw", raw)

    private fun JSONArray.attempts(): List<JSONObject> =
        (0 until length()).flatMap { index ->
            val attempts = getJSONObject(index).getJSONArray("attempts")
            (0 until attempts.length()).map { attempts.getJSONObject(it) }
        }

    private companion object {
        const val TAG = "MyNoteEval"
        const val EVAL_DIR = "eval"
        const val SUMMARY_MIN = 9
        const val PATTERN_MIN = 5
        const val P95 = 0.95
        const val P95_LIMIT_MILLIS = 20_000L
        const val CALIBRATION_TOKENS = 24
    }
}
