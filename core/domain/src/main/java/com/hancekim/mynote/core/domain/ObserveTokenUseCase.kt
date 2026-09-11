package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTokenUseCase @Inject constructor(
    private val repository: TokenRepository,
) {
    operator fun invoke(): Flow<String?> = repository.observeToken()
}
