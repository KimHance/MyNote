package com.hancekim.mynote.feature.notes.stats

import androidx.lifecycle.SavedStateHandle
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.common.throttle.Clock
import com.hancekim.mynote.core.common.throttle.WallClock
import com.hancekim.mynote.core.domain.ObserveInsightsUseCase
import com.hancekim.mynote.core.domain.ObserveModelStatusUseCase
import com.hancekim.mynote.core.domain.ObserveStudyLogEnabledUseCase
import com.hancekim.mynote.core.domain.ObserveStudyStatsUseCase
import com.hancekim.mynote.core.domain.ObserveWeeklyHistoryUseCase
import com.hancekim.mynote.core.domain.model.InsightSnapshot
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeStudyStats: ObserveStudyStatsUseCase,
    observeStudyLogEnabled: ObserveStudyLogEnabledUseCase,
    observeInsights: ObserveInsightsUseCase,
    observeHistory: ObserveWeeklyHistoryUseCase,
    observeModelStatus: ObserveModelStatusUseCase,
    private val navigator: Navigator,
    private val trace: StatsTrace,
    private val clock: Clock,
    private val wallClock: WallClock,
) : MviViewModel<StatsUiState, StatsIntent>() {
    private val periodSaved = savedStateHandle.getMutableStateFlow(KEY_PERIOD, StatsPeriod.Week)
    private val sectionSaved = savedStateHandle.getMutableStateFlow(KEY_SECTION, StatsSection.Reading)
    private val period = periodSaved.triggerIn(periodSaved.value)
    private val section = sectionSaved.triggerIn(sectionSaved.value)
    private val today = MutableStateFlow(nowZoned())

    private val top = navigator.stack.map { it.lastOrNull() }.distinctUntilChanged()
    private val statsVisible = top.map { it is NotesNavKey.Stats }.distinctUntilChanged()
    private val historyVisible = top.map { it is NotesNavKey.StatsHistory }.distinctUntilChanged()

    private val insights =
        statsVisible.flatMapLatest { visible ->
            if (visible) {
                combine(
                    observeInsights(),
                    observeModelStatus().map { it == ModelStatus.Ready }.distinctUntilChanged(),
                ) { snapshot, ready -> InsightInput(snapshot, ready) }
            } else {
                emptyFlow()
            }
        }

    private val summary =
        combine(insights, periodSaved, today) { input, selected, now ->
            summaryCardOf(input.snapshot, input.modelReady, selected, now)
        }.distinctUntilChanged()
            .catch { trace.failed(it) }
            .triggerIn(null)

    private val pattern =
        combine(insights, today) { input, now -> patternOf(input.snapshot, input.modelReady, now) }
            .distinctUntilChanged()
            .catch { trace.failed(it) }
            .triggerIn(null)

    private val history =
        combine(historyVisible, today) { visible, now -> visible to now }
            .flatMapLatest { (visible, now) ->
                if (visible) observeHistory(now).map { it.toHistoryUi(now) } else emptyFlow()
            }.flowOn(Dispatchers.Default)
            .catch { trace.failed(it) }
            .triggerIn(null)

    private val logEnabled =
        observeStudyLogEnabled()
            .catch { emit(true) }
            .triggerIn(true)

    private val content =
        combine(statsVisible, periodSaved, today) { visible, selected, now -> Triple(visible, selected, now) }
            .flatMapLatest { (visible, selected, now) ->
                if (visible) aggregate(observeStudyStats, selected, now) else emptyFlow()
            }.flowOn(Dispatchers.Default)
            .catch { trace.failed(it) }
            .triggerIn(null)

    override fun build() =
        StatsUiState(
            period = period.value,
            section = section.value,
            logEnabled = logEnabled.value,
            summary = summary.value,
            content = content.value,
            pattern = pattern.value,
            history = history.value,
        )

    override fun handleIntent(event: StatsIntent) {
        when (event) {
            is StatsIntent.SelectPeriod -> periodSaved.value = event.period
            is StatsIntent.SelectSection -> sectionSaved.value = event.section
            StatsIntent.Entered -> today.value = nowZoned()
            StatsIntent.OpenHistory -> navigator.push(NotesNavKey.StatsHistory)
        }
    }

    override fun onFailure(cause: Throwable) = trace.failed(cause)

    private fun aggregate(
        observeStudyStats: ObserveStudyStatsUseCase,
        period: StatsPeriod,
        now: ZonedDateTime,
    ): Flow<StatsContentUi> {
        var startedAt = 0L
        return observeStudyStats(period, now)
            .onStart {
                startedAt = clock.elapsedMillis()
                trace.started(period)
            }.map { stats ->
                stats.toUi().also { trace.computed(period, clock.elapsedMillis() - startedAt) }
            }
    }

    private data class InsightInput(
        val snapshot: InsightSnapshot,
        val modelReady: Boolean,
    )

    private fun nowZoned(): ZonedDateTime = Instant.ofEpochMilli(wallClock.nowMillis()).atZone(ZoneId.systemDefault())

    private companion object {
        const val KEY_PERIOD = "stats_period"
        const val KEY_SECTION = "stats_section"
    }
}
