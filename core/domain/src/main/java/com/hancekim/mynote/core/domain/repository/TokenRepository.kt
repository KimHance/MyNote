package com.hancekim.mynote.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface TokenRepository {
    suspend fun currentToken(): String?

    suspend fun saveToken(token: String?)

    fun observeToken(): Flow<String?>
}
