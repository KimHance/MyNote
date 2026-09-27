package com.hancekim.mynote.core.ai.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.hancekim.mynote.core.ai.R

internal class DownloadNotifications(private val context: Context) {
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

    fun progress(percent: Int?): Notification =
        builder()
            .setContentTitle(context.getString(R.string.ai_model_download_title))
            .setContentText(
                percent
                    ?.let { context.getString(R.string.ai_model_download_percent, it) }
                    ?: context.getString(R.string.ai_model_download_preparing),
            ).setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, percent ?: 0, percent == null)
            .build()

    fun showProgress(percent: Int?) = manager?.notify(PROGRESS_ID, progress(percent))

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
                .setContentText(
                    if (reason == ModelInstaller.REASON_NO_SPACE) {
                        context.getString(R.string.ai_model_download_no_space)
                    } else {
                        context.getString(R.string.ai_model_download_failed_body)
                    },
                ).setStyle(NotificationCompat.BigTextStyle())
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .build(),
        )

    private fun builder(): NotificationCompat.Builder =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setContentIntent(openApp())

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
        private const val CHANNEL_ID = "quiz_model_download"
    }
}
