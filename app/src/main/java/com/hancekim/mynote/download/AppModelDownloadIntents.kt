package com.hancekim.mynote.download

import android.app.PendingIntent
import android.content.Context
import com.hancekim.mynote.MainActivity
import com.hancekim.mynote.core.ai.download.work.ModelDownloadIntents
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppModelDownloadIntents @Inject constructor(
    @ApplicationContext private val context: Context,
) : ModelDownloadIntents {
    override fun retry(): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_RETRY,
            MainActivity.retryIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    override fun openSettings(): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_OPEN_SETTINGS,
            MainActivity.settingsIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val REQUEST_RETRY = 4211
        const val REQUEST_OPEN_SETTINGS = 4212
    }
}
