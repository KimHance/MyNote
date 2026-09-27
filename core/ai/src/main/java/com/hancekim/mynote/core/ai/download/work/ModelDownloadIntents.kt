package com.hancekim.mynote.core.ai.download.work

import android.app.PendingIntent

interface ModelDownloadIntents {
    fun retry(): PendingIntent?

    fun openSettings(): PendingIntent?
}
