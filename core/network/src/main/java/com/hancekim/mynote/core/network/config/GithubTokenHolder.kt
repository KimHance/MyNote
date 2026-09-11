package com.hancekim.mynote.core.network.config

import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubTokenHolder @Inject constructor() : GithubTokenProvider, AppliedTokenSource {
    private val state = MutableStateFlow<String?>(null)

    override fun currentToken(): String? = state.value

    override fun applied(): Flow<String?> = state.asStateFlow()

    fun update(token: String?) {
        state.value = token?.ifBlank { null }
    }
}
