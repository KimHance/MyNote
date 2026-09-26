package com.hancekim.mynote

import com.hancekim.mynote.core.domain.ObserveTokenUseCase
import com.hancekim.mynote.core.network.config.GithubTokenHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenSync @Inject constructor(
    private val scope: CoroutineScope,
    private val observeToken: ObserveTokenUseCase,
    private val holder: GithubTokenHolder,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job =
            scope.launch {
                observeToken().distinctUntilChanged().collect { stored ->
                    holder.update(stored ?: BuildConfig.GITHUB_TOKEN.ifBlank { null })
                }
            }
    }
}
