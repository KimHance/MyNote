package com.hancekim.mynote.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** GitHub 토큰 보관 추상화(인터페이스는 domain 소유, DIP). 구현은 :core:data. */
interface TokenRepository {
    suspend fun currentToken(): String?

    suspend fun saveToken(token: String?)

    fun observeToken(): Flow<String?>
}
