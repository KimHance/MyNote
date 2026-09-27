package com.hancekim.mynote.core.ai.download.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.storage.StorageManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hancekim.mynote.core.ai.di.ModelDownload
import com.hancekim.mynote.core.ai.download.DownloadWork
import com.hancekim.mynote.core.ai.download.ModelSource
import com.hancekim.mynote.core.ai.download.decision.percentOf
import com.hancekim.mynote.core.ai.download.modelFilesOf
import com.hancekim.mynote.core.ai.download.transfer.InstallResult
import com.hancekim.mynote.core.ai.download.transfer.ModelDownloader
import com.hancekim.mynote.core.ai.download.transfer.ModelInstaller
import com.hancekim.mynote.core.common.throttle.Throttler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.util.Optional
import java.util.concurrent.TimeUnit

@HiltWorker
internal class ModelDownloadWorker
    @AssistedInject
    constructor(
        @Assisted private val context: Context,
        @Assisted params: WorkerParameters,
        @ModelDownload private val client: OkHttpClient,
        private val intents: Optional<ModelDownloadIntents>,
    ) : CoroutineWorker(context, params) {
        private val notifications =
            DownloadNotifications(
                context = context,
                cancel = WorkManager.getInstance(context).createCancelPendingIntent(id),
                retry = intents.orElse(null)?.retry(),
                openSettings = intents.orElse(null)?.openSettings(),
            )
        private val throttler = Throttler(PROGRESS_INTERVAL_MILLIS)
        private var notifiedPercent: Int? = null

        override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo()

        override suspend fun doWork(): Result {
            notifications.createChannel()
            notifications.clearRetrying()
            setForeground(foregroundInfo())

            val files = modelFilesOf(context)
            val installer = ModelInstaller(ModelDownloader(client), files) { allocatableBytes(files.target) }

            val outcome =
                try {
                    installer.install(
                        url = ModelSource.url,
                        onVerifying = ::reportVerifying,
                    ) { received, total ->
                        throttler.process { report(received, total) }
                    }
                } catch (cancellation: CancellationException) {
                    notifications.clearTransient()
                    throw cancellation
                }

            return when (outcome) {
                InstallResult.Installed -> {
                    notifications.showDone()
                    Result.success()
                }

                is InstallResult.Retry -> {
                    notifications.showRetrying()
                    Result.retry()
                }

                is InstallResult.Failed -> {
                    Log.w(TAG, "모델 내려받기 실패: ${outcome.reason}")
                    notifications.showFailed(outcome.reason)
                    Result.failure(workDataOf(DownloadWork.KEY_REASON to outcome.reason))
                }
            }
        }

        private fun report(received: Long, total: Long) {
            if (isStopped) return
            setProgressAsync(workDataOf(DownloadWork.KEY_RECEIVED to received, DownloadWork.KEY_TOTAL to total))
            percentOf(received, total)
                .takeIf { it != notifiedPercent }
                ?.let {
                    notifiedPercent = it
                    notifications.showDownloading(it, received, total)
                }
        }

        private fun reportVerifying() {
            setProgressAsync(workDataOf(DownloadWork.KEY_PHASE to DownloadWork.PHASE_VERIFY))
            notifications.showVerifying()
        }

        private fun foregroundInfo(): ForegroundInfo =
            ForegroundInfo(
                DownloadNotifications.PROGRESS_ID,
                notifications.preparing(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )

        private fun allocatableBytes(target: File): Long {
            val storage = context.getSystemService(StorageManager::class.java) ?: return target.usableSpace
            return try {
                storage.getAllocatableBytes(storage.getUuidForPath(target.parentFile ?: target))
            } catch (cause: IOException) {
                target.usableSpace
            }
        }

        internal companion object {
            private const val TAG = "MyNoteAi"

            private const val PROGRESS_INTERVAL_MILLIS = 200L
            private const val BACKOFF_SECONDS = 30L

            fun enqueue(context: Context) {
                val request =
                    OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setConstraints(
                            Constraints
                                .Builder()
                                .setRequiredNetworkType(NetworkType.UNMETERED)
                                .setRequiresStorageNotLow(true)
                                .build(),
                        ).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                        .build()

                WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork(DownloadWork.NAME, ExistingWorkPolicy.KEEP, request)
            }

            fun cancel(context: Context) {
                WorkManager.getInstance(context).cancelUniqueWork(DownloadWork.NAME)
            }
        }
    }
