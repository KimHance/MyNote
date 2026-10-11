package com.hancekim.mynote.feature.notes.stats

import androidx.lifecycle.SavedStateHandle
import com.hancekim.mynote.core.common.throttle.Clock
import com.hancekim.mynote.core.common.throttle.WallClock
import com.hancekim.mynote.core.domain.ObserveInsightsUseCase
import com.hancekim.mynote.core.domain.ObserveModelStatusUseCase
import com.hancekim.mynote.core.domain.ObserveStudyLogEnabledUseCase
import com.hancekim.mynote.core.domain.ObserveStudyStatsUseCase
import com.hancekim.mynote.core.domain.ObserveWeeklyHistoryUseCase
import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.FontScale
import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.InsightStamp
import com.hancekim.mynote.core.domain.model.InsightStatus
import com.hancekim.mynote.core.domain.model.InsightWorkState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.NoteFacts
import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.model.WeeklySummary
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.InsightScheduler
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.QuizModelSource
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FM-8 통계 집계 시간은 주입한 시계로 잰다`() =
        runTest {
            val ticks = AtomicLong(1_000L)
            val wallReads = AtomicLong(0L)
            val trace = RecordingTrace()
            val vm =
                StatsViewModel(
                    savedStateHandle = SavedStateHandle(),
                    observeStudyStats = ObserveStudyStatsUseCase(FakeStudyLog, FakeNotes, FakeBookmarks),
                    observeStudyLogEnabled = ObserveStudyLogEnabledUseCase(FakeSettings),
                    observeInsights = ObserveInsightsUseCase(FakeInsights, FakeScheduler),
                    observeHistory = ObserveWeeklyHistoryUseCase(FakeStudyLog, FakeInsights, FakeScheduler),
                    observeModelStatus = ObserveModelStatusUseCase(FakeModels),
                    navigator = Navigator(backgroundScope, NotesNavKey.Stats, clock = Clock { 0L }),
                    trace = trace,
                    clock = Clock { ticks.getAndAdd(STEP) },
                    wallClock =
                        WallClock {
                            wallReads.incrementAndGet()
                            WALL_NOW
                        },
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

            vm.state.first { it.content != null }

            assertEquals(listOf(StatsPeriod.Week to STEP), trace.computed)
            assertTrue(wallReads.get() > 0)
        }

    private class RecordingTrace : StatsTrace {
        val computed = mutableListOf<Pair<StatsPeriod, Long>>()

        override fun started(period: StatsPeriod) = Unit

        override fun computed(
            period: StatsPeriod,
            tookMillis: Long,
        ) {
            computed += period to tookMillis
        }

        override fun failed(cause: Throwable): Unit = throw AssertionError(cause)
    }

    private object FakeStudyLog : StudyLogRepository {
        override suspend fun record(session: ReadSession) = Unit

        override suspend fun record(attempt: QuizAttempt) = Unit

        override suspend fun record(event: BookmarkEvent) = 0L

        override suspend fun deleteBookmarkEvent(id: Long) = Unit

        override fun observeWindow(
            from: Long,
            until: Long,
        ): Flow<StudyWindow> = flowOf(StudyWindow(emptyList(), emptyList(), emptyList()))

        override fun observeNoteFacts(): Flow<NoteFacts> = flowOf(NoteFacts(emptyList(), emptyList()))

        override fun observeFirstRecordAt(): Flow<Long?> = flowOf(null)

        override suspend fun wrongAnswers(
            since: Long,
            limit: Int,
        ) = emptyList<Nothing>()

        override suspend fun countWrongAnswers(since: Long) = 0

        override suspend fun clear() = Unit

        override suspend fun prune(before: Long) = Unit

        override suspend fun pruneAnswers(before: Long) = Unit
    }

    private object FakeNotes : NotesRepository {
        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())

        override suspend fun sync(): Result<SyncOutcome> = Result.success(SyncOutcome())
    }

    private object FakeBookmarks : BookmarkRepository {
        override fun observeBookmarks() = flowOf(emptyList<Nothing>())

        override suspend fun add(bookmark: Bookmark) = Unit

        override suspend fun remove(noteIds: Collection<String>) = Unit
    }

    private object FakeSettings : SettingsRepository {
        override fun observeFontScale(): Flow<FontScale> = flowOf(FontScale.Normal)

        override suspend fun setFontScale(scale: FontScale) = Unit

        override fun observeStudyLogEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setStudyLogEnabled(enabled: Boolean) = Unit
    }

    private object FakeModels : QuizModelSource {
        override fun status(): Flow<ModelStatus> = flowOf(ModelStatus.Missing)

        override fun start() = Unit

        override fun cancel() = Unit

        override fun discardPartial() = Unit

        override suspend fun deleteModel() = Unit
    }

    private object FakeInsights : InsightRepository {
        override fun observeLatestSummary(stamp: InsightStamp): Flow<WeeklySummary?> = flowOf(null)

        override fun observeSummaries(stamp: InsightStamp): Flow<List<WeeklySummary>> = flowOf(emptyList())

        override fun observeLatestPattern(stamp: InsightStamp): Flow<QuizPattern?> = flowOf(null)

        override fun observeStatus(): Flow<InsightStatus> = flowOf(InsightStatus())

        override suspend fun summaries(stamp: InsightStamp) = emptyList<WeeklySummary>()

        override suspend fun latestPattern(stamp: InsightStamp) = null

        override suspend fun status() = InsightStatus()

        override suspend fun save(
            summary: WeeklySummary,
            stamp: InsightStamp,
        ) = Unit

        override suspend fun save(
            pattern: QuizPattern,
            stamp: InsightStamp,
        ) = Unit

        override suspend fun markSuccess(at: Long) = Unit

        override suspend fun markFailure(
            at: Long,
            kind: InsightFailureKind,
            summaryFailed: Boolean,
        ) = Unit

        override suspend fun markSkipped(at: Long) = Unit

        override suspend fun bumpEpoch() = 0L

        override suspend fun clearStatus() = Unit

        override suspend fun prune(before: LocalDate) = Unit
    }

    private object FakeScheduler : InsightScheduler {
        override val stamp = InsightStamp("test", 1)

        override suspend fun schedule() = Unit

        override fun cancel() = Unit

        override fun state(): Flow<InsightWorkState> = flowOf(InsightWorkState.Idle)
    }

    private companion object {
        const val STEP = 250L
        const val WALL_NOW = 1_760_000_000_000L
    }
}
