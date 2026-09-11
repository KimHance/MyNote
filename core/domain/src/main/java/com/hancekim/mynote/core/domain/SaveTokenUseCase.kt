package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.TokenRepository
import javax.inject.Inject

class SaveTokenUseCase @Inject constructor(
    private val repository: TokenRepository,
) {
    suspend operator fun invoke(token: String?) = repository.saveToken(token?.ifBlank { null })
}
