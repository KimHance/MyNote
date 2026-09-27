package com.hancekim.mynote.core.ai.download

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
import com.hancekim.mynote.core.common.throttle.Throttler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@HiltWorker
internal class ModelDownloadWorker
    @AssistedInject
    constructor(
        @Assisted private val context: Context,
        @Assisted params: WorkerParameters,
        @ModelDownload private val client: OkHttpClient,
    ) : CoroutineWorker(context, params) {
        private val notifications = DownloadNotifications(context)
        private val throttler = Throttler(PROGRESS_INTERVAL_MILLIS)

        override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(null)

        override suspend fun doWork(): Result {
            notifications.createChannel()
            setForeground(foregroundInfo(null))

            val files = ModelFiles(context.getExternalFilesDir(null) ?: context.filesDir, ModelSource.FILE_NAME)
            val installer = ModelInstaller(ModelDownloader(client), files) { allocatableBytes(files.target) }

            val outcome =
                installer.install(ModelSource.url) { received, total ->
                    throttler.process { report(received, total) }
                }

            return when (outcome) {
                InstallResult.Installed -> {
                    notifications.showDone()
                    Result.success()
                }

                is InstallResult.Retry -> {
                    Result.retry()
                }

                is InstallResult.Failed -> {
                    Log.w(TAG, "모델 내려받기 실패: ${outcome.reason}")
                    notifications.showFailed(outcome.reason)
                    Result.failure(workDataOf(KEY_REASON to outcome.reason))
                }
            }
        }

        private fun report(received: Long, total: Long) {
            val percent = if (total > 0L) ((received * 100) / total).toInt() else 0
            setProgressAsync(workDataOf(KEY_RECEIVED to received, KEY_TOTAL to total))
            notifications.showProgress(percent)
        }

        private fun foregroundInfo(percent: Int?): ForegroundInfo =
            ForegroundInfo(
                DownloadNotifications.PROGRESS_ID,
                notifications.progress(percent),
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
            const val WORK_NAME = "quiz-model-download"

            private const val TAG = "MyNoteAi"
            const val KEY_RECEIVED = "received"
            const val KEY_TOTAL = "total"
            const val KEY_REASON = "reason"

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
                    .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
            }

            fun cancel(context: Context) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            }
        }
    }
