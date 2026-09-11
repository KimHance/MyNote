package com.hancekim.mynote.core.network.interceptor

import com.hancekim.mynote.core.network.config.GithubTokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 매 요청에 GitHub 토큰을 Bearer 로 주입한다. 토큰이 없으면 헤더 없이 보낸다
 * → private 레포면 401 → reconcile 이 실패(Result.failure)하고 화면이 "토큰 필요" 에러 상태를 띄운다.
 */
class AuthInterceptor
    @Inject
    constructor(
        private val tokenProvider: GithubTokenProvider,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = tokenProvider.currentToken()
            val request =
                if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain
                        .request()
                        .newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }
            return chain.proceed(request)
        }
    }
