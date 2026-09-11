package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.TokenStore
import com.hancekim.mynote.core.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TokenRepositoryImpl
    @Inject
    constructor(
        private val tokenStore: TokenStore,
    ) : TokenRepository {
        override suspend fun currentToken(): String? = tokenStore.currentToken()

        override suspend fun saveToken(token: String?) = tokenStore.saveToken(token)

        override fun observeToken(): Flow<String?> = tokenStore.observeToken()
    }
