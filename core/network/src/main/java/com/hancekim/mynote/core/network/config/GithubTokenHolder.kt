package com.hancekim.mynote.core.network.config

import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 네트워크 클라에 주입되는 토큰 홀더.
 * - [GithubTokenProvider.currentToken] : AuthInterceptor 가 동기로 읽음(블로킹 없음)
 * - [AppliedTokenSource.applied] : "적용된" 토큰 Flow → ViewModel 이 이걸 보고 sync(홀더와 같은 값이라 레이스 없음)
 */
@Singleton
class GithubTokenHolder @Inject constructor() : GithubTokenProvider, AppliedTokenSource {
    private val state = MutableStateFlow<String?>(null)
    override fun currentToken(): String? = state.value
    override fun applied(): Flow<String?> = state.asStateFlow()
    fun update(token: String?) { state.value = token?.ifBlank { null } }
}
