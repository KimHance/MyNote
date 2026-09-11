package com.hancekim.mynote.core.data.datasource

import kotlinx.coroutines.flow.Flow

interface TokenStore {
    suspend fun currentToken(): String?

    suspend fun saveToken(token: String?)

    fun observeToken(): Flow<String?>
}
