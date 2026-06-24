package com.hancekim.mynote.core.data.datasource

import kotlinx.coroutines.flow.Flow

/** 토큰 로컬 저장소 추상화(인터페이스 data 소유, 구현 :core:database). */
interface TokenStore {
    suspend fun currentToken(): String?
    suspend fun saveToken(token: String?)
    fun observeToken(): Flow<String?>
}
