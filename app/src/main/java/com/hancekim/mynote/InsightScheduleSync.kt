package com.hancekim.mynote

import com.hancekim.mynote.core.domain.ObserveModelStatusUseCase
import com.hancekim.mynote.core.domain.ObserveStudyLogEnabledUseCase
import com.hancekim.mynote.core.domain.insightsWanted
import com.hancekim.mynote.core.domain.repository.InsightScheduler
import com.hancekim.mynote.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightScheduleSync @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val observeModelStatus: ObserveModelStatusUseCase,
    private val observeStudyLogEnabled: ObserveStudyLogEnabledUseCase,
    private val scheduler: InsightScheduler,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job =
            scope.launch {
                combine(observeModelStatus(), observeStudyLogEnabled(), ::insightsWanted)
                    .distinctUntilChanged()
                    .catch { emit(false) }
                    .collect { wanted -> if (wanted) scheduler.schedule() else scheduler.cancel() }
            }
    }
}
