package com.hancekim.mynote.core.ai.insight

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppVisibility @Inject constructor() {
    suspend fun foreground(): Boolean =
        withContext(Dispatchers.Main.immediate) {
            ProcessLifecycleOwner
                .get()
                .lifecycle.currentState
                .isAtLeast(Lifecycle.State.STARTED)
        }
}
