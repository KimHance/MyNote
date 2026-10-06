package com.hancekim.mynote.core.ai.download.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.hancekim.mynote.core.ai.R
import com.hancekim.mynote.core.ai.download.decision.PERCENT_FULL

internal class DownloadNotifications(
    private val context: Context,
    private val cancel: PendingIntent? = null,
    private val retry: PendingIntent? = null,
    private val openSettings: PendingIntent? = null,
) : NoticeBoard {
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

    override fun show(notice: Notice) {
        manager?.notify(notice.slot.id, build(notice))
    }

    override fun hide(slot: NoticeSlot) {
        manager?.cancel(slot.id)
    }

    fun build(notice: Notice): Notification =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setContentIntent(openSettings ?: openApp())
            .setContentTitle(context.getString(notice.title))
            .setContentText(context.getString(notice.body, *notice.bodyArgs.toTypedArray()))
            .setSmallIcon(notice.icon)
            .setOngoing(notice.ongoing)
            .setOnlyAlertOnce(notice.ongoing)
            .setSilent(notice.ongoing)
            .setAutoCancel(!notice.ongoing)
            .apply {
                bar(notice.bar)
                if (notice.expanded) setStyle(NotificationCompat.BigTextStyle())
                action(notice.action)
            }.build()

    private fun NotificationCompat.Builder.bar(bar: NoticeBar) {
        when (bar) {
            NoticeBar.None -> Unit
            NoticeBar.Moving -> setProgress(PERCENT_FULL, 0, true)
            is NoticeBar.Value -> setProgress(PERCENT_FULL, bar.percent, false)
        }
    }

    private fun NotificationCompat.Builder.action(action: NoticeAction?) {
        when (action) {
            NoticeAction.CANCEL -> {
                cancel?.let {
                    addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        context.getString(R.string.ai_model_download_action_cancel),
                        it,
                    )
                }
            }

            NoticeAction.RETRY -> {
                retry?.let {
                    addAction(
                        android.R.drawable.ic_popup_sync,
                        context.getString(R.string.ai_model_download_action_retry),
                        it,
                    )
                }
            }

            null -> {
                Unit
            }
        }
    }

    private fun openApp(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val CHANNEL_ID = "quiz_model_download"
    }
}
