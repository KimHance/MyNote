package com.hancekim.mynote.core.ai.download

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hancekim.mynote.core.ai.download.decision.DownloadFacts
import com.hancekim.mynote.core.ai.download.decision.WorkFacts
import com.hancekim.mynote.core.ai.download.decision.statusOf
import com.hancekim.mynote.core.ai.download.transfer.ModelFiles
import com.hancekim.mynote.core.ai.download.work.DownloadNotifications
import com.hancekim.mynote.core.ai.download.work.ModelDownloadIntents
import com.hancekim.mynote.core.ai.download.work.ModelDownloadWorker
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.repository.QuizModelSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.Optional
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal fun modelFilesOf(context: Context): ModelFiles =
    ModelFiles(context.getExternalFilesDir(null) ?: context.filesDir, ModelSource.FILE_NAME)

@Singleton
class ModelDownloads
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val intents: Optional<ModelDownloadIntents>,
    ) : QuizModelSource {
        private val revisions = MutableStateFlow(0)

        override fun start() = ModelDownloadWorker.enqueue(context)

        override fun cancel() = ModelDownloadWorker.cancel(context)

        override fun status(): Flow<ModelStatus> =
            combine(workInfos(), revisions) { infos, _ -> infos }
                .map { statusOf(factsOf(leading(it))) }
                .flowOn(Dispatchers.IO)
                .distinctUntilChanged()

        override fun discardPartial() {
            modelFilesOf(context).run {
                discardPart()
                discardPlan()
            }
            revisions.update { it + 1 }
        }

        override fun deleteModel() {
            modelFilesOf(context).target.delete()
            revisions.update { it + 1 }
        }

        suspend fun notifyWaitingForWifi() {
            val info = leading(workInfos().first()) ?: return
            withContext(Dispatchers.IO) { statusOf(factsOf(info)) }
                .takeIf { it == ModelStatus.WaitingForWifi }
                ?: return
            notifications(info.id).run {
                createChannel()
                showWaitingForWifi()
            }
        }

        private fun workInfos(): Flow<List<WorkInfo>> =
            WorkManager
                .getInstance(context)
                .getWorkInfosForUniqueWorkFlow(DownloadWork.NAME)

        private fun notifications(workId: UUID): DownloadNotifications =
            DownloadNotifications(
                context = context,
                cancel = WorkManager.getInstance(context).createCancelPendingIntent(workId),
                retry = intents.orElse(null)?.retry(),
                openSettings = intents.orElse(null)?.openSettings(),
            )

        private fun leading(infos: List<WorkInfo>): WorkInfo? =
            infos.firstOrNull { !it.state.isFinished } ?: infos.lastOrNull()

        private fun factsOf(info: WorkInfo?): DownloadFacts {
            val files = modelFilesOf(context)
            return DownloadFacts(
                work = info?.let { workFactsOf(it) },
                installedBytes = files.target.takeIf { it.isFile }?.length(),
                partBytes = files.partBytes(),
                plannedTotalBytes = files.savedPlan()?.totalBytes,
                nowMillis = System.currentTimeMillis(),
            )
        }

        private fun workFactsOf(info: WorkInfo): WorkFacts =
            WorkFacts(
                state = info.state,
                runAttemptCount = info.runAttemptCount,
                received = info.progress.getLong(DownloadWork.KEY_RECEIVED, ABSENT).takeIf { it >= 0L },
                total = info.progress.getLong(DownloadWork.KEY_TOTAL, ABSENT).takeIf { it >= 0L },
                phase = info.progress.getString(DownloadWork.KEY_PHASE),
                reason = info.outputData.getString(DownloadWork.KEY_REASON),
                nextScheduleTimeMillis = info.nextScheduleTimeMillis.takeIf { it != Long.MAX_VALUE },
            )

        private companion object {
            const val ABSENT = -1L
        }
    }
