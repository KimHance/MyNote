package com.hancekim.mynote

import com.hancekim.mynote.core.domain.ObserveTokenUseCase
import com.hancekim.mynote.core.network.config.GithubTokenHolder
import com.hancekim.mynote.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenSync @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val observeToken: ObserveTokenUseCase,
    private val holder: GithubTokenHolder,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job =
            scope.launch {
                observeToken()
                    .catch { cause -> if (cause is IOException) emit(null) else throw cause }
                    .distinctUntilChanged()
                    .collect { stored ->
                        holder.update(stored ?: BuildConfig.GITHUB_TOKEN.ifBlank { null })
                    }
            }
    }
}
