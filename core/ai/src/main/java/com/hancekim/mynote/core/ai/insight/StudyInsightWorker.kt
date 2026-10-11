package com.hancekim.mynote.core.ai.insight

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.ai.edge.litertlm.Engine
import com.hancekim.mynote.core.ai.EngineUse
import com.hancekim.mynote.core.ai.ModelEngine
import com.hancekim.mynote.core.ai.download.DownloadWork
import com.hancekim.mynote.core.domain.PruneStudyLogUseCase
import com.hancekim.mynote.core.domain.model.InsightFailure
import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.WeeklySummary
import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.InsightScheduler
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.domain.stats.InsightFacts
import com.hancekim.mynote.core.domain.stats.InsightPlan
import com.hancekim.mynote.core.domain.stats.InsightTarget
import com.hancekim.mynote.core.domain.stats.NoteRef
import com.hancekim.mynote.core.domain.stats.StatsThresholds
import com.hancekim.mynote.core.domain.stats.WeeklyDigestBuilder
import com.hancekim.mynote.core.domain.stats.WeeklyHistory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds

@HiltWorker
internal class StudyInsightWorker
    @AssistedInject
    constructor(
        @Assisted private val context: Context,
        @Assisted params: WorkerParameters,
        private val studyLog: StudyLogRepository,
        private val insights: InsightRepository,
        private val notes: NotesRepository,
        private val scheduler: InsightScheduler,
        private val engine: ModelEngine,
        private val generator: InsightGenerator,
        private val visibility: AppVisibility,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val epoch = insights.status().logEpoch
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val input = collect(now)
            val plan = InsightPlan.of(input.facts)
            Log.i(TAG, "워커 판정 · ${plan.map { it.label() }}")
            if (plan.isEmpty()) return Result.success()

            skipReason()?.let { return skip(it) }
            setProgress(workDataOf(InsightWork.KEY_PHASE to InsightWork.PHASE_GENERATING))

            val outcome = Outcome()
            val use =
                try {
                    withTimeout(InsightWork.TIMEOUT_MILLIS.milliseconds) {
                        engine.tryUse(shouldRelease = { !visibility.foreground() }) { ready ->
                            generate(ready, plan, input, epoch, outcome)
                        }
                    }
                } catch (timeout: TimeoutCancellationException) {
                    Log.i(TAG, "워커 타임아웃 · ${InsightWork.TIMEOUT_MILLIS}ms · 돌던 대상 ${outcome.current?.label()}")
                    outcome.record(InsightFailure.Timeout(), summary = outcome.current !is InsightTarget.Pattern)
                    return finish(outcome, now)
                } catch (cancellation: CancellationException) {
                    Log.i(TAG, "워커 중단 · ${cancellation.message}")
                    throw cancellation
                } catch (cause: Exception) {
                    Log.w(TAG, "워커 엔진 실패", cause)
                    insights.markFailure(System.currentTimeMillis(), InsightFailureKind.Engine, summaryFailed = true)
                    return Result.success()
                }

            return when (use) {
                EngineUse.Busy -> {
                    skip("busy")
                }

                is EngineUse.Done -> {
                    Log.i(
                        TAG,
                        "워커 엔진 · init ${use.initMillis?.let { "${it}ms(새로 올림)" } ?: "0ms(재사용)"} · 닫음 ${use.released}",
                    )
                    finish(use.value, now)
                }
            }
        }

        private suspend fun collect(now: ZonedDateTime): Input {
            val zone = now.zone
            val thisWeek = WeeklyHistory.mondayOf(now.toLocalDate())
            val from =
                thisWeek
                    .minusWeeks(2)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            val nowMillis = now.toInstant().toEpochMilli()
            val pattern = insights.latestPattern(scheduler.stamp)
            val facts =
                InsightFacts(
                    now = now,
                    window = studyLog.observeWindow(from, Long.MAX_VALUE).first(),
                    summaries = insights.summaries(scheduler.stamp),
                    pattern = pattern,
                    wrongRecent = studyLog.countWrongAnswers(nowMillis - PATTERN_WINDOW_MILLIS),
                    wrongSincePattern = pattern?.let { studyLog.countWrongAnswers(it.generatedAt + 1) } ?: 0,
                )
            val refs =
                notes
                    .observeCategories()
                    .first()
                    .flatMap { category -> category.notes.map { NoteRef(it.id, it.title, category.name) } }
            return Input(facts, refs, studyLog.observeFirstRecordAt().first())
        }

        private suspend fun skipReason(): String? =
            when {
                visibility.foreground() -> "foreground"
                downloadRunning() -> "download"
                else -> null
            }

        private suspend fun downloadRunning(): Boolean =
            WorkManager
                .getInstance(context)
                .getWorkInfosForUniqueWorkFlow(DownloadWork.NAME)
                .first()
                .any { !it.state.isFinished }

        private suspend fun skip(reason: String): Result {
            Log.i(TAG, "skipped: $reason")
            insights.markSkipped(System.currentTimeMillis())
            return Result.retry()
        }

        private suspend fun generate(
            ready: Engine,
            plan: List<InsightTarget>,
            input: Input,
            epoch: Long,
            outcome: Outcome,
        ): Outcome {
            for ((index, target) in plan.withIndex()) {
                if (index > 0 && visibility.foreground()) {
                    Log.i(TAG, "skipped: foreground · 남은 대상 ${plan.drop(index).map { it.label() }}")
                    outcome.interrupted = true
                    break
                }
                outcome.current = target
                when (target) {
                    is InsightTarget.Summary -> summarize(ready, target, input, epoch, outcome)
                    is InsightTarget.Pattern -> findPattern(ready, target, input, epoch, outcome)
                }
                if (outcome.discarded || outcome.engineFailed) break
            }
            outcome.current = null
            return outcome
        }

        private suspend fun summarize(
            ready: Engine,
            target: InsightTarget.Summary,
            input: Input,
            epoch: Long,
            outcome: Outcome,
        ) {
            val digest =
                WeeklyDigestBuilder.of(
                    target.weekStart,
                    input.facts.now,
                    input.facts.window,
                    input.notes,
                    input.firstRecordAt,
                )
            val started = System.currentTimeMillis()
            val run = generator.summary(ready, digest)
            Log.i(
                TAG,
                "${target.label()} ${System.currentTimeMillis() - started}ms · 시도 ${run.attempts.size} · ${run.failure?.message ?: "ok"}",
            )
            run.value?.let { draft ->
                if (epochMoved(epoch, outcome)) return
                insights.save(
                    WeeklySummary(
                        target.weekStart,
                        System.currentTimeMillis(),
                        draft.headline,
                        draft.detail,
                        draft.focus,
                    ),
                    scheduler.stamp,
                )
                outcome.saved++
            }
            run.failure?.let { outcome.record(it, summary = true) }
        }

        private suspend fun findPattern(
            ready: Engine,
            target: InsightTarget.Pattern,
            input: Input,
            epoch: Long,
            outcome: Outcome,
        ) {
            val since =
                input.facts.now
                    .toInstant()
                    .toEpochMilli() - PATTERN_WINDOW_MILLIS
            val wrong = studyLog.wrongAnswers(since, StatsThresholds.PATTERN_INPUT_LIMIT)
            val started = System.currentTimeMillis()
            val titles = input.notes.associate { it.id to it.title }
            val run = generator.pattern(ready, wrong, titles)
            Log.i(
                TAG,
                "${target.label()} ${System.currentTimeMillis() - started}ms · 시도 ${run.attempts.size} · ${run.failure?.message ?: "ok"}",
            )
            run.value?.let { draft ->
                if (epochMoved(epoch, outcome)) return
                insights.save(
                    QuizPattern(target.weekStart, System.currentTimeMillis(), draft.tags, draft.sentence),
                    scheduler.stamp,
                )
                outcome.saved++
            }
            run.failure?.let { outcome.record(it, summary = false) }
        }

        private suspend fun epochMoved(
            epoch: Long,
            outcome: Outcome,
        ): Boolean {
            if (insights.status().logEpoch == epoch) return false
            Log.i(TAG, "기록이 지워져 결과를 버림")
            outcome.discarded = true
            return true
        }

        private suspend fun finish(
            outcome: Outcome,
            now: ZonedDateTime,
        ): Result {
            if (outcome.discarded) return Result.success()
            val at = System.currentTimeMillis()
            if (outcome.saved > 0) {
                insights.prune(PruneStudyLogUseCase.insightCutoff(at, now.zone))
            }
            val failure = outcome.failure
            return when {
                failure != null -> {
                    if (outcome.saved > 0) insights.markSuccess(at)
                    insights.markFailure(outcome.failureAt, failure.kind(), outcome.summaryFailed)
                    if (outcome.interrupted) skip("foreground") else Result.success()
                }

                outcome.interrupted -> {
                    if (outcome.saved > 0) insights.markSuccess(at)
                    skip("foreground")
                }

                else -> {
                    insights.markSuccess(at)
                    Result.success()
                }
            }
        }

        private fun InsightFailure.kind(): InsightFailureKind =
            when (this) {
                is InsightFailure.Engine -> InsightFailureKind.Engine

                is InsightFailure.Timeout -> InsightFailureKind.Timeout

                is InsightFailure.Parse,
                is InsightFailure.TooLong,
                is InsightFailure.HasDigits,
                is InsightFailure.MissingField,
                -> InsightFailureKind.Parse
            }

        private fun InsightTarget.label(): String =
            when (this) {
                is InsightTarget.Summary -> "요약 $weekStart($kind)"
                is InsightTarget.Pattern -> "패턴 $weekStart"
            }

        private class Input(
            val facts: InsightFacts,
            val notes: List<NoteRef>,
            val firstRecordAt: Long?,
        )

        private class Outcome {
            var saved = 0
            var interrupted = false
            var discarded = false
            var current: InsightTarget? = null
            var failure: InsightFailure? = null
            var failureAt = 0L
            var summaryFailed = false
            val engineFailed: Boolean get() = failure is InsightFailure.Engine

            fun record(
                cause: InsightFailure,
                summary: Boolean,
            ) {
                if (failure == null || cause is InsightFailure.Engine) failure = cause
                failureAt = System.currentTimeMillis()
                if (summary) summaryFailed = true
            }
        }

        private companion object {
            const val TAG = "MyNoteAi"
            const val PATTERN_WINDOW_MILLIS = StatsThresholds.PATTERN_WINDOW_DAYS * 86_400_000L
        }
    }
