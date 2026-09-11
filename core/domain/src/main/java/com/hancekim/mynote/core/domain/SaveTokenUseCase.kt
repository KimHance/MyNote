package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.TokenRepository
import javax.inject.Inject

/** 토큰을 저장한다(공백이면 null 로 비움). */
class SaveTokenUseCase
    @Inject
    constructor(
        private val repository: TokenRepository,
    ) {
        suspend operator fun invoke(token: String?) = repository.saveToken(token?.ifBlank { null })
    }
