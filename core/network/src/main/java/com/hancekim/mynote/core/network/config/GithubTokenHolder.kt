package com.hancekim.mynote.core.network.config

import javax.inject.Inject
import javax.inject.Singleton

/** 네트워크 클라에 주입되는 인메모리 토큰 홀더. AuthInterceptor 가 동기로 읽는다(블로킹 없음). */
@Singleton
class GithubTokenHolder @Inject constructor() : GithubTokenProvider {
    @Volatile private var token: String? = null
    override fun currentToken(): String? = token
    fun update(token: String?) { this.token = token?.ifBlank { null } }
}
