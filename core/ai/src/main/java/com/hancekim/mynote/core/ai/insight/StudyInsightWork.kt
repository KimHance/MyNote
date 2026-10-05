package com.hancekim.mynote.core.ai.insight

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hancekim.mynote.core.ai.download.ModelSource
import com.hancekim.mynote.core.domain.model.InsightStamp
import com.hancekim.mynote.core.domain.model.InsightWorkState
import com.hancekim.mynote.core.domain.repository.InsightScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyInsightWork
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : InsightScheduler {
        override val stamp = InsightStamp(ModelSource.COMMIT, InsightWork.SCHEMA_VERSION)

        override suspend fun schedule() {
            val manager = WorkManager.getInstance(context)
            val current = manager.getWorkInfosForUniqueWorkFlow(InsightWork.NAME).first()
            if (current.any { !it.state.isFinished && InsightWork.SPEC_TAG in it.tags }) return
            val request =
                PeriodicWorkRequestBuilder<StudyInsightWorker>(InsightWork.PERIOD_DAYS, TimeUnit.DAYS)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiresCharging(true)
                            .setRequiresBatteryNotLow(true)
                            .build(),
                    ).setBackoffCriteria(BackoffPolicy.LINEAR, InsightWork.BACKOFF_MINUTES, TimeUnit.MINUTES)
                    .addTag(InsightWork.SPEC_TAG)
                    .build()
            manager.enqueueUniquePeriodicWork(InsightWork.NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        override fun cancel() {
            WorkManager.getInstance(context).cancelUniqueWork(InsightWork.NAME)
        }

        override fun state(): Flow<InsightWorkState> =
            WorkManager
                .getInstance(context)
                .getWorkInfosForUniqueWorkFlow(InsightWork.NAME)
                .map { infos ->
                    if (infos.any { it.generating() }) InsightWorkState.Generating else InsightWorkState.Idle
                }.distinctUntilChanged()

        private fun WorkInfo.generating(): Boolean =
            state == WorkInfo.State.RUNNING && progress.getString(InsightWork.KEY_PHASE) == InsightWork.PHASE_GENERATING
    }
