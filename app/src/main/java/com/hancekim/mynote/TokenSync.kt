package com.hancekim.mynote

import com.hancekim.mynote.core.domain.ObserveTokenUseCase
import com.hancekim.mynote.core.network.config.GithubTokenHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenSync @Inject constructor(
    scope: CoroutineScope,
    observeToken: ObserveTokenUseCase,
    private val holder: GithubTokenHolder,
) {
    init {
        scope.launch {
            observeToken().distinctUntilChanged().collect { stored ->
                holder.update(stored ?: BuildConfig.GITHUB_TOKEN.ifBlank { null })
            }
        }
    }
}
