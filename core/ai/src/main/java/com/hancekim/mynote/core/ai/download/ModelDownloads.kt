package com.hancekim.mynote.core.ai.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloads
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun start() = ModelDownloadWorker.enqueue(context)

        fun cancel() = ModelDownloadWorker.cancel(context)
    }
