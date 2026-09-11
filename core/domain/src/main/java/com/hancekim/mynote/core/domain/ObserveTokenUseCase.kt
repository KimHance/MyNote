package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 현재 토큰을 관찰한다(없으면 null). */
class ObserveTokenUseCase
    @Inject
    constructor(
        private val repository: TokenRepository,
    ) {
        operator fun invoke(): Flow<String?> = repository.observeToken()
    }
