package com.hancekim.mynote.download

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.hancekim.mynote.core.ai.download.ModelDownloads
import com.hancekim.mynote.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelWaitNotice @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val downloads: ModelDownloads,
) : DefaultLifecycleObserver {
    private var started = false

    fun start() {
        if (started) return
        started = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch { downloads.status().collect(downloads::settleNotices) }
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch { downloads.notifyResting() }
    }
}
