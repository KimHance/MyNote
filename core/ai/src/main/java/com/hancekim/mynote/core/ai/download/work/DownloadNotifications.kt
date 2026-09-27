package com.hancekim.mynote.core.ai.download.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.hancekim.mynote.core.ai.R
import com.hancekim.mynote.core.ai.download.decision.PERCENT_FULL
import com.hancekim.mynote.core.ai.download.decision.gigabytesOf
import com.hancekim.mynote.core.ai.download.transfer.ModelInstaller

internal fun failureBodyRes(reason: String): Int =
    when (reason) {
        ModelInstaller.REASON_NO_SPACE -> R.string.ai_model_download_no_space
        else -> R.string.ai_model_download_failed_body
    }

internal class DownloadNotifications(
    private val context: Context,
    private val cancel: PendingIntent? = null,
    private val retry: PendingIntent? = null,
    private val openSettings: PendingIntent? = null,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        manager?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.ai_model_download_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    fun preparing(): Notification = ongoing(context.getString(R.string.ai_model_download_preparing), null)

    fun showWaitingForWifi() =
        manager?.notify(
            PROGRESS_ID,
            ongoing(context.getString(R.string.ai_model_download_waiting_wifi), null),
        )

    fun showDownloading(percent: Int, received: Long, total: Long) =
        manager?.notify(
            PROGRESS_ID,
            ongoing(
                context.getString(
                    R.string.ai_model_download_progress_bytes,
                    percent,
                    gigabytesOf(received),
                    gigabytesOf(total),
                ),
                percent,
            ),
        )

    fun showRetrying() =
        manager?.notify(
            RETRY_ID,
            ongoing(context.getString(R.string.ai_model_download_retrying), null),
        )

    fun clearRetrying() = manager?.cancel(RETRY_ID)

    fun clearTransient() {
        manager?.cancel(PROGRESS_ID)
        manager?.cancel(RETRY_ID)
    }

    fun showVerifying() =
        manager?.notify(
            PROGRESS_ID,
            ongoing(context.getString(R.string.ai_model_download_verifying), null, cancel = null),
        )

    fun showDone() =
        manager?.notify(
            RESULT_ID,
            builder()
                .setContentTitle(context.getString(R.string.ai_model_download_done))
                .setContentText(context.getString(R.string.ai_model_download_done_body))
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .build(),
        )

    fun showFailed(reason: String) =
        manager?.notify(
            RESULT_ID,
            builder()
                .setContentTitle(context.getString(R.string.ai_model_download_failed))
                .setContentText(context.getString(failureBodyRes(reason)))
                .setStyle(NotificationCompat.BigTextStyle())
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .apply {
                    retry?.let {
                        addAction(
                            android.R.drawable.ic_popup_sync,
                            context.getString(R.string.ai_model_download_action_retry),
                            it,
                        )
                    }
                }.build(),
        )

    private fun ongoing(body: String, percent: Int?, cancel: PendingIntent? = this.cancel): Notification =
        builder()
            .setContentTitle(context.getString(R.string.ai_model_download_title))
            .setContentText(body)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(PERCENT_FULL, percent ?: 0, percent == null)
            .apply {
                cancel?.let {
                    addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        context.getString(R.string.ai_model_download_action_cancel),
                        it,
                    )
                }
            }.build()

    private fun builder(): NotificationCompat.Builder =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setContentIntent(openSettings ?: openApp())

    private fun openApp(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    internal companion object {
        const val PROGRESS_ID = 4201
        const val RESULT_ID = 4202
        const val RETRY_ID = 4203
        private const val CHANNEL_ID = "quiz_model_download"
    }
}
