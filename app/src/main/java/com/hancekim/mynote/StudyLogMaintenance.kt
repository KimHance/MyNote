package com.hancekim.mynote

import com.hancekim.mynote.core.domain.PruneStudyLogUseCase
import com.hancekim.mynote.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyLogMaintenance @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val prune: PruneStudyLogUseCase,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch { prune() }
    }
}
